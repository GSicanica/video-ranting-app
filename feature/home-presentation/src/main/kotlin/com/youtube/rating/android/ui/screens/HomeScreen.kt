@file:Suppress("FunctionName")
@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    FlowPreview::class
)

package com.youtube.rating.android.ui.screens

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.youtube.rating.android.cache.ApiCache
import com.youtube.rating.android.data.ClipsRepository
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.prefs.FeatureFlagsPrefs
import com.youtube.rating.android.data.prefs.HomePrefs
import com.youtube.rating.android.data.prefs.ScrollEffectsPrefs
import com.youtube.rating.android.domain.usecase.home.GetHomeCategoriesUseCase
import com.youtube.rating.android.domain.usecase.home.GetPopularSearchTermsUseCase
import com.youtube.rating.android.domain.usecase.home.GetTopVideosUseCase
import com.youtube.rating.android.domain.usecase.home.ReportVideoUseCase
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.core.designsystem.components.PullToRefreshBox
import com.youtube.rating.core.designsystem.components.SimpleRatingSelector
import com.youtube.rating.android.ui.models.BrowseRatingFilters
import com.youtube.rating.android.ui.models.HomeTab
import com.youtube.rating.android.ui.models.RatingDraft
import com.youtube.rating.android.ui.models.VideoCloseAction
import com.youtube.rating.android.ui.screens.home.HomeCloseChoiceDialog
import com.youtube.rating.android.ui.screens.home.HomeDeleteVideoDialog
import com.youtube.rating.android.ui.screens.home.HomePrefetchController
import com.youtube.rating.android.ui.screens.home.HomeSaintDialogs
import com.youtube.rating.android.ui.screens.home.HomeScreenCallbacks
import com.youtube.rating.android.ui.screens.home.HomeScreenHelpers
import com.youtube.rating.android.ui.screens.home.dialogs.VideoDetailsDialog
import com.youtube.rating.android.home.HomeContract
import com.youtube.rating.android.home.PopularRange
import com.youtube.rating.android.home.RandomResult
import com.youtube.rating.android.util.VideoSource
import com.youtube.rating.android.util.detectVideoSource
import com.youtube.rating.android.util.extractYouTubeVideoId
import com.youtube.rating.android.utils.AdminManager
import com.youtube.rating.android.utils.AnalyticsManager
import com.youtube.rating.android.utils.SaintOfDayManager
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.utils.ViewPreferencesManager
import com.youtube.rating.android.utils.getLanguageName
import com.youtube.rating.android.utils.rememberHapticFeedback
import com.youtube.rating.android.viewmodel.HomeViewModel
import com.youtube.rating.android.viewmodel.HomeViewModelFactory
import com.youtube.rating.android.viewmodel.RatingViewModel
import com.youtube.rating.android.viewmodel.RatingViewModelFactory
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.VideoSearchResult
import com.youtube.rating.shared.models.VideoStats
import com.youtube.rating.core.coroutines.makeIOCall
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// -----------------------------------------------------------------------------
// Const
// -----------------------------------------------------------------------------
private const val DELAY_SUCCESS_MESSAGE_MS = 500L
private const val EMPTY_STATE_ICON_SIZE = 64

// -----------------------------------------------------------------------------
// HomeScreen
// -----------------------------------------------------------------------------
@Composable
fun HomeScreen(
    viewModel: RatingViewModel? = null,
    adminManager: AdminManager = koinInject(),
    initialVideoId: String? = null,
    initialStartSeconds: Int = 0,
    isHomeActive: Boolean = true,
    homeTabClickTick: Long = 0L,
    onPlayVideo: (OfflineVideo) -> Unit = {},
    onStartFloatingVideo: (VideoSearchResult, Int, VideoCloseAction) -> Unit,
    onStopFloatingVideo: () -> Unit,
    isInPipMode: Boolean,
    onFullScreenVideoActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val deps = rememberHomeScreenDependencies()
    val app = remember(context) { context.applicationContext as? Application }

    val activity = context.findActivityOrNull()
    if (activity == null || app == null) {
        Text(Strings.missingActivityContext)
        return
    }

    val ratingViewModel = viewModel ?: viewModel(
        viewModelStoreOwner = activity,
        factory = deps.ratingViewModelFactory(application = app)
    )
    val homeViewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = deps.homeViewModelFactory(owner = activity)
    )

    // ✅ manje “remember { flow }” šema, direktno
    val homeState by homeViewModel.homeState.collectAsStateWithLifecycle()
    val browsePagingItems = homeViewModel.browsePagingData.collectAsLazyPagingItems()

    val sortBy = homeState.sortBy
    val browseVideos = browsePagingItems.itemSnapshotList.items
    val displayedBrowseVideos = if (homeState.isBrowseOrderManual) homeState.browseVideos else browseVideos
    val isBrowsing = browsePagingItems.loadState.refresh is LoadState.Loading
    val browseError = (browsePagingItems.loadState.refresh as? LoadState.Error)
        ?.error?.message
        ?: homeState.browseError
    val searchQuery = homeState.searchQuery
    val appendState = browsePagingItems.loadState.append
    val isLoadingMore = appendState is LoadState.Loading
    val hasMore = (appendState as? LoadState.NotLoading)?.endOfPaginationReached != true
    val isShuffling = homeState.isShuffling
    val totalResults = homeState.totalResults
    val favoriteIdSet = homeState.favoriteIds // ✅ bez toSet() / alloc
    val isOnline = homeState.isOnline
    val selectedContentLanguages = homeState.selectedContentLanguages
    val featuredVideos = homeState.featuredVideos
    val popularRange = homeState.popularRange
    val selectedCategory = homeState.selectedCategory
    val uiState = homeState.uiState
    val currentPlaybackSeconds = homeState.currentPlaybackSeconds
    val selectedTab = uiState.selectedTab
    val languageCodes = homeState.languageCodes

    LaunchedEffect(browseVideos) {
        homeViewModel.syncBrowseSnapshot(browseVideos)
    }
    val browseRatingFilters = homeState.browseRatingFilters
    val autoShuffle = homeState.autoShuffle
    val urlDialogPrefill = homeState.urlDialogPrefill
    val pendingCloseVideo = homeState.pendingCloseVideo
    val pendingCloseSeconds = homeState.pendingCloseSeconds
    val showCloseChoiceDialog = homeState.showCloseChoiceDialog

    val disableScrollEffects by ScrollEffectsPrefs.disableFlow(context)
        .collectAsStateWithLifecycle(initialValue = true)

    val popularTimeVideosEnabled = homeState.popularTimeVideosEnabled

    val uiScope = rememberCoroutineScope()
    val scope = rememberCoroutineScope()
    val haptic = rememberHapticFeedback()

    val activeFilterCount by remember(browseRatingFilters) {
        derivedStateOf {
            var c = 0
            if (browseRatingFilters.loveMin > 0) c++
            if (browseRatingFilters.faithMin > 0) c++
            if (browseRatingFilters.hopeMin > 0) c++
            c
        }
    }

    val popularVideos = homeState.popularVideos
    var lastHandledHomeTabClickTick by rememberSaveable {
        mutableLongStateOf(homeTabClickTick)
    }

    BackHandler(enabled = selectedTab != HomeTab.Browse) {
        homeViewModel.dispatch(HomeContract.Intent.SelectTab(HomeTab.Browse))
    }

    LaunchedEffect(uiState.showVideoDetailsDialog?.videoId) {
        homeViewModel.setPlaybackSeconds(0f)
    }


    val videoInfo by ratingViewModel.currentVideoInfo.collectAsStateWithLifecycle()
    val submitState by ratingViewModel.submitState.collectAsStateWithLifecycle()

    val searchSuggestions = homeState.searchSuggestions
    val quickSearchTerms by HomePrefs.quickSearchTermsFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val isAdminMode by AdminPrefs.adminModeFlow(context)
        .collectAsStateWithLifecycle(initialValue = adminManager.isAdminMode())

    val saintPayload = homeState.saintOfDay
    val showSaintDialog = homeState.showSaintDialog
    val showSaintFullDialog = homeState.showSaintFullDialog

    val perfProfile =
        remember(context) { com.youtube.rating.android.utils.PerformanceProfile.get(context) }

    val isFavorite: (String) -> Boolean = rememberUpdatedState(newValue = { id: String ->
        favoriteIdSet.contains(id)
    }).value

    // Draft
    val draftState = rememberRatingDraft()
    var draft by draftState

    // Actions
    val dispatchHomeIntent = remember(homeViewModel) {
        { intent: HomeContract.Intent -> homeViewModel.dispatch(intent) }
    }

    LaunchedEffect(popularTimeVideosEnabled) {
        if (!popularTimeVideosEnabled && popularRange != PopularRange.WEEK) {
            dispatchHomeIntent(
                HomeContract.Intent.SelectPopularRange(PopularRange.WEEK)
            )
        }
    }

    fun openUrlDialog(prefill: String = "") {
        homeViewModel.setUrlDialogPrefill(prefill)
        homeViewModel.updateUiState { it.copy(showUrlInputDialog = true) }
    }

    fun startRatingFromUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        homeViewModel.clearUrlDialogPrefill()
        homeViewModel.updateUiState {
            it.copy(
                showUrlInputDialog = false,
                showBrowseControlsSheet = false,
                showSearchField = false,
                showBrowse = false
            )
        }
        draft = draft.copy(url = trimmed)
    }

    fun applyVideoCloseAction(
        action: VideoCloseAction,
        video: VideoSearchResult,
        startSeconds: Int
    ) {
        onStartFloatingVideo(video, startSeconds.coerceAtLeast(0), VideoCloseAction.MINI_PLAYER)
    }

    fun handleVideoClose(video: VideoSearchResult) {
        val startSeconds = currentPlaybackSeconds.toInt()
        homeViewModel.updateUiState { it.copy(showVideoDetailsDialog = null) }
        if (!isAdminMode) {
            applyVideoCloseAction(VideoCloseAction.MINI_PLAYER, video, startSeconds)
            return
        }
        homeViewModel.requestCloseChoice(video, startSeconds)
    }

    fun refreshBrowse(
        resetPage: Boolean = true,
        forceRefresh: Boolean = false,
        preserveOrder: Boolean = false
    ) {
        if (resetPage) {
            dispatchHomeIntent(
                HomeContract.Intent.RefreshBrowse(
                    forceRefresh = forceRefresh,
                    preserveOrder = preserveOrder
                )
            )
        }
        if (forceRefresh || resetPage) {
            homeViewModel.loadFeaturedVideos(languageCodes)
        }
    }

    fun selectTab(tab: HomeTab) {
        dispatchHomeIntent(HomeContract.Intent.SelectTab(tab))
    }

    // Callbacks
    val callbacks = remember(homeViewModel, haptic, favoriteIdSet, context) {
        HomeScreenCallbacks(
            context = context,
            homeViewModel = homeViewModel,
            haptic = haptic,
            favoriteSet = favoriteIdSet,
            refreshBrowse = { resetPage -> refreshBrowse(resetPage) }
        )
    }

    // Init browse if no video
    LaunchedEffect(draft.url, initialVideoId) {
        if (draft.url.isBlank() && initialVideoId.isNullOrBlank()) {
            homeViewModel.updateUiState { it.copy(showBrowse = true) }
        }
    }

    // Sheets
    val quickRateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val quickActionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Cache windows
    val gridCacheState = remember(perfProfile) {
        LazyLayoutCacheWindow(
            ahead = perfProfile.gridCacheAheadDp.dp,
            behind = perfProfile.gridCacheBehindDp.dp
        )
    }
    val listCacheState = remember(perfProfile) {
        LazyLayoutCacheWindow(
            ahead = perfProfile.listCacheAheadDp.dp,
            behind = perfProfile.listCacheBehindDp.dp
        )
    }

    val gridScrollState = rememberLazyGridState(
        initialFirstVisibleItemIndex = uiState.gridFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = uiState.gridFirstVisibleItemScrollOffset,
        cacheWindow = gridCacheState
    )
    val listScrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.listFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = uiState.listFirstVisibleItemScrollOffset,
        cacheWindow = listCacheState
    )

    val shouldShowBrowse by remember { derivedStateOf { uiState.showBrowse } }
    val isGridView by remember { derivedStateOf { uiState.isGridView } }
    var showScrollToTop by remember { mutableStateOf(false) }
    LaunchedEffect(isGridView, gridScrollState, listScrollState) {
        snapshotFlow {
            if (isGridView) gridScrollState.firstVisibleItemIndex else listScrollState.firstVisibleItemIndex
        }.distinctUntilChanged().collect { idx ->
            showScrollToTop = if (showScrollToTop) {
                idx >= (if (isGridView) 4 else 6)
            } else {
                idx >= (if (isGridView) 8 else 10)
            }
        }
    }

    val shouldPrefetch = remember(perfProfile) {
        { _: List<String> -> perfProfile.prefetchEnabled }
    }

    val performPrefetch = remember(homeViewModel) {
        { ids: List<String> -> homeViewModel.maybePrefetchVideoInfo(ids) }
    }

    LaunchedEffect(isHomeActive, autoShuffle, browseVideos) {
        if (isHomeActive && autoShuffle) {
            homeViewModel.autoShuffleCurrentListIfNeeded()
        }
    }

    // Save scroll positions only on dispose
    DisposableEffect(homeViewModel, gridScrollState, listScrollState) {
        onDispose {
            homeViewModel.updateUiState {
                it.copy(
                    gridFirstVisibleItemIndex = gridScrollState.firstVisibleItemIndex,
                    gridFirstVisibleItemScrollOffset = gridScrollState.firstVisibleItemScrollOffset,
                    listFirstVisibleItemIndex = listScrollState.firstVisibleItemIndex,
                    listFirstVisibleItemScrollOffset = listScrollState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    HomePrefetchController(
        shouldShowBrowse = shouldShowBrowse,
        isGridView = isGridView,
        gridScrollState = gridScrollState,
        listScrollState = listScrollState,
        browseVideos = displayedBrowseVideos,
        isBrowsing = isBrowsing,
        isLoadingMore = isLoadingMore,
        isShuffling = isShuffling,
        perfProfile = perfProfile,
        shouldPrefetch = shouldPrefetch,
        performPrefetch = performPrefetch
    )

    val randomLoading = remember { mutableStateOf(false) }
    val randomProgress = remember { Animatable(0f) }

    val isRandomLocked by homeViewModel.isRandomLocked.collectAsStateWithLifecycle(initialValue = false)
    val onRandomVideo: () -> Unit =
        remember(context, homeViewModel, displayedBrowseVideos, hasMore, isShuffling, languageCodes, isGridView) {
            {
                when (homeViewModel.handleRandomRequest(
                    hasMore,
                    isShuffling,
                    displayedBrowseVideos.size,
                    languageCodes
                )) {
                    RandomResult.Locked -> {
                        Toast.makeText(
                            context,
                            "Random je zakljucan do ponovnog pokretanja aplikacije.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    RandomResult.WaitForLoad ->
                        Toast.makeText(context, Strings.waitForVideoLoad, Toast.LENGTH_SHORT).show()

                    RandomResult.AlreadyShuffling ->
                        Toast.makeText(context, Strings.alreadyShuffling, Toast.LENGTH_SHORT).show()

                    RandomResult.NotEnoughVideos -> {
                        Toast.makeText(
                            context,
                            if (displayedBrowseVideos.isEmpty()) Strings.noVideosToShuffle else Strings.notEnoughToShuffle,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    RandomResult.Shuffled -> {
                        scope.launch {
                            randomLoading.value = true
                            randomProgress.snapTo(0f)
                            scrollToTopSafe(isGridView, gridScrollState, listScrollState)
                            randomProgress.animateTo(
                                1f,
                                animationSpec = tween(durationMillis = 2500)
                            )
                            randomLoading.value = false
                        }
                    }
                }
            }
        }

    LaunchedEffect(homeTabClickTick, selectedTab, isGridView, gridScrollState, listScrollState) {
        val isNewHomeTabClick = homeTabClickTick > 0L && homeTabClickTick != lastHandledHomeTabClickTick
        if (!isNewHomeTabClick) return@LaunchedEffect

        lastHandledHomeTabClickTick = homeTabClickTick
        if (selectedTab == HomeTab.Browse) {
            uiScope.launch {
                scrollToTopSafe(isGridView, gridScrollState, listScrollState)
            }
        }
    }

    // Close sheet when leaving browse / opening details / leaving home
    LaunchedEffect(uiState.showBrowse, uiState.showVideoDetailsDialog, isHomeActive) {
        if (!uiState.showBrowse || uiState.showVideoDetailsDialog != null || !isHomeActive) {
            homeViewModel.updateUiState { it.copy(showBrowseControlsSheet = false) }
        }
    }

    LaunchedEffect(isHomeActive, uiState.showVideoDetailsDialog?.videoId) {
        if (!isHomeActive) {
            uiState.showVideoDetailsDialog?.let { video ->
                handleVideoClose(video)
            }
        }
    }

    val latestVideoDialog by rememberUpdatedState(uiState.showVideoDetailsDialog)
    DisposableEffect(Unit) {
        onDispose {
            if (activity.isFinishing || activity.isChangingConfigurations) return@onDispose
            latestVideoDialog?.let { video ->
                handleVideoClose(video)
            }
        }
    }

    LaunchedEffect(displayedBrowseVideos, uiState.showVideoDetailsDialog?.videoId) {
        val currentId = uiState.showVideoDetailsDialog?.videoId ?: return@LaunchedEffect
        val updated = displayedBrowseVideos.firstOrNull { it.videoId == currentId } ?: return@LaunchedEffect
        homeViewModel.updateUiState { it.copy(showVideoDetailsDialog = updated) }
    }

    LaunchedEffect(uiState.showVideoDetailsDialog?.videoId) {
        if (uiState.showVideoDetailsDialog != null) onStopFloatingVideo()
    }

    fun refreshDetailsStats(videoId: String) {
        homeViewModel.requestVideoStats(videoId)
    }

    // Clips
    val clips by ClipsRepository.clipsFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val clipToPlay = homeState.clipToPlay

    val currentContext by rememberUpdatedState(context)
    val shareVideo = remember {
        { videoId: String -> HomeScreenHelpers.shareVideo(currentContext, videoId) }
    }
    val shareClip = remember {
        { clip: com.youtube.rating.android.data.models.VideoClip ->
            HomeScreenHelpers.shareClip(currentContext, clip)
        }
    }

    val toggleFavorite = callbacks.toggleFavorite
    val onVideoClick = callbacks.onVideoClick
    val onReportClick = callbacks.onReportClick
    val onQuickRateClick = callbacks.onQuickRateClick
    val onLongPressVideo = callbacks.onLongPressVideo
    val onResetFiltersCallback = callbacks.onResetFilters
    val onRetryCallback = callbacks.onRetry

    // Initial video id
    LaunchedEffect(initialVideoId) {
        if (!initialVideoId.isNullOrBlank()) {
            ratingViewModel.loadVideoInfo(initialVideoId)
            draft = draft.copy(url = "https://www.youtube.com/watch?v=$initialVideoId")
        }
    }

    var openedInitialVideo by rememberSaveable(initialVideoId) { mutableStateOf(false) }
    LaunchedEffect(initialVideoId, videoInfo) {
        val info = videoInfo
        if (!initialVideoId.isNullOrBlank() && !openedInitialVideo && info?.videoId == initialVideoId) {
            val video = VideoSearchResult(
                videoId = info.videoId,
                title = info.title,
                channelName = info.channelName,
                thumbnail = info.thumbnail,
                category = null,
                language = info.language
            )
            homeViewModel.updateUiState { it.copy(showVideoDetailsDialog = video) }
            if (initialStartSeconds > 0) {
                homeViewModel.setPlaybackSeconds(initialStartSeconds.toFloat())
            }
            openedInitialVideo = true
        }
    }

    // URL -> detect source + load info (youtube only)
    var lastHandledUrl by rememberSaveable { mutableStateOf(draft.url.trim()) }
    LaunchedEffect(Unit) {
        snapshotFlow { draft.url }
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(250)
            .collectLatest { rawUrl ->
                if (rawUrl.isBlank()) {
                    draft = draft.copy(source = VideoSource.Unknown())
                    lastHandledUrl = ""
                    return@collectLatest
                }

                val src = detectVideoSource(rawUrl)

                if (rawUrl == lastHandledUrl) {
                    draft = draft.copy(url = rawUrl, source = src)
                    if (src is VideoSource.YouTube) ratingViewModel.loadVideoInfo(src.videoId)
                    return@collectLatest
                }

                lastHandledUrl = rawUrl

                draft = draft.copy(
                    url = rawUrl,
                    source = src,
                    love = 0,
                    faith = 0,
                    hope = 0,
                    category = null,
                    manualLanguage = ""
                )

                if (src is VideoSource.YouTube) ratingViewModel.loadVideoInfo(src.videoId)
            }
    }

    // Submit haptics + optimistic updates
    LaunchedEffect(submitState) {
        when (submitState) {
            is RatingViewModel.SubmitState.Success -> {
                haptic.success()

                fun applyRatingUpdate(video: VideoSearchResult): VideoSearchResult {
                    val currentTotal = video.totalRatings.coerceAtLeast(0)
                    val newTotal = currentTotal + 1
                    val newAvgLove = (video.avgLove * currentTotal + draft.love) / newTotal
                    val newAvgFaith = (video.avgFaith * currentTotal + draft.faith) / newTotal
                    val newAvgHope = (video.avgHope * currentTotal + draft.hope) / newTotal
                    val newAvgTotal = (newAvgLove + newAvgFaith + newAvgHope) / 3.0
                    return video.copy(
                        totalRatings = newTotal,
                        avgLove = newAvgLove,
                        avgFaith = newAvgFaith,
                        avgHope = newAvgHope,
                        avgTotal = newAvgTotal
                    )
                }

                val ratedVideoId = ratingViewModel.currentVideoInfo.value?.videoId
                if (!ratedVideoId.isNullOrBlank()) {
                    uiState.showVideoDetailsDialog?.let { current ->
                        if (current.videoId == ratedVideoId) {
                            homeViewModel.updateUiState { it.copy(showVideoDetailsDialog = applyRatingUpdate(current)) }
                        }
                    }
                    homeViewModel.updateBrowseVideo(ratedVideoId) { existing -> applyRatingUpdate(existing) }
                }

                ratingViewModel.currentVideoInfo.value?.let { info ->
                    val newVideo = VideoSearchResult(
                        videoId = info.videoId,
                        title = info.title,
                        channelName = info.channelName,
                        thumbnail = info.thumbnail,
                        createdAt = System.currentTimeMillis(),
                        totalRatings = 1,
                        avgLove = draft.love.toDouble(),
                        avgFaith = draft.faith.toDouble(),
                        avgHope = draft.hope.toDouble(),
                        avgTotal = ((draft.love + draft.faith + draft.hope) / 3.0),
                        category = null,
                        language = draft.manualLanguage.takeIf { it.isNotBlank() }
                            ?: info.language.ifBlank { "unknown" }
                    )
                    homeViewModel.upsertBrowseVideo(newVideo)
                }

                refreshBrowse(resetPage = true, forceRefresh = true)
                uiState.showVideoDetailsDialog?.videoId?.let { refreshDetailsStats(it) }
                delay(DELAY_SUCCESS_MESSAGE_MS)
                ratingViewModel.resetSubmitState()
            }

            is RatingViewModel.SubmitState.Error -> haptic.error()
            else -> Unit
        }
    }

    // -------------------------------------------------------------------------
    // Dialogs (iste kao kod tebe)
    // -------------------------------------------------------------------------
    uiState.showVideoDetailsDialog?.let { video ->
        VideoDetailsDialog(
            video = video,
            onQuickRate = { onQuickRateClick(video) },
            submitState = submitState,
            isFavorite = isFavorite(video.videoId),
            onToggleFavorite = { toggleFavorite(video) },
            onShare = { shareVideo(video.videoId) },
            onCloseNoMini = { homeViewModel.updateUiState { it.copy(showVideoDetailsDialog = null) } },
            onDismiss = { handleVideoClose(video) },
            isInPipMode = isInPipMode,
            onFullScreenVideoActiveChange = onFullScreenVideoActiveChange,
            onPlaybackSecond = { homeViewModel.setPlaybackSeconds(it) },
            initialStartSeconds = if (video.videoId == initialVideoId) initialStartSeconds else 0
        )
    }

    uiState.showReportDialog?.let { video ->
        ReportVideoDialog(
            video = video,
            isReporting = uiState.reportState.loading,
            error = uiState.reportState.error,
            success = uiState.reportState.success,
            onDismiss = {
                if (!uiState.reportState.loading) {
                    homeViewModel.updateUiState {
                        it.copy(
                            showReportDialog = null,
                            reportState = it.reportState.reset()
                        )
                    }
                }
            },
            onConfirm = {
                homeViewModel.submitReport(video.videoId)
            }
        )
    }

    uiState.showDeleteDialog?.let { video ->
        HomeDeleteVideoDialog(
            video = video,
            loading = uiState.deleteState.loading,
            error = uiState.deleteState.error,
            success = uiState.deleteState.success,
            onDismiss = {
                homeViewModel.updateUiState {
                    it.copy(
                        showDeleteDialog = null,
                        deleteState = it.deleteState.reset()
                    )
                }
            },
            onConfirmDelete = {
                homeViewModel.updateUiState { it.copy(deleteState = it.deleteState.start()) }
                scope.launch {
                    try {
                        val response = ratingViewModel.deleteVideo(video.videoId)
                        if (response.success) {
                            homeViewModel.updateUiState { it.copy(deleteState = it.deleteState.ok(Strings.deleteSuccess)) }
                            refreshBrowse(resetPage = true)
                            homeViewModel.loadFeaturedVideos(languageCodes)
                            delay(1500)
                            homeViewModel.updateUiState {
                                it.copy(showDeleteDialog = null, deleteState = it.deleteState.reset())
                            }
                        } else {
                            homeViewModel.updateUiState {
                                it.copy(deleteState = it.deleteState.fail(response.message ?: Strings.deleteError))
                            }
                        }
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        homeViewModel.updateUiState {
                            it.copy(deleteState = it.deleteState.fail("${Strings.error}: ${e.message}"))
                        }
                    }
                }
            }
        )
    }

    uiState.quickRateVideo?.let { video ->
        QuickRateSheet(
            video = video,
            draft = uiState.quickRateDraft,
            submitState = submitState,
            sheetState = quickRateSheetState,
            onDraftChange = { newDraft ->
                homeViewModel.updateUiState { it.copy(quickRateDraft = newDraft) }
            },
            onDismiss = { homeViewModel.updateUiState { it.copy(quickRateVideo = null) } },
            onSubmit = { draftToSend ->
                haptic.strongFeedback()
                ratingViewModel.submitRating(
                    love = draftToSend.love,
                    faith = draftToSend.faith,
                    hope = draftToSend.hope,
                    category = null,
                    language = video.language.takeIf { it.isNotBlank() },
                    videoIdOverride = video.videoId,
                    titleOverride = video.title,
                    thumbnailOverride = video.thumbnail,
                    channelOverride = video.channelName
                )
                homeViewModel.updateUiState { it.copy(quickRateVideo = null) }
            }
        )
    }

    uiState.quickActionsVideo?.let { video ->
        QuickActionsSheet(
            video = video,
            sheetState = quickActionsSheetState,
            isFavorite = isFavorite(video.videoId),
            onFavoriteToggle = { toggleFavorite(video) },
            onShare = { shareVideo(video.videoId) },
            onReport = { homeViewModel.updateUiState { it.copy(showReportDialog = video) } },
            onDismiss = { homeViewModel.updateUiState { it.copy(quickActionsVideo = null) } }
        )
    }

    if (showCloseChoiceDialog && pendingCloseVideo != null) {
        HomeCloseChoiceDialog(
            isAdminMode = isAdminMode,
            onCancel = { homeViewModel.clearCloseChoice() },
            onSelect = { action ->
                val video = pendingCloseVideo ?: return@HomeCloseChoiceDialog
                applyVideoCloseAction(action, video, pendingCloseSeconds)
                homeViewModel.clearCloseChoice()
            }
        )
    }

    saintPayload?.let { saint ->
        HomeSaintDialogs(
            saint = saint,
            showSaintDialog = showSaintDialog,
            showSaintFullDialog = showSaintFullDialog,
            onDismissSaint = { homeViewModel.dismissSaintDialog() },
            onImageClick = { homeViewModel.openSaintFullDialog() },
            onPrev = saint.prev?.date?.let { date -> { homeViewModel.loadSaintByDate(date) } },
            onNext = saint.next?.date?.let { date -> { homeViewModel.loadSaintByDate(date) } },
            onDismissFull = { homeViewModel.closeSaintFullDialog() }
        )
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            if (uiState.showBrowse) {
                val homeTabs = listOf(
                    HomeTab.Browse to Strings.browse,
                    HomeTab.Clips to Strings.clips,
                )
                SecondaryTabRow(
                    selectedTabIndex = homeTabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Tab(
                        selected = selectedTab == HomeTab.Browse,
                        onClick = { selectTab(HomeTab.Browse) },
                        text = { Text(Strings.browse) },
                    )
                    Tab(
                        selected = selectedTab == HomeTab.Clips,
                        onClick = { selectTab(HomeTab.Clips) },
                        text = { Text(Strings.clips) },
                    )
                }

                val lockFeaturedHome by HomePrefs.lockFeaturedHomeFlow(context)
                    .collectAsStateWithLifecycle(initialValue = true)

                if (selectedTab == HomeTab.Browse && lockFeaturedHome && popularVideos.isNotEmpty()) {
                    HomeBrowseHeaderCard(
                        featured = popularVideos,
                        popularRange = popularRange,
                        onPopularRangeChange = { dispatchHomeIntent(HomeContract.Intent.SelectPopularRange(it)) },
                        showPopularTimeDropdown = popularTimeVideosEnabled,
                        onVideoClick = onVideoClick
                    )
                }

                when (selectedTab) {
                    HomeTab.Clips -> {
                        ClipsSection(
                            clips = clips,
                            onPlay = { clip -> homeViewModel.setClipToPlay(clip) },
                            onDelete = { clip ->
                                scope.launch { ClipsRepository.deleteClip(context, clip.id) }
                            },
                            onReorder = { newOrder ->
                                scope.launch { ClipsRepository.reorderClips(context, newOrder) }
                            },
                            onShare = { clip -> shareClip(clip) }
                        )
                    }

                    else -> {
                        PullToRefreshBox(
                            isRefreshing = isBrowsing && displayedBrowseVideos.isEmpty(),
                            onRefresh = {
                                com.youtube.rating.android.sentry.SentryLogger.metricCount("home_refresh", 1.0)
                                refreshBrowse(resetPage = true, forceRefresh = true, preserveOrder = true)
                                homeViewModel.loadFeaturedVideos(languageCodes)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            BrowseSection(
                                headerContent = if (!lockFeaturedHome) {
                                    {
                                        if (popularVideos.isNotEmpty()) {
                                            HomeBrowseHeaderCard(
                                                featured = popularVideos,
                                                popularRange = popularRange,
                                                onPopularRangeChange = { dispatchHomeIntent(HomeContract.Intent.SelectPopularRange(it)) },
                                                showPopularTimeDropdown = popularTimeVideosEnabled,
                                                onVideoClick = onVideoClick
                                            )
                                        }
                                    }
                                } else null,
                                isOnline = isOnline,
                                searchQuery = searchQuery,
                                browseItems = browsePagingItems,
                                orderedBrowseVideos = if (homeState.isBrowseOrderManual) homeState.browseVideos else emptyList(),
                                isBrowsing = isBrowsing,
                                browseError = browseError,
                                randomLoading = randomLoading.value,
                                randomProgress = randomProgress.value,
                                isGridView = uiState.isGridView,
                                isAdminMode = isAdminMode,
                                onResetFilters = onResetFiltersCallback,
                                onClearSearch = {
                                    dispatchHomeIntent(HomeContract.Intent.ClearSearch)
                                    refreshBrowse(resetPage = true)
                                },
                                onRetry = onRetryCallback,
                                onVideoClick = onVideoClick,
                                onFavoriteClick = toggleFavorite,
                                onQuickRateClick = onQuickRateClick,
                                onLongPress = onLongPressVideo,
                                onReportClick = onReportClick,
                                isFavorite = isFavorite,
                                isLoadingMore = isLoadingMore,
                                hasMore = hasMore,
                                onAddVideo = { openUrlDialog("") },
                                gridState = gridScrollState,
                                listState = listScrollState,
                                disableScrollEffects = disableScrollEffects
                            )
                        }
                    }
                }
            } else {
                when (val src = draft.source) {
                    is VideoSource.YouTube -> {
                        val info = videoInfo
                        if (info != null) {
                            RatingFormScreen(
                                infoTitle = info.title,
                                infoChannel = info.channelName,
                                infoLanguage = info.language,
                                infoThumbnail = info.thumbnail,
                                draft = draft,
                                onDraftChange = { draft = it },
                                submitState = submitState,
                                onSubmit = {
                                    haptic.strongFeedback()
                                    ratingViewModel.submitRating(
                                        love = draft.love,
                                        faith = draft.faith,
                                        hope = draft.hope,
                                        category = null,
                                        language = draft.manualLanguage.takeIf { it.isNotBlank() }
                                    )
                                },
                                urlForFallbackThumb = draft.url,
                                onHapticLight = { haptic.lightTap() }
                            )
                        } else {
                            LoadingCard(text = Strings.loadingYoutube, url = draft.url)
                        }
                    }

                    else -> {
                        if (draft.url.isNotBlank()) {
                            ErrorCard(
                                title = Strings.unsupportedLink,
                                message = Strings.onlyYoutubeSupported,
                                onRetry = { },
                                onManualEntry = {
                                    homeViewModel.updateUiState { it.copy(showUrlInputDialog = true) }
                                }
                            )
                        }
                    }
                }
            }
        }

        clipToPlay?.let { clip ->
            ClipPlayerDialog(
                clip = clip,
                onDismiss = { homeViewModel.setClipToPlay(null) },
                onMiniplayer = {
                    val video = VideoSearchResult(
                        videoId = clip.videoId,
                        title = clip.title,
                        channelName = clip.channelName,
                        thumbnail = clip.thumbnail,
                        category = null,
                        language = ""
                    )
                    onStartFloatingVideo(video, clip.startSeconds, VideoCloseAction.MINI_PLAYER)
                    homeViewModel.setClipToPlay(null)
                }
            )
        }

        if (uiState.showBrowse && uiState.showBrowseControlsSheet && !uiState.isFullScreenVideoOpen) {
            BrowseControlsSheet(
                onSearchClick = { refreshBrowse(resetPage = true) },
                onOpenClips = { selectTab(HomeTab.Clips) },
                isBrowsing = isBrowsing,
                browseVideosCount = displayedBrowseVideos.size,
                totalResults = totalResults,
                isGridView = uiState.isGridView,
                sortBy = sortBy,
                onToggleView = { newValue ->
                    homeViewModel.updateUiState { it.copy(isGridView = newValue) }
                    ViewPreferencesManager.setGridView(newValue, context)
                },
                onSortChange = { newSort ->
                    dispatchHomeIntent(HomeContract.Intent.SortSelected(newSort))
                },
                ratingFilters = browseRatingFilters,
                onRatingFiltersChange = { newFilters ->
                    dispatchHomeIntent(HomeContract.Intent.SetBrowseRatingFilters(newFilters))
                },
                onResetAll = {
                    haptic.click()
                    homeViewModel.resetBrowseRatingFilters()
                    homeViewModel.resetFilters()
                    refreshBrowse(resetPage = true)
                },
                onDismiss = { homeViewModel.updateUiState { it.copy(showBrowseControlsSheet = false) } }
            )
        }

        if (uiState.showBrowse && uiState.showHomeControlsSheet && !uiState.isFullScreenVideoOpen) {
            val homeControlsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = {
                    homeViewModel.updateUiState { it.copy(showHomeControlsSheet = false, showSearchField = false) }
                },
                sheetState = homeControlsSheetState
            ) {
                HomeTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        dispatchHomeIntent(HomeContract.Intent.SearchQueryChanged(it))
                    },
                    onSearchClear = { dispatchHomeIntent(HomeContract.Intent.ClearSearch) },
                    onSearchSubmit = {
                        com.youtube.rating.android.sentry.SentryLogger.metricCount("home_search_submit", 1.0)
                        refreshBrowse(resetPage = true)
                        homeViewModel.updateUiState { it.copy(showHomeControlsSheet = false, showSearchField = false) }
                    },
                    searchSuggestions = searchSuggestions,
                    quickSearchTerms = quickSearchTerms,
                    totalResults = totalResults,
                    onOpenFilters = {
                        com.youtube.rating.android.sentry.SentryLogger.metricCount("home_filters_open", 1.0)
                        homeViewModel.updateUiState { it.copy(showHomeControlsSheet = false, showBrowseControlsSheet = true) }
                    },
                    showSearchField = uiState.showSearchField,
                    onToggleSearchField = {
                        homeViewModel.updateUiState { it.copy(showSearchField = !it.showSearchField) }
                    },
                    activeFilterCount = activeFilterCount,
                    onClearAllFilters = {
                        haptic.click()
                        homeViewModel.resetBrowseRatingFilters()
                        homeViewModel.resetFilters()
                        refreshBrowse(resetPage = true)
                    },
                    selectedLanguages = selectedContentLanguages,
                    onToggleLanguage = { dispatchHomeIntent(HomeContract.Intent.ToggleLanguage(it)) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        if (uiState.showBrowse && selectedTab == HomeTab.Browse && !uiState.isFullScreenVideoOpen) {
            BrowseFloatingButtons(
                showScrollToTop = showScrollToTop,
                onScrollToTop = {
                    scope.launch {
                        scrollToTopSafe(uiState.isGridView, gridScrollState, listScrollState)
                    }
                },
                onOpenControls = {
                    homeViewModel.updateUiState { it.copy(showHomeControlsSheet = true, showSearchField = false) }
                },
                onAddVideo = { openUrlDialog("") },
                onRandomVideo = onRandomVideo,
                randomEnabled = !isRandomLocked,
                showRandom = !hasMore,
                showClipsFab = clips.isNotEmpty(),
                onOpenClips = { selectTab(HomeTab.Clips) }
            )
        }

        if ((submitState is RatingViewModel.SubmitState.Success || submitState is RatingViewModel.SubmitState.Error) &&
            uiState.showVideoDetailsDialog == null
        ) {
            SubmitBanner(submitState = submitState)
        }

        if (uiState.showUrlInputDialog) {
            UrlInputDialog(
                initialUrl = urlDialogPrefill,
                onDismiss = {
                    homeViewModel.clearUrlDialogPrefill()
                    homeViewModel.updateUiState { it.copy(showUrlInputDialog = false) }
                },
                onConfirm = { url -> startRatingFromUrl(url) }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// HomeTopBar (✅ poboljšana)
// -----------------------------------------------------------------------------
@Composable
private fun HomeTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onSearchSubmit: () -> Unit,
    searchSuggestions: List<String>,
    quickSearchTerms: List<String>,
    totalResults: Int,
    onOpenFilters: () -> Unit,
    showSearchField: Boolean,
    onToggleSearchField: () -> Unit,
    activeFilterCount: Int,
    onClearAllFilters: () -> Unit,
    selectedLanguages: Set<Strings.Language>,
    onToggleLanguage: (Strings.Language) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val onSubmitLatest by rememberUpdatedState(onSearchSubmit)
    val onClearLatest by rememberUpdatedState(onSearchClear)
    val onQueryChangeLatest by rememberUpdatedState(onSearchQueryChange)

    LaunchedEffect(showSearchField) {
        if (showSearchField) {
            delay(30)
            focusRequester.requestFocus()
        } else {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    val normalizedQuery = remember(searchQuery) { searchQuery.trim() }

    val mergedChips by remember(normalizedQuery, quickSearchTerms, searchSuggestions) {
        derivedStateOf {
            val base = (quickSearchTerms + searchSuggestions)
                .asSequence()
                .map { it.normalizeSuggestion().trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()

            if (normalizedQuery.isBlank()) base.take(12)
            else base.asSequence()
                .filter { it.contains(normalizedQuery, ignoreCase = true) }
                .take(12)
                .toList()
        }
    }

    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .padding(top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (totalResults > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
                    ) {
                        Text(
                            text = "${Strings.found}: $totalResults",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }

                val languages = remember {
                    listOf(
                        Strings.Language.CROATIAN to "HR",
                        Strings.Language.ENGLISH to "EN",
                        Strings.Language.GERMAN to "DE"
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(end = 8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(
                        languages,
                        key = { it.first.name },
                        contentType = { "lang_chip" }
                    ) { (lang, code) ->
                        FilterChip(
                            selected = selectedLanguages.contains(lang),
                            onClick = { onToggleLanguage(lang) },
                            label = { Text(code) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!showSearchField && normalizedQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onClearLatest()
                                onSubmitLatest()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = Strings.clearSearch,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onToggleSearchField, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = Strings.search,
                            tint = if (showSearchField) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onOpenFilters, modifier = Modifier.size(36.dp)) {
                        BadgedBox(
                            badge = {
                                if (activeFilterCount > 0) Badge { Text(activeFilterCount.toString(), fontSize = 9.sp) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = Strings.filters,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activeFilterCount > 0) {
                        IconButton(onClick = onClearAllFilters, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = Strings.reset,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showSearchField, enter = fadeIn(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { onQueryChangeLatest(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text(Strings.searchVideo, style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        onClearLatest()
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = Strings.clearSearch)
                                    }
                                }
                                IconButton(onClick = {
                                    keyboard?.hide()
                                    focusManager.clearFocus()
                                    onSubmitLatest()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = Strings.search)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboard?.hide()
                                focusManager.clearFocus()
                                onSubmitLatest()
                            }
                        )
                    )

                    if (mergedChips.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(
                                mergedChips,
                                key = { it },
                                contentType = { "search_chip" }
                            ) { chip ->
                                val selected = chip.equals(normalizedQuery, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onQueryChangeLatest(chip)
                                        keyboard?.hide()
                                        focusManager.clearFocus()
                                        onSubmitLatest()
                                    },
                                    label = {
                                        Text(
                                            chip,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.normalizeSuggestion(): String =
    trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private suspend fun scrollToTopSafe(
    isGridView: Boolean,
    gridScrollState: LazyGridState,
    listScrollState: LazyListState
) {
    if (isGridView) {
        if (gridScrollState.firstVisibleItemIndex == 0 && gridScrollState.firstVisibleItemScrollOffset == 0) return
        val animated = runCatching { gridScrollState.animateScrollToItem(0) }.isSuccess
        if (!animated ||
            gridScrollState.firstVisibleItemIndex != 0 ||
            gridScrollState.firstVisibleItemScrollOffset != 0
        ) {
            gridScrollState.scrollToItem(0)
        }
    } else {
        if (listScrollState.firstVisibleItemIndex == 0 && listScrollState.firstVisibleItemScrollOffset == 0) return
        val animated = runCatching { listScrollState.animateScrollToItem(0) }.isSuccess
        if (!animated ||
            listScrollState.firstVisibleItemIndex != 0 ||
            listScrollState.firstVisibleItemScrollOffset != 0
        ) {
            listScrollState.scrollToItem(0)
        }
    }
}
