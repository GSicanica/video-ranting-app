package com.youtube.rating.android.viewmodel

import com.youtube.rating.core.coroutines.ioDispatcher

import android.app.Application
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.cache.SearchCacheKey
import com.youtube.rating.android.core.AppKeys
import com.youtube.rating.android.domain.usecase.HomeBrowseRulesUseCase
import com.youtube.rating.android.domain.usecase.HomeRandomRequestUseCase
import com.youtube.rating.android.domain.usecase.ShuffleVideosUseCase
import com.youtube.rating.android.home.APPLY_RATING_FILTERS_DEBOUNCE_MS
import com.youtube.rating.android.home.BrowseCache
import com.youtube.rating.android.home.DELAY_REPORT_DIALOG_MS
import com.youtube.rating.android.home.FILTER_DELAY_MS
import com.youtube.rating.android.home.HomeContract
import com.youtube.rating.android.home.HomeViewState
import com.youtube.rating.android.home.MAX_RATING
import com.youtube.rating.android.home.MIN_RATING
import com.youtube.rating.android.home.PopularRange
import com.youtube.rating.android.home.RandomResult
import com.youtube.rating.android.home.SEARCH_TYPING_DEBOUNCE_MS
import com.youtube.rating.android.home.SORT_DEBOUNCE_MS
import com.youtube.rating.android.localization.ContentLanguageManager
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.network.NetworkMonitor
import com.youtube.rating.android.storage.FavoriteVideo
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.storage.OfflineVideo
import kotlinx.coroutines.flow.first
import com.youtube.rating.android.ui.models.BrowseRatingFilters
import com.youtube.rating.android.ui.models.HomeScreenUiState
import com.youtube.rating.android.sentry.SentryLogger
import com.youtube.rating.android.domain.usecase.home.GetHomeCategoriesUseCase
import com.youtube.rating.android.domain.usecase.home.GetPopularSearchTermsUseCase
import com.youtube.rating.android.domain.usecase.home.GetTopVideosUseCase
import com.youtube.rating.android.domain.usecase.home.ReportVideoUseCase
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase
import com.youtube.rating.android.paging.HomeBrowsePagingParams
import com.youtube.rating.android.paging.HomeSearchPagingSource
import com.youtube.rating.shared.models.HomeCategory
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.VideoSearchResult
import com.youtube.rating.shared.models.VideoStats
import com.youtube.rating.shared.usecase.TasteGraphRankerUseCase
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.RequestDeduplicator
import io.sentry.SpanStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.youtube.rating.android.ui.models.HomeTab
import java.time.LocalDate
import com.youtube.rating.android.data.prefs.HomePrefs
import com.youtube.rating.android.data.prefs.LanguagePrefs
import com.youtube.rating.android.data.prefs.SaintsPrefs
import com.youtube.rating.android.data.prefs.VideoPrefs
import com.youtube.rating.android.data.prefs.FeatureFlagsPrefs
import com.youtube.rating.android.utils.PerformanceProfile
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.core.coroutines.makeIOCall

private const val REPORT_REASON_USER = "User report"

@OptIn(FlowPreview::class)
class HomeViewModel(
    application: Application,
    private val getPopularSearchTermsUseCase: GetPopularSearchTermsUseCase,
    private val getHomeCategoriesUseCase: GetHomeCategoriesUseCase,
    private val getTopVideosUseCase: GetTopVideosUseCase,
    private val searchVideosUseCase: SearchVideosUseCase,
    private val reportVideoUseCase: ReportVideoUseCase,
    private val favoritesGateway: FavoritesGateway,
    private val offlineRepository: OfflineRepository,
    private val ratingApiClient: com.youtube.rating.shared.api.RatingApiClient,
    private val youTubeInfoService: YouTubeInfoService,
    private val userTokenManager: com.youtube.rating.android.utils.UserTokenManager,
    private val savedStateHandle: SavedStateHandle,
    private val homeBrowseRulesUseCase: HomeBrowseRulesUseCase,
    private val homeRandomRequestUseCase: HomeRandomRequestUseCase,
    private val shuffleVideosUseCase: ShuffleVideosUseCase,
    private val saintOfDayManager: com.youtube.rating.android.utils.SaintOfDayManager
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val perfProfile = PerformanceProfile.get(appContext)
    private val tasteGraphRanker = TasteGraphRankerUseCase()

    // Prefetch throttling (moved from UI)
    private var lastPrefetchTime: Long = 0L
    private var lastPrefetchedSig: Long = 0L

    private val videoStatsCache = com.youtube.rating.android.cache.ApiCache<String, VideoStats>(
        maxSize = 100,
        ttlMs = 60_000L
    )

    private val networkMonitor = NetworkMonitor(context = appContext)
    private val contentLanguageManager = ContentLanguageManager(context = appContext)

    private val json = Json { encodeDefaults = true }

    // -----------------------------
    // MVI (Intent -> State + Effects)
    // -----------------------------

    private val _intents = MutableSharedFlow<HomeContract.Intent>(extraBufferCapacity = 64)
    internal val intents = _intents.asSharedFlow()

    private val _effects = Channel<HomeContract.Effect>(capacity = Channel.BUFFERED)
    internal val effects: Flow<HomeContract.Effect> = _effects.receiveAsFlow()

    internal fun dispatch(intent: HomeContract.Intent) {
        _intents.tryEmit(intent)
    }

    @Deprecated("Use dispatch(intent = HomeContract.Intent) instead.")
    internal fun onAction(action: HomeContract.Intent) {
        dispatch(intent = action)
    }

    private fun reduce(intent: HomeContract.Intent) {
        when (intent) {
            is HomeContract.Intent.SearchQueryChanged -> updateSearchQuery(query = intent.query)
            HomeContract.Intent.ClearSearch -> updateSearchQuery(query = "")
            is HomeContract.Intent.SortSelected -> onSortSelected(sort = intent.sort)
            is HomeContract.Intent.ToggleLanguage -> toggleContentLanguage(language = intent.language)
            is HomeContract.Intent.SelectPopularRange -> {
                updateState {
                    it.copy(
                        popularRange = intent.range,
                        popularVideos = computePopularVideos(
                            browseVideos = it.browseVideos,
                            featuredVideos = it.featuredVideos,
                            range = intent.range,
                            popularTimeVideosEnabled = it.popularTimeVideosEnabled
                        )
                    )
                }
            }
            is HomeContract.Intent.RefreshBrowse -> {
                updateState { it.copy(isBrowsing = true, isLoadingMore = false, browseError = null, hasMore = true) }
            }
            HomeContract.Intent.LoadMore -> Unit // paging handles append
            is HomeContract.Intent.SetBrowseRatingFilters -> updateBrowseRatingFilters { intent.filters }
            is HomeContract.Intent.SelectTab -> updateUiState { it.copy(selectedTab = intent.tab) }
        }
    }

    private fun handle(intent: HomeContract.Intent) {
        when (intent) {
            is HomeContract.Intent.SearchQueryChanged -> Unit // observers react to searchQuery changes
            HomeContract.Intent.ClearSearch -> Unit
            is HomeContract.Intent.SortSelected -> Unit // onSortSelected already debounces refresh
            is HomeContract.Intent.ToggleLanguage -> Unit // toggleContentLanguage triggers observers
            is HomeContract.Intent.SelectPopularRange -> loadFeaturedVideos(_state.value.languageCodes, intent.range)
            is HomeContract.Intent.RefreshBrowse -> loadBrowseVideos(
                _state.value.languageCodes,
                resetPage = true,
                forceRefresh = intent.forceRefresh,
                preserveOrder = intent.preserveOrder
            )
            HomeContract.Intent.LoadMore -> loadMoreVideos(languageCodes = _state.value.languageCodes)
            is HomeContract.Intent.SetBrowseRatingFilters -> Unit // observers react to filters changes
            is HomeContract.Intent.SelectTab -> {
                if (intent.tab == HomeTab.ForYou) {
                    loadForYouVideos(forceRefresh = false)
                }
            }
        }
    }

    internal fun handleRandomRequest(
        hasMore: Boolean,
        isShuffling: Boolean,
        listSize: Int,
        languageCodes: List<String>
    ): RandomResult {
        val useCaseResult = homeRandomRequestUseCase.execute(
            isLocked = _isRandomLocked.value,
            timestamps = randomClickTimestamps,
            nowMs = System.currentTimeMillis(),
            hasMore = hasMore,
            isShuffling = isShuffling,
            listSize = listSize
        )
        randomClickTimestamps.clear()
        randomClickTimestamps.addAll(useCaseResult.timestamps)

        if (useCaseResult.shouldLock) {
            _isRandomLocked.value = true
            return RandomResult.Locked
        }

        return when (useCaseResult.outcome) {
            HomeRandomRequestUseCase.Outcome.WAIT_FOR_LOAD -> RandomResult.WaitForLoad
            HomeRandomRequestUseCase.Outcome.ALREADY_SHUFFLING -> RandomResult.AlreadyShuffling
            HomeRandomRequestUseCase.Outcome.NOT_ENOUGH_VIDEOS -> RandomResult.NotEnoughVideos
            HomeRandomRequestUseCase.Outcome.SHUFFLE -> {
                shuffleBrowseVideos(languageCodes = languageCodes)
                updateUiState { it.copy(showBrowseControlsSheet = false) }
                RandomResult.Shuffled
            }
            HomeRandomRequestUseCase.Outcome.LOCKED -> RandomResult.Locked
        }
    }

    fun requestCloseChoice(video: VideoSearchResult, seconds: Int) {
        updateState {
            it.copy(
                pendingCloseVideo = video,
                pendingCloseSeconds = seconds,
                showCloseChoiceDialog = true
            )
        }
    }

    fun clearCloseChoice() {
        updateState {
            it.copy(
                pendingCloseVideo = null,
                pendingCloseSeconds = 0,
                showCloseChoiceDialog = false
            )
        }
    }

    fun setClipToPlay(clip: com.youtube.rating.android.data.models.VideoClip?) {
        updateState { it.copy(clipToPlay = clip) }
    }

    fun setPlaybackSeconds(seconds: Float) {
        if (_state.value.currentPlaybackSeconds == seconds) return
        updateState { it.copy(currentPlaybackSeconds = seconds) }
    }

    // -----------------------------
    // STATE
    // -----------------------------

    private val _loveRange = MutableStateFlow(MIN_RATING..MAX_RATING)
    val loveRange: StateFlow<ClosedFloatingPointRange<Float>> = _loveRange.asStateFlow()

    private val _faithRange = MutableStateFlow(MIN_RATING..MAX_RATING)
    val faithRange: StateFlow<ClosedFloatingPointRange<Float>> = _faithRange.asStateFlow()

    private val _hopeRange = MutableStateFlow(MIN_RATING..MAX_RATING)
    val hopeRange: StateFlow<ClosedFloatingPointRange<Float>> = _hopeRange.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isRandomLocked = MutableStateFlow(false)
    val isRandomLocked: StateFlow<Boolean> = _isRandomLocked.asStateFlow()

    private val randomClickTimestamps = mutableListOf<Long>()

    private val _userToken = MutableStateFlow<String?>(null)
    val userToken: StateFlow<String?> = _userToken.asStateFlow()

    // Mutex to prevent race conditions between loadBrowseVideos and shuffleBrowseVideos
    private val browseVideosMutex = Mutex()

    private var autoLoadJob: kotlinx.coroutines.Job? = null

    private val _state = MutableStateFlow(
        run {
            val restoredFilters = restoreBrowseRatingFilters()
            val restoredCategory = restoreSelectedCategory()
            val restoredUi = restoreUiState()
            val autoShuffle = restoredFilters == BrowseRatingFilters() &&
                restoredCategory == null &&
                restoredUi.showBrowseControlsSheet == false &&
                restoredUi.showBrowse == false
            HomeViewState(
                uiState = restoredUi,
                browseRatingFilters = restoredFilters,
                selectedCategory = restoredCategory,
                autoShuffle = autoShuffle
            )
        }
    )

    private val browsePagingParams by lazy {
        MutableStateFlow(buildBrowsePagingParams(forceRefresh = false))
    }

    val browsePagingData: kotlinx.coroutines.flow.Flow<PagingData<VideoSearchResult>> =
        browsePagingParams
            .flatMapLatest { params ->
                Pager(
                    config = PagingConfig(
                        pageSize = params.perPage,
                        prefetchDistance = 2,
                        initialLoadSize = params.perPage,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = { HomeSearchPagingSource(searchVideosUseCase = searchVideosUseCase, params = params) }
                ).flow
            }
            .cachedIn(viewModelScope)

    /**
     * Sort param koji šaljemo API-ju.
     *
     * Dozvoljene vrijednosti (server):
     * "latest" (default)
     * "most_rated"
     * "popular"
     * "love" | "faith" | "hope" | "total"
     * "manual"
     */
    private val _offlineVideos = MutableStateFlow<List<OfflineVideo>>(emptyList())
    val offlineVideos: StateFlow<List<OfflineVideo>> = _offlineVideos.asStateFlow()

    private val _isFeaturedLoading = MutableStateFlow(false)
    val isFeaturedLoading: StateFlow<Boolean> = _isFeaturedLoading.asStateFlow()

    val favorites = favoritesGateway.favoritesFlow
    private fun buildBrowsePagingParams(forceRefresh: Boolean, refreshKey: Long = 0L): HomeBrowsePagingParams {
        val sort = (_state.value.sortBy ?: "latest").ifBlank { "latest" }
        val langs = _state.value.languageCodes
            .ifEmpty { listOf("hr", "unknown") }
            .distinct()
            .sorted()
        return HomeBrowsePagingParams(
            searchQuery = _state.value.searchQuery.trim().ifBlank { "" },
            category = _state.value.selectedCategory,
            minLove = _loveRange.value.start.toInt(),
            maxLove = _loveRange.value.endInclusive.toInt(),
            minFaith = _faithRange.value.start.toInt(),
            maxFaith = _faithRange.value.endInclusive.toInt(),
            minHope = _hopeRange.value.start.toInt(),
            maxHope = _hopeRange.value.endInclusive.toInt(),
            languages = langs,
            sortBy = sort,
            perPage = pageSize,
            refreshKey = refreshKey,
            forceRefresh = forceRefresh
        )
    }

    private fun onBrowsePageLoaded(response: PaginatedSearchResponse) {
        updateState {
            it.copy(
                hasMore = response.hasMore,
                totalResults = response.total,
                browseError = null,
                isBrowsing = false,
                isLoadingMore = false
            )
        }
    }

    // ✅ OPTIMIZOVANO: Jedan glavni StateFlow umjesto 13 redundantnih
    internal val homeState: StateFlow<HomeViewState> = _state.asStateFlow()

    suspend fun getTopSearchSuggestions(maxCount: Int = 6): List<String> {
        return try {
            // Get current language codes
            val languageCodes = _state.value.selectedContentLanguages.map(::mapLanguageToCode)
            val primaryLanguage = if (languageCodes.contains("hr")) "hr" else "unknown"

            // Call server API for popular search terms
            getPopularSearchTermsUseCase(maxCount, primaryLanguage)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            LogConfig.logError("Error getting top search suggestions: ${e.message}", e)
            // Return empty list when API fails - don't show hardcoded suggestions
            emptyList()
        }
    }

    // -----------------------------
    // Helpers
    // -----------------------------

    private val requestDeduplicator = RequestDeduplicator()
    private val pageSize = 20
    private val browseRefreshDebouncer = KeyedDebouncer(scope = viewModelScope)
    private var searchSuggestionsJob: kotlinx.coroutines.Job? = null
    private var lastAutoShuffleTs = 0L

    private fun updateState(transform: (HomeViewState) -> HomeViewState) {
        _state.update(transform)
    }

    private fun disableAutoShuffle() {
        if (_state.value.autoShuffle) {
            updateState { it.copy(autoShuffle = false) }
        }
    }

    private fun enableAutoShuffle() {
        if (!_state.value.autoShuffle) {
            updateState { it.copy(autoShuffle = true) }
        }
    }

    fun autoShuffleCurrentListIfNeeded() {
        val state = _state.value
        if (!state.autoShuffle) return
        if (state.browseVideos.size <= 1) return
        val now = System.currentTimeMillis()
        if (now - lastAutoShuffleTs < 3_000) return  // avoid hammering on recompositions
        lastAutoShuffleTs = now
        viewModelScope.launch {
            browseVideosMutex.withLock {
                updateState {
                    it.copy(
                        browseVideos = shuffleVideosUseCase.execute(it.browseVideos),
                        isBrowseOrderManual = true
                    )
                }
            }
        }
    }

    private fun computeLanguageCodes(languages: Set<Strings.Language>): List<String> =
        homeBrowseRulesUseCase.computeLanguageCodes(languages, ::mapLanguageToCode)

private fun setSelectedContentLanguages(languages: Set<Strings.Language>) {
    if (languages != _state.value.selectedContentLanguages) {
        disableAutoShuffle()
    }
    val codes = computeLanguageCodes(languages = languages)
    updateState {
        it.copy(
            selectedContentLanguages = languages,
            languageCodes = codes
            )
        }
    }

    private suspend fun onMain(block: () -> Unit) {
        withContext(Dispatchers.Main) { block() }
    }

    private fun buildBrowseCacheSignature(state: HomeViewState): String {
        return homeBrowseRulesUseCase.buildBrowseCacheSignature(
            searchQuery = state.searchQuery,
            selectedCategory = state.selectedCategory,
            languageCodes = state.languageCodes,
            sortBy = state.sortBy,
            loveRange = _loveRange.value,
            faithRange = _faithRange.value,
            hopeRange = _hopeRange.value
        )
    }

    private fun isDefaultBrowseState(state: HomeViewState): Boolean {
        return homeBrowseRulesUseCase.isDefaultBrowseState(
            searchQuery = state.searchQuery,
            selectedCategory = state.selectedCategory,
            browseRatingFilters = state.browseRatingFilters,
            minRating = MIN_RATING,
            maxRating = MAX_RATING,
            loveRange = _loveRange.value,
            faithRange = _faithRange.value,
            hopeRange = _hopeRange.value
        )
    }

    private suspend fun restoreBrowseCacheIfValid() {
        if (_state.value.browseVideos.isNotEmpty()) return
        val raw = runCatching { VideoPrefs.getBrowseCacheJson(appContext) }.getOrNull() ?: return
        val cache = runCatching { json.decodeFromString<BrowseCache>(raw) }.getOrNull() ?: return
        val signature = buildBrowseCacheSignature(state = _state.value)
        if (cache.signature != signature || cache.videos.isEmpty()) return
        onMain {
            updateState {
                it.copy(
                    browseVideos = cache.videos,
                    hasMore = cache.hasMore,
                    totalResults = cache.totalResults,
                    browseError = null
                )
            }
            _currentPage.value = if (cache.hasMore) 1 else 1
        }
    }

    private fun shouldUseCachedBrowse(): Boolean {
        val state = _state.value
        return state.browseVideos.isNotEmpty() && !state.hasMore
    }

    private suspend fun startAutoLoadAllBrowse() {
        autoLoadJob?.cancelAndJoin()
        autoLoadJob = viewModelScope.launch {
            while (isActive && _state.value.hasMore) {
                if (_state.value.isShuffling) break
                if (!_state.value.isLoadingMore) {
                    loadMoreVideos(languageCodes = _state.value.languageCodes)
                }
                delay(350)
            }
        }
    }

    // -----------------------------
    // Init
    // -----------------------------

    init {
        viewModelScope.launch {
            intents.collectLatest { intent ->
                reduce(intent = intent)
                handle(intent = intent)
            }
        }

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                updateState {
                    if (online) {
                        it.copy(isOnline = true)
                    } else {
                        it.copy(
                            isOnline = false,
                            browseError = "Nema internet konekcije",
                            isBrowsing = false,
                            isLoadingMore = false
                        )
                    }
                }
            }
        }

        makeIOCall {
            runCatching { contentLanguageManager.getSelectedContentLanguages() }
                .onSuccess { loaded ->
                    if (loaded.isNotEmpty()) {
                        onMain { setSelectedContentLanguages(languages = loaded) }
                    }
                }
        }

        viewModelScope.launch {
            LanguagePrefs.contentLanguagesFlow(appContext).collect { names ->
                if (names.isEmpty()) return@collect
                val languages = names.mapNotNull {
                    runCatching { Strings.Language.valueOf(it) }.getOrNull()
                }.toSet()
                if (languages.isNotEmpty()) {
                    setSelectedContentLanguages(languages = languages)
                }
            }
        }

        makeIOCall {
            val s = runCatching { HomePrefs.getSortBy(appContext) }.getOrNull()
            onMain {
                if (!s.isNullOrBlank()) {
                    disableAutoShuffle()
                    updateState { it.copy(sortBy = s) }
                }
            }
        }

        makeIOCall {
            restoreBrowseCacheIfValid()
        }

        makeIOCall {
            val token = runCatching { userTokenManager.getUserTokenAsync() }.getOrNull()
            onMain {
                _userToken.value = token
                if (_state.value.uiState.selectedTab == HomeTab.ForYou) {
                    loadForYouVideos(forceRefresh = false)
                }
            }
        }

        viewModelScope.launch {
            favoritesGateway.favoritesFlow
                .map { list -> list.map { it.videoId }.toSet() }
                .distinctUntilChanged()
                .collect { ids ->
                    updateState { it.copy(favoriteIds = ids) }
                }
        }

        viewModelScope.launch {
            FeatureFlagsPrefs.popularTimeVideosEnabledFlow(appContext)
                .collect { enabled ->
                    updateState {
                        it.copy(
                            popularTimeVideosEnabled = enabled,
                            popularVideos = computePopularVideos(
                                browseVideos = it.browseVideos,
                                featuredVideos = it.featuredVideos,
                                range = it.popularRange,
                                popularTimeVideosEnabled = enabled
                            )
                        )
                    }
                }
        }

        viewModelScope.launch {
            SaintsPrefs.saintOfDayNotificationsEnabledFlow(appContext)
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    if (!enabled) {
                        updateState { it.copy(showSaintDialog = false, showSaintFullDialog = false) }
                    } else if (_state.value.saintOfDay == null) {
                        loadSaintOfDay()
                    }
                }
        }
        startBrowseObservers()
        loadSearchSuggestions()
        loadHomeCategories()
    }

    private fun startBrowseObservers() {
        // ✅ OPTIMIZOVANO: Kreiraj flow-ove lokalno iz homeState
        val showBrowseFlow = homeState.map { it.uiState.showBrowse }.distinctUntilChanged()
        val searchQueryFlow = homeState.map { it.searchQuery }.distinctUntilChanged()
        val browseRatingFiltersFlow = homeState.map { it.browseRatingFilters }.distinctUntilChanged()
        val languageCodesFlow = homeState.map { it.languageCodes }.distinctUntilChanged()
        val categoryFlow = homeState.map { it.selectedCategory }.distinctUntilChanged()
        val selectedTabFlow = homeState.map { it.uiState.selectedTab }.distinctUntilChanged()

        viewModelScope.launch {
            showBrowseFlow.collectLatest { show ->
                if (!show) return@collectLatest
                val codes = _state.value.languageCodes
                if (shouldUseCachedBrowse()) {
                    loadFeaturedVideos(languageCodes = codes)
                    return@collectLatest
                }
                loadBrowseVideos(codes, resetPage = true)
                loadFeaturedVideos(languageCodes = codes)
            }
        }

        viewModelScope.launch {
            combine(showBrowseFlow, selectedTabFlow) { show, tab ->
                show && tab == HomeTab.Browse
            }
                .distinctUntilChanged()
                .collectLatest { shouldAutoLoad ->
                    if (!shouldAutoLoad) {
                        autoLoadJob?.cancelAndJoin()
                        autoLoadJob = null
                        return@collectLatest
                    }
                    startAutoLoadAllBrowse()
                }
        }

        viewModelScope.launch {
            searchQueryFlow
                .drop(1)
                .debounce(SEARCH_TYPING_DEBOUNCE_MS)
                .collectLatest {
                    if (!_state.value.uiState.showBrowse) return@collectLatest
                    if (_state.value.uiState.selectedTab != HomeTab.Browse) return@collectLatest
                    loadBrowseVideos(_state.value.languageCodes, resetPage = true)
                }
        }

        viewModelScope.launch {
            browseRatingFiltersFlow
                .drop(1)
                .debounce(APPLY_RATING_FILTERS_DEBOUNCE_MS)
                .collectLatest { filters ->
                    if (!_state.value.uiState.showBrowse) return@collectLatest
                    if (_state.value.uiState.selectedTab == HomeTab.ForYou) {
                        loadForYouVideos(forceRefresh = true)
                    } else {
                        applyRatingFilterRanges(filters = filters)
                        loadBrowseVideos(_state.value.languageCodes, resetPage = true)
                    }
                }
        }

        viewModelScope.launch {
            categoryFlow
                .drop(1)
                .debounce(FILTER_DELAY_MS)
                .collectLatest {
                    if (!_state.value.uiState.showBrowse) return@collectLatest
                    if (_state.value.uiState.selectedTab == HomeTab.ForYou) {
                        loadForYouVideos(forceRefresh = true)
                    } else {
                        loadBrowseVideos(_state.value.languageCodes, resetPage = true)
                    }
                }
        }

        viewModelScope.launch {
            languageCodesFlow
                .drop(1)
                .debounce(FILTER_DELAY_MS)
                .collectLatest { codes ->
                    loadSearchSuggestions()
                    if (!_state.value.uiState.showBrowse) return@collectLatest
                    if (_state.value.uiState.selectedTab == HomeTab.ForYou) {
                        loadForYouVideos(forceRefresh = true)
                    } else {
                        loadBrowseVideos(codes, resetPage = true)
                        loadFeaturedVideos(languageCodes = codes)
                    }
                }
        }
    }

    fun loadForYouVideos(forceRefresh: Boolean = false) {
        val token = _userToken.value?.trim().orEmpty()
        if (token.isBlank()) {
            updateState {
                it.copy(
                    isForYouLoading = false,
                    forYouError = Strings.tokenLoading,
                    forYouVideos = emptyList(),
                    forYouColdStart = false
                )
            }
            return
        }

        updateState { it.copy(isForYouLoading = true, forYouError = null) }

        makeIOCall {
            runCatching {
                ratingApiClient.getPersonalizedFeed(
                    userToken = token,
                    limit = 50,
                    category = _state.value.selectedCategory,
                    languages = _state.value.languageCodes,
                    forceRefresh = forceRefresh
                )
            }.onSuccess { response ->
                onMain {
                    if (response.success) {
                        updateState {
                            it.copy(
                                isForYouLoading = false,
                                forYouError = null,
                                forYouColdStart = response.coldStart,
                                forYouVideos = tasteGraphRanker(response.videos, limit = 50)
                            )
                        }
                    } else {
                        updateState {
                            it.copy(
                                isForYouLoading = false,
                                forYouError = response.message ?: Strings.loadError,
                                forYouVideos = emptyList(),
                                forYouColdStart = false
                            )
                        }
                    }
                }
            }.onFailure { error ->
                com.youtube.rating.android.sentry.SentryLogger.captureException(error)
                onMain {
                    updateState {
                        it.copy(
                            isForYouLoading = false,
                            forYouError = error.message ?: Strings.loadError,
                            forYouVideos = emptyList(),
                            forYouColdStart = false
                        )
                    }
                }
            }
        }
    }

    private fun computePopularVideos(
        browseVideos: List<VideoSearchResult>,
        featuredVideos: List<VideoSearchResult>,
        range: PopularRange,
        popularTimeVideosEnabled: Boolean
    ): List<VideoSearchResult> {
        val source = if (featuredVideos.isNotEmpty() && popularTimeVideosEnabled) {
            featuredVideos
        } else {
            if (browseVideos.isNotEmpty()) browseVideos else featuredVideos
        }
        if (source.isEmpty()) return emptyList()
        val base = if (!popularTimeVideosEnabled) {
            val cutoff = popularCutoffMillis(range = range)
            val filtered = source.filter { it.createdAt > 0L && it.createdAt >= cutoff }
            if (filtered.isNotEmpty()) filtered else source
        } else {
            source
        }
        if (base.size <= 10) {
            return base.sortedByDescending { it.totalRatings }
        }
        val heap = java.util.PriorityQueue<VideoSearchResult>(10, compareBy { it.totalRatings })
        for (video in base) {
            if (heap.size < 10) {
                heap.add(video)
            } else {
                val smallest = heap.peek() ?: continue
                if (video.totalRatings > smallest.totalRatings) {
                    heap.poll()
                    heap.add(video)
                }
            }
        }
        return heap.toList().sortedByDescending { it.totalRatings }
    }

    private fun popularCutoffMillis(range: PopularRange): Long {
        val days = when (range) {
            PopularRange.WEEK -> 7
            PopularRange.MONTH -> 30
            PopularRange.YEAR -> 365
        }
        return System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
    }

    private fun applyRatingFilterRanges(filters: BrowseRatingFilters) {
        _loveRange.value = if (filters.loveMin > 0) filters.loveMin.toFloat()..MAX_RATING else MIN_RATING..MAX_RATING
        _faithRange.value = if (filters.faithMin > 0) filters.faithMin.toFloat()..MAX_RATING else MIN_RATING..MAX_RATING
        _hopeRange.value = if (filters.hopeMin > 0) filters.hopeMin.toFloat()..MAX_RATING else MIN_RATING..MAX_RATING
    }

    private fun loadSearchSuggestions(maxCount: Int = 6) {
        searchSuggestionsJob?.cancel()
        searchSuggestionsJob = makeIOCall {
            val suggestions = runCatching { getTopSearchSuggestions(maxCount = maxCount) }.getOrDefault(emptyList())
            onMain { updateState { it.copy(searchSuggestions = suggestions) } }
        }
    }


    fun loadSaintOfDay() {
        makeIOCall {
            if (!SaintsPrefs.getSaintOfDayNotificationsEnabled(appContext)) return@makeIOCall
            val saint = runCatching { saintOfDayManager.getSaintOfDayOncePerDay() }.getOrNull()
            if (saint != null && saint.success) {
                val saintDate = saint.date ?: saint.fetchedAt?.take(10) ?: LocalDate.now().toString()
                val dismissedDate = runCatching { SaintsPrefs.getSaintOfDayDismissedDate(appContext) }.getOrNull()
                val shouldShow = dismissedDate != saintDate
                onMain { updateState { it.copy(saintOfDay = saint, showSaintDialog = shouldShow) } }
            }
        }
    }

    fun dismissSaintDialog() {
        val saint = _state.value.saintOfDay ?: return
        updateState { it.copy(showSaintDialog = false) }
        makeIOCall {
            val saintDate = saint.date ?: saint.fetchedAt?.take(10) ?: LocalDate.now().toString()
            runCatching { SaintsPrefs.setSaintOfDayDismissedDate(appContext, saintDate) }
        }
    }

    fun openSaintFullDialog() {
        updateState { it.copy(showSaintDialog = false, showSaintFullDialog = true) }
    }

    fun closeSaintFullDialog() {
        updateState { it.copy(showSaintFullDialog = false) }
    }

    fun loadSaintByDate(date: String) {
        makeIOCall {
            val nav = runCatching { saintOfDayManager.getSaintByDate(date) }.getOrNull()
            if (nav != null && nav.success) {
                onMain { updateState { it.copy(saintOfDay = nav, showSaintDialog = true, showSaintFullDialog = false) } }
            }
        }
    }

    fun setUrlDialogPrefill(value: String) {
        updateState { it.copy(urlDialogPrefill = value) }
    }

    fun clearUrlDialogPrefill() {
        updateState { it.copy(urlDialogPrefill = "") }
    }

    // -----------------------------
    // Public API (UI actions)
    // -----------------------------

    fun loadHomeCategories(forceRefresh: Boolean = false) {
        makeIOCall {
            try {
                val resp = getHomeCategoriesUseCase(forceRefresh)
                if (resp.success) {
                    val values = resp.categories.map { it.value }.toSet()
                    val selected = _state.value.selectedCategory?.takeIf { it in values }
                    onMain {
                        updateState { it.copy(homeCategories = resp.categories, selectedCategory = selected) }
                        persistSelectedCategory(category = selected)
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                // ignore errors, categories are optional
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val trimmed = query.trimStart()
        if (_state.value.searchQuery == trimmed) return
        disableAutoShuffle()
        updateState { it.copy(searchQuery = trimmed) }
    }

    fun maybePrefetchVideoInfo(videoIds: List<String>) {
        if (!perfProfile.prefetchEnabled) return
        if (videoIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val limited = videoIds.distinct().filter { it.isNotBlank() }.take(perfProfile.prefetchBatchSize)
        if (limited.isEmpty()) return
        val sig = idsSignature(ids = limited)
        if (sig == lastPrefetchedSig && now - lastPrefetchTime < 15_000L) return
        if (now - lastPrefetchTime < 5_000L) return
        lastPrefetchTime = now
        lastPrefetchedSig = sig
        viewModelScope.launch(ioDispatcher) {
            runCatching { youTubeInfoService.prefetch(limited) }
                .onFailure { com.youtube.rating.android.sentry.SentryLogger.captureException(it) }
        }
    }

    fun requestVideoStats(videoId: String) {
        if (videoId.isBlank()) return
        videoStatsCache.get(videoId)?.let { cached ->
            applyVideoStats(videoId = videoId, stats = cached)
            return
        }
        viewModelScope.launch(ioDispatcher) {
            runCatching { ratingApiClient.getVideoStats(videoId) }
                .onSuccess { stats ->
                    videoStatsCache.put(videoId, stats)
                    withContext(Dispatchers.Main) { applyVideoStats(videoId = videoId, stats = stats) }
                }
                .onFailure { com.youtube.rating.android.sentry.SentryLogger.captureException(it) }
        }
    }

    fun syncBrowseSnapshot(videos: List<VideoSearchResult>) {
        val state = _state.value
        val currentIds = state.browseVideos.map { it.videoId }
        val nextIds = videos.map { it.videoId }
        if (currentIds == nextIds) return
        if (state.isBrowseOrderManual && currentIds.toSet() == nextIds.toSet()) return
        updateState {
            it.copy(
                browseVideos = videos,
                isBrowseOrderManual = false,
                popularVideos = computePopularVideos(
                    browseVideos = videos,
                    featuredVideos = it.featuredVideos,
                    range = it.popularRange,
                    popularTimeVideosEnabled = it.popularTimeVideosEnabled
                )
            )
        }
    }

    private fun applyVideoStats(videoId: String, stats: VideoStats) {
        val current = _state.value.uiState.showVideoDetailsDialog
            ?.takeIf { it.videoId == videoId }
            ?: _state.value.browseVideos.firstOrNull { it.videoId == videoId }
            ?: return

        val updated = current.copy(
            totalRatings = stats.totalRatings,
            avgLove = stats.averageLove,
            avgFaith = stats.averageFaith,
            avgHope = stats.averageHope,
            avgTotal = listOf(stats.averageLove, stats.averageFaith, stats.averageHope).average(),
            channelName = stats.channelName ?: current.channelName
        )

        updateUiState { it.copy(showVideoDetailsDialog = updated) }
        updateBrowseVideo(videoId) { existing ->
            existing.copy(
                totalRatings = updated.totalRatings,
                avgLove = updated.avgLove,
                avgFaith = updated.avgFaith,
                avgHope = updated.avgHope,
                avgTotal = updated.avgTotal,
                channelName = updated.channelName
            )
        }
    }

    private fun idsSignature(ids: List<String>): Long {
        var acc = 1125899906842597L
        for (s in ids) acc = (acc * 31L) + s.hashCode().toLong()
        return acc
    }

    fun selectCategory(value: String?) {
        val normalized = value?.takeIf { it.isNotBlank() }
        if (_state.value.selectedCategory == normalized) return
        SentryLogger.metricCount("home_category_select", 1.0)
        disableAutoShuffle()
        updateState { it.copy(selectedCategory = normalized) }
        persistSelectedCategory(category = normalized)
        if (_state.value.uiState.selectedTab == HomeTab.ForYou) {
            loadForYouVideos(forceRefresh = true)
        }
    }

    fun updateLoveRange(range: ClosedFloatingPointRange<Float>) {
        if (_loveRange.value == range) return
        _loveRange.value = range
        disableAutoShuffle()
    }

    fun updateFaithRange(range: ClosedFloatingPointRange<Float>) {
        if (_faithRange.value == range) return
        _faithRange.value = range
        disableAutoShuffle()
    }

    fun updateHopeRange(range: ClosedFloatingPointRange<Float>) {
        if (_hopeRange.value == range) return
        _hopeRange.value = range
        disableAutoShuffle()
    }

    /**
     * ✅ Ovdje možeš postaviti:
     * - "latest"
     * - "most_rated"
     * - "popular"
     * - "love" | "faith" | "hope" | "total"
     * - "manual"
     */
    fun updateSortBy(sortBy: String?) {
        if (_state.value.sortBy == sortBy) return
        disableAutoShuffle()
        updateState { it.copy(sortBy = sortBy) }
    }

    fun onSortSelected(sort: String?) {
        val normalized = sort?.takeIf { it.isNotBlank() }
        if (_state.value.sortBy == normalized) return
        updateSortBy(normalized)
        persistSortBy(sort = normalized)
        debounceBrowseRefresh(key = "sort", delayMs = SORT_DEBOUNCE_MS)
    }

    private fun persistSortBy(sort: String?) {
        makeIOCall {
            runCatching { HomePrefs.setSortBy(appContext, sort) }
        }
    }

    private fun debounceBrowseRefresh(key: String, delayMs: Long) {
        browseRefreshDebouncer.debounce(key, delayMs) {
            loadBrowseVideos(_state.value.languageCodes, resetPage = true)
        }
    }

    fun toggleContentLanguage(language: Strings.Language) {
        val current = _state.value.selectedContentLanguages
        val next = current.toMutableSet()

        if (next.contains(language)) {
            if (next.size <= 1) return
            next.remove(language)
        } else {
            next.add(language)
        }

        setSelectedContentLanguages(languages = next)
        makeIOCall {
            runCatching { contentLanguageManager.saveContentLanguages(next) }
        }
    }

    private var featuredJob: kotlinx.coroutines.Job? = null

    internal fun loadFeaturedVideos(
        languageCodes: List<String> = emptyList(),
        range: PopularRange = _state.value.popularRange
    ) {
        featuredJob = makeIOCall {
            featuredJob?.cancelAndJoin()
            val transaction = SentryLogger.startTransaction("home.featured", "ui.load")
            transaction.setTag("hasLanguageFilter", languageCodes.isNotEmpty().toString())
            transaction.setTag("range", range.name.lowercase())
            _isFeaturedLoading.value = true
            try {
                val known = languageCodes.filter { it != "unknown" }
                val lang = if (known.size == 1) known.first() else null
                val resp = getTopVideosUseCase(
                    type = "popular",
                    category = null,
                    language = lang,
                    limit = 10,
                    range = when (range) {
                        PopularRange.WEEK -> "week"
                        PopularRange.MONTH -> "month"
                        PopularRange.YEAR -> "year"
                    }
                )
                if (resp.success) {
                    onMain {
                        updateState {
                            it.copy(
                                featuredVideos = resp.videos,
                                popularVideos = computePopularVideos(
                                    browseVideos = it.browseVideos,
                                    featuredVideos = resp.videos,
                                    range = it.popularRange,
                                    popularTimeVideosEnabled = it.popularTimeVideosEnabled
                                )
                            )
                        }
                    }
                    transaction.setStatus(SpanStatus.OK)
                } else {
                    transaction.setStatus(SpanStatus.INTERNAL_ERROR)
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                // silently fail - featured is optional
                transaction.setStatus(SpanStatus.INTERNAL_ERROR)
            } finally {
                _isFeaturedLoading.value = false
                SentryLogger.finishTransaction(transaction, transaction.status ?: SpanStatus.OK)
            }
        }
    }

    /**
     * Paging-based refresh (resetPage=true). Append is handled by Paging.
     */
    fun loadBrowseVideos(
        languageCodes: List<String>,
        resetPage: Boolean = true,
        forceRefresh: Boolean = false,
        preserveOrder: Boolean = false
    ) {
        if (!_state.value.isOnline) {
            updateState {
                it.copy(
                    browseError = "Nema internet konekcije. Provjerite mrežu.",
                    isBrowsing = false,
                    isLoadingMore = false
                )
            }
            return
        }
        if (!resetPage) return

        updateState {
            it.copy(
                isBrowsing = true,
                isLoadingMore = false,
                browseError = null,
                hasMore = true,
                isBrowseOrderManual = if (preserveOrder) it.isBrowseOrderManual else false,
                languageCodes = languageCodes
            )
        }
        if (!preserveOrder) {
            updateState { it.copy(browseVideos = emptyList()) }
        }
        browsePagingParams.value = buildBrowsePagingParams(
            forceRefresh = forceRefresh,
            refreshKey = System.currentTimeMillis()
        )
    }

    fun loadMoreVideos(languageCodes: List<String>) {
        // No-op: paging handles append automatically.
    }

    fun clearBrowseError() {
        updateState { it.copy(browseError = null) }
    }

    fun resetFilters() {
        _loveRange.value = MIN_RATING..MAX_RATING
        _faithRange.value = MIN_RATING..MAX_RATING
        _hopeRange.value = MIN_RATING..MAX_RATING
        enableAutoShuffle()
        updateState {
            it.copy(
                searchQuery = "",
                sortBy = null,
                browseError = null,
                hasMore = true,
                totalResults = 0,
                selectedCategory = null
            )
        }
        persistSelectedCategory(category = null)
        _currentPage.value = 1

        resetBrowseRatingFilters()
    }

    fun upsertBrowseVideo(video: VideoSearchResult) {
        viewModelScope.launch {
            // 🔒 Mutex protection: prevent race with loadBrowseVideos/shuffleBrowseVideos
            browseVideosMutex.withLock {
                val existing = _state.value.browseVideos
                val without = existing.filterNot { it.videoId == video.videoId }
                val currentTotal = _state.value.totalResults
                updateState {
                    it.copy(
                        browseVideos = listOf(video) + without,
                        totalResults = currentTotal + if (existing.any { it.videoId == video.videoId }) 0 else 1,
                        browseError = null
                    )
                }
            }
        }
    }

    fun updateBrowseVideo(videoId: String, transform: (VideoSearchResult) -> VideoSearchResult) {
        viewModelScope.launch {
            // 🔒 Mutex protection: prevent race with loadBrowseVideos/shuffleBrowseVideos
            browseVideosMutex.withLock {
                val existing = _state.value.browseVideos
                if (existing.none { it.videoId == videoId }) return@withLock
                val updated = existing.map { if (it.videoId == videoId) transform(it) else it }
                updateState { it.copy(browseVideos = updated) }
            }
        }
    }

    fun shuffleBrowseVideos(languageCodes: List<String>) {
        val existing = _state.value.browseVideos
        if (existing.size <= 1) return
        if (_state.value.isShuffling) return

        val total = _state.value.totalResults
        val needsFetchAll = _state.value.hasMore || (total > 0 && existing.size < total)

        if (!needsFetchAll) {
            // 🔒 Mutex protection: prevent race with loadBrowseVideos
            viewModelScope.launch {
                browseVideosMutex.withLock {
                    updateState {
                        it.copy(
                            browseVideos = shuffleVideosUseCase.execute(existing),
                            isBrowseOrderManual = true
                        )
                    }
                }
            }
            return
        }

        // Avoid racing with paging / refresh. User can tap again after loading finishes.
        if (_state.value.isBrowsing || _state.value.isLoadingMore) return

        makeIOCall {
            updateState { it.copy(isShuffling = true) }
            try {
                val q = _state.value.searchQuery.trim().ifBlank { null }
                val sort = (_state.value.sortBy ?: "latest").ifBlank { "latest" }

                val requestLimit = 100 // backend max for /api/v2/videos/search.php

                val all = mutableListOf<VideoSearchResult>()
                val seenIds = HashSet<String>(existing.size * 2)

                var page = 1
                var totalFromServer = 0
                var totalPages = Int.MAX_VALUE

                while (page <= totalPages) {
                    val resp = searchVideosUseCase(
                        searchQuery = q,
                        minLove = _loveRange.value.start.toInt(),
                        maxLove = _loveRange.value.endInclusive.toInt(),
                        minFaith = _faithRange.value.start.toInt(),
                        maxFaith = _faithRange.value.endInclusive.toInt(),
                        minHope = _hopeRange.value.start.toInt(),
                        maxHope = _hopeRange.value.endInclusive.toInt(),
                        category = _state.value.selectedCategory,
                        sortBy = sort,
                        languages = languageCodes,
                        page = page,
                        perPage = requestLimit,
                        forceRefresh = true
                    )

                    if (!resp.success) break

                    totalFromServer = resp.total
                    if (resp.totalPages > 0) totalPages = resp.totalPages

                    // De-dupe defensively (shouldn't happen, but protects from paging/caching glitches).
                    resp.videos.forEach { v ->
                        if (seenIds.add(v.videoId)) all.add(v)
                    }

                    if (!resp.hasMore || resp.videos.isEmpty()) break
                    page++
                }

                if (all.isEmpty()) {
                    // 🔒 Mutex protection: prevent race with loadBrowseVideos
                    browseVideosMutex.withLock {
                        onMain {
                            updateState {
                                it.copy(
                                    browseVideos = shuffleVideosUseCase.execute(existing),
                                    isBrowseOrderManual = true
                                )
                            }
                        }
                    }
                    return@makeIOCall
                }

                val shuffled = shuffleVideosUseCase.execute(all)
                // 🔒 Mutex protection: prevent race with loadBrowseVideos
                browseVideosMutex.withLock {
                    onMain {
                        updateState {
                            it.copy(
                                browseVideos = shuffled,
                                isBrowseOrderManual = true,
                                hasMore = false,
                                totalResults = if (totalFromServer > 0) totalFromServer else all.size,
                                browseError = null
                            )
                        }
                        _currentPage.value = 1
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                LogConfig.logError("Error shuffling browse videos: ${e.message}", e)
                // 🔒 Mutex protection: prevent race with loadBrowseVideos
                browseVideosMutex.withLock {
                    onMain {
                        // Fallback: at least shuffle currently loaded videos.
                        updateState {
                            it.copy(
                                browseVideos = shuffleVideosUseCase.execute(existing),
                                isBrowseOrderManual = true
                            )
                        }
                    }
                }
            } finally {
                updateState { it.copy(isShuffling = false) }
            }
        }
    }

    fun addToFavorites(video: VideoSearchResult, onComplete: (Boolean) -> Unit = {}) {
        makeIOCall {
            val success = runCatching {
                    val favorite = FavoriteVideo(
                        videoId = video.videoId,
                        title = video.title,
                        thumbnail = video.thumbnail,
                        channelName = video.channelName,
                        avgLove = video.avgLove,
                        avgFaith = video.avgFaith,
                        avgHope = video.avgHope,
                        totalRatings = video.totalRatings,
                        category = null
                    )
                favoritesGateway.addFavorite(favorite)
                true
            }.getOrElse {
                LogConfig.logError("Error adding to favorites: ${it.message}", it)
                false
            }

            withContext(Dispatchers.Main) { onComplete(success) }
        }
    }

    fun removeFromFavorites(videoId: String, onComplete: (Boolean) -> Unit = {}) {
        makeIOCall {
            val success = runCatching {
                favoritesGateway.removeFavorite(videoId)
                true
            }.getOrElse {
                LogConfig.logError("Error removing from favorites: ${it.message}", it)
                false
            }
            withContext(Dispatchers.Main) { onComplete(success) }
        }
    }

    fun loadOfflineVideos() {
        makeIOCall {
            runCatching {
                offlineRepository.getAllVideosFlow().first()
            }.onSuccess { list ->
                onMain { _offlineVideos.value = list }
            }.onFailure {
                LogConfig.logError("Error loading offline videos: ${it.message}", it)
            }
        }
    }

    fun reportVideo(
        videoId: String,
        reason: String,
        userToken: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        makeIOCall {
            val (success, message) = runCatching {
                val response = reportVideoUseCase(videoId, userToken, reason)
                if (response.success) true to null else false to response.message
            }.getOrElse {
                LogConfig.logError("Error reporting video: ${it.message}", it)
                false to it.message
            }

            withContext(Dispatchers.Main) { onComplete(success, message) }
        }
    }

    fun submitReport(videoId: String) {
        val token = _userToken.value
        if (token.isNullOrBlank()) {
            updateUiState { it.copy(reportState = it.reportState.fail(Strings.tokenLoading)) }
            return
        }

        updateUiState { it.copy(reportState = it.reportState.start()) }
        reportVideo(videoId = videoId, userToken = token, reason = REPORT_REASON_USER) { success, error ->
            if (success) {
                updateUiState { it.copy(reportState = it.reportState.ok(Strings.thanksReport)) }
                viewModelScope.launch {
                    delay(DELAY_REPORT_DIALOG_MS)
                    updateUiState {
                        it.copy(
                            showReportDialog = null,
                            reportState = it.reportState.reset()
                        )
                    }
                }
            } else {
                updateUiState { it.copy(reportState = it.reportState.fail(error ?: Strings.reportError)) }
            }
        }
    }

    fun updateUiState(transform: (HomeScreenUiState) -> HomeScreenUiState) {
        val updated = transform(_state.value.uiState)
        if (updated != _state.value.uiState) {
            updateState { it.copy(uiState = updated) }
            persistUiState(state = updated)
        }
    }

    fun updateBrowseRatingFilters(transform: (BrowseRatingFilters) -> BrowseRatingFilters) {
        val updated = transform(_state.value.browseRatingFilters)
        if (updated != _state.value.browseRatingFilters) {
            disableAutoShuffle()
            updateState { it.copy(browseRatingFilters = updated) }
            persistBrowseRatingFilters(filters = updated)
        }
    }

    fun resetBrowseRatingFilters() {
        enableAutoShuffle()
        updateBrowseRatingFilters { BrowseRatingFilters() }
    }

    private fun restoreUiState(): HomeScreenUiState {
        return runCatching {
            savedStateHandle.get<String>(AppKeys.SavedState.Home.UI_STATE)
                ?.let { json.decodeFromString<HomeScreenUiState>(it) }
        }.getOrNull() ?: HomeScreenUiState()
    }

    private fun restoreBrowseRatingFilters(): BrowseRatingFilters {
        return runCatching {
            savedStateHandle.get<String>(AppKeys.SavedState.Home.BROWSE_FILTERS)
                ?.let { json.decodeFromString<BrowseRatingFilters>(it) }
        }.getOrNull() ?: BrowseRatingFilters()
    }

    private fun restoreSelectedCategory(): String? {
        return runCatching { savedStateHandle.get<String>(AppKeys.SavedState.Home.SELECTED_CATEGORY) }.getOrNull()
    }

    private fun persistUiState(state: HomeScreenUiState) {
        savedStateHandle[AppKeys.SavedState.Home.UI_STATE] = json.encodeToString(state)
    }

    private fun persistBrowseRatingFilters(filters: BrowseRatingFilters) {
        savedStateHandle[AppKeys.SavedState.Home.BROWSE_FILTERS] = json.encodeToString(filters)
    }

    private fun persistSelectedCategory(category: String?) {
        savedStateHandle[AppKeys.SavedState.Home.SELECTED_CATEGORY] = category
    }

    // -----------------------------
    // Internal
    // -----------------------------

    private fun buildRequestKey(key: SearchCacheKey): String {
        return homeBrowseRulesUseCase.buildRequestKey(key)
    }

    private fun mapLanguageToCode(lang: Strings.Language): String = when (lang) {
        Strings.Language.ENGLISH -> "en"
        Strings.Language.CROATIAN -> "hr"
        Strings.Language.GERMAN -> "de"
    }


}

private class KeyedDebouncer(
    private val scope: CoroutineScope
) {
    private val jobs = mutableMapOf<String, Job>()

    fun debounce(key: String, delayMs: Long, block: suspend () -> Unit) {
        jobs[key]?.cancel()
        jobs[key] = scope.launch {
            delay(delayMs)
            block()
        }
    }
}
