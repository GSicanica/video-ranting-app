package com.youtube.rating.android.viewmodel

import com.youtube.rating.core.coroutines.ioDispatcher

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.utils.AnalyticsManager
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.RatingRequest
import com.youtube.rating.shared.models.YouTubeVideoInfo
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.android.utils.DailyActionLimiter
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.WatchHistoryManager
import com.youtube.rating.android.storage.WatchHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.youtube.rating.android.data.prefs.RatingsPrefs
import com.youtube.rating.android.data.prefs.VideoPrefs
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * ViewModel for rating videos.
 * Uses Application context to prevent memory leaks.
 * ViewModels should never hold references to Activity or Fragment contexts.
 */
class RatingViewModel(
    private val application: Application,
    private val apiClient: RatingApiClient,
    private val youTubeInfoService: YouTubeInfoService,
    private val userTokenManager: UserTokenManager,
    private val analyticsManager: AnalyticsManager,
    private val watchHistoryRepository: com.youtube.rating.android.data.WatchHistoryRepository
) : ViewModel() {

    private val _currentVideoInfo = MutableStateFlow<YouTubeVideoInfo?>(null)
    val currentVideoInfo: StateFlow<YouTubeVideoInfo?> = _currentVideoInfo.asStateFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private var loadJob: Job? = null
    private var submitJob: Job? = null

    sealed class LoadingState {
        data object Idle : LoadingState()
        data object Loading : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    sealed class SubmitState {
        data object Idle : SubmitState()
        data object Loading : SubmitState()
        data object Success : SubmitState()
        data class Error(val message: String) : SubmitState()
    }

    private fun log(message: String) = LogConfig.log(message)
    private fun logError(message: String, throwable: Throwable? = null) =
        LogConfig.logError(message, throwable)

    fun resetSubmitState() {
        _submitState.value = SubmitState.Idle
    }

    fun clearVideo() {
        loadJob?.cancel()
        submitJob?.cancel()
        _currentVideoInfo.value = null
        _loadingState.value = LoadingState.Idle
        _submitState.value = SubmitState.Idle
    }

    fun loadVideoInfo(videoId: String) {
        if (videoId.isBlank()) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _loadingState.value = LoadingState.Loading
            _submitState.value = SubmitState.Idle

            try {
                log(message = "🔍 Loading video info for: $videoId")
                val result = youTubeInfoService.getVideoInfo(videoId)
                result.onSuccess { info ->
                    // Validate video info has required fields
                    if (info.title.isNullOrBlank()) {
                        log(message = "⚠️ Video info missing title")
                        _loadingState.value = LoadingState.Error(Strings.videoInfoIncomplete)
                        return@onSuccess
                    }

                    log(message = "✅ Video info loaded: ${info.title}")

                    // Safely handle nullable fields with explicit null checks
                    val safeTitle = info.title ?: "Unknown Title"
                    val safeChannel = info.channelName ?: "Unknown Channel"
                    val normalizedLang = normalizeLanguage(apiLang = info.language, title = safeTitle, channel = safeChannel)

                    _currentVideoInfo.value = info.copy(language = normalizedLang)
                    _loadingState.value = LoadingState.Idle

                    // Track video view in watch history (if enabled)
                    trackVideoView(videoId = videoId, info = info)
                }.onFailure { error ->
                    val message = when (error) {
                        is UnknownHostException -> Strings.noInternetConnection
                        is SocketTimeoutException -> Strings.timeoutTryAgain
                        else -> Strings.errorLoadingVideo(error.message)
                    }
                    _loadingState.value = LoadingState.Error(message)
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                log(message = "❌ Error loading video: ${e.message}")
                logError(message = "Error details:", throwable = e)
                _loadingState.value =
                    LoadingState.Error(Strings.errorLoadingVideo(e.message))
            }
        }
    }

    /**
     * Normalize language code with explicit null safety
     * @param apiLang Language from API (nullable)
     * @param title Video title (non-null, validated earlier)
     * @param channel Channel name (non-null, validated earlier)
     * @return Normalized language code, never null
     */
    private fun normalizeLanguage(apiLang: String?, title: String, channel: String): String {
        // Use API language if valid
        val lang = apiLang?.trim().orEmpty()
        if (lang.isNotBlank() && !lang.equals("unknown", ignoreCase = true)) return lang

        // Fallback to local detection
        val guess = detectLanguageLocal(title = title, channel = channel)
        return guess.ifBlank { "unknown" }
    }

    private fun detectLanguageLocal(title: String, channel: String): String {
        val text = (title + " " + channel).lowercase(Locale.getDefault())
        val hasHrChars = text.any { it in "čćžšđ" }
        val hasDeChars = text.any { it in "äöüß" }

        val hrWords = listOf(
            "bog",
            "isus",
            "krist",
            "molitva",
            "vjera",
            "ljubav",
            "nada",
            "svjedočanstvo",
            "propovijed",
            "hrvatski"
        )
        val deWords = listOf(
            "gott",
            "jesus",
            "glaube",
            "liebe",
            "hoffnung",
            "kirche",
            "gemeinde",
            "predigt",
            "deutsch"
        )

        val hrScore = (if (hasHrChars) 3 else 0) + hrWords.count { text.contains(it) }
        val deScore = (if (hasDeChars) 3 else 0) + deWords.count { text.contains(it) }

        return when {
            hrScore >= deScore && hrScore >= 2 -> "hr"
            deScore > hrScore && deScore >= 2 -> "de"
            text.contains("jesus") || text.contains("god") || text.contains("faith") || text.contains(
                "bible"
            ) -> "en"

            else -> ""
        }
    }

    /**
     * Radi i za YouTube i za FB (overrides).
     */
    fun submitRating(
        love: Int,
        faith: Int,
        hope: Int,
        category: String? = null,
        language: String? = null,
        videoIdOverride: String? = null,
        titleOverride: String? = null,
        thumbnailOverride: String? = null,
        channelOverride: String? = null
    ) {
        // basic validation
        if (love !in 1..3 || faith !in 1..3 || hope !in 1..3) {
            _submitState.value = SubmitState.Error(Strings.ratingsMustBe1To3)
            return
        }

        val yt = _currentVideoInfo.value

        val finalVideoId = videoIdOverride ?: yt?.videoId
        val finalTitle = titleOverride ?: yt?.title
        val finalThumb = thumbnailOverride ?: yt?.thumbnail
        val finalChannel = channelOverride ?: yt?.channelName
        val finalLanguage = (language ?: yt?.language ?: "unknown").trim().ifBlank { "unknown" }

        if (finalVideoId.isNullOrBlank() || finalTitle.isNullOrBlank()) {
            _submitState.value = SubmitState.Error(Strings.missingVideoData)
            return
        }

        val limiter = DailyActionLimiter(context = application)
        val limitKey = "rate_${finalVideoId}"

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            if (!limiter.canPerform(limitKey)) {
                _submitState.value = SubmitState.Error(Strings.canRateOncePerVideo)
                return@launch
            }

            _submitState.value = SubmitState.Loading
            try {
                val response = withContext(ioDispatcher) {
                    log(message = "🔄 Starting rating submission...")
                    val userToken = userTokenManager.getUserTokenAsync()
                    if (userToken == null) {
                        val e = IllegalStateException(Strings.userTokenNotSet)
                        com.youtube.rating.android.sentry.SentryLogger.captureException(
                            e,
                            tags = mapOf(
                                "where" to "RatingViewModel.submitRating",
                                "video_id" to (finalVideoId ?: "")
                            )
                        )
                        return@withContext com.youtube.rating.shared.models.RatingResponse(
                            success = false,
                            message = e.message ?: "User token missing"
                        )
                    }
                    log(message = "📱 User Token: ${userToken.take(8)}…")

                    val request = RatingRequest(
                        videoId = finalVideoId,
                        videoTitle = finalTitle,
                        videoThumbnail = finalThumb.orEmpty(),
                        videoChannel = finalChannel.orEmpty(),
                        love = love,
                        faith = faith,
                        hope = hope,
                        category = category,
                        userToken = userToken,
                        language = finalLanguage
                    )

                    log("📤 Sending request: videoId=$finalVideoId love=$love faith=$faith hope=$hope")
                    apiClient.submitRating(request)
                }

                if (response.success) {
                    limiter.markPerformed(limitKey)
                    youTubeInfoService.invalidate(finalVideoId ?: "")

                    // Persist only "my rating" locally (no comments/ratings caching).
                    runCatching {
                        val ratedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date())
                        RatingsPrefs.upsertMyRating(
                            context = application,
                            videoId = finalVideoId ?: "",
                            videoTitle = finalTitle ?: "",
                            channelName = finalChannel.orEmpty(),
                            thumbnail = finalThumb.orEmpty(),
                            category = category,
                            myLove = love,
                            myFaith = faith,
                            myHope = hope,
                            ratedAt = ratedAt
                        )
                    }
                    
                    // Track video rating analytics
                    runCatching {
                        analyticsManager.trackVideoRating(
                            videoId = finalVideoId ?: "",
                            videoTitle = finalTitle ?: "",
                            videoChannel = finalChannel ?: "",
                            love = love,
                            faith = faith,
                            hope = hope,
                            category = (category ?: "").toString(),
                            language = finalLanguage
                        )
                    }
                }
                _submitState.value =
                    if (response.success) SubmitState.Success
                    else SubmitState.Error(response.message.ifBlank { Strings.errorSendingRating })

            } catch (e: UnknownHostException) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _submitState.value = SubmitState.Error(Strings.noInternetConnection)
            } catch (e: SocketTimeoutException) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _submitState.value = SubmitState.Error(Strings.timeoutTryAgain)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                logError(message = "❌ Exception during rating submission: ${e.message}", throwable = e)
                _submitState.value = SubmitState.Error(Strings.genericError(e.message))
            }
        }
    }

    suspend fun deleteVideo(videoId: String): ApiResponse {
        return withContext(ioDispatcher) { apiClient.deleteVideo(videoId) }
    }

    /**
     * Admin funkcije - token mora biti proslijeđen iz sigurnog izvora
     * (npr. EncryptedSharedPreferences ili nakon backend autentifikacije).
     * ✅ FIXED: Uklonjen hardcoded token, token se prima kao parametar.
     */
    suspend fun adminDeleteVideo(videoId: String, adminToken: String): ApiResponse {
        return withContext(ioDispatcher) { apiClient.adminDeleteVideo(videoId, adminToken) }
    }

    suspend fun adminToggleVideo(videoId: String, isDisabled: Boolean, adminToken: String): ApiResponse {
        return withContext(ioDispatcher) {
            apiClient.adminToggleVideo(
                videoId,
                isDisabled,
                adminToken
            )
        }
    }

    /**
     * Track video view in watch history
     * Records that the user viewed this video (if history tracking is enabled)
     */
    private fun trackVideoView(videoId: String, info: YouTubeVideoInfo) {
        viewModelScope.launch {
            try {
                log(message = "🎬 trackVideoView called for videoId: $videoId")

                // Check if watch history is enabled in preferences
                val historyEnabled = withContext(ioDispatcher) {
                    VideoPrefs.getWatchHistoryEnabled(application)
                }
                log(message = "📝 Watch history enabled: $historyEnabled")

                if (!historyEnabled) {
                    log(message = "⚠️ Watch history tracking is disabled in preferences")
                    return@launch
                }

                // Get WatchHistoryManager instance
                val watchHistoryManager = WatchHistoryManager.getInstance(application)
                log(message = "📦 WatchHistoryManager instance obtained")

                // Create history entry
                val entry = WatchHistoryEntry(
                    videoId = videoId,
                    title = info.title ?: "Unknown Title",
                    thumbnail = info.thumbnail ?: "",
                    channelName = info.channelName ?: "Unknown Channel",
                    category = null, // Category not available in YouTubeVideoInfo
                    viewedAt = System.currentTimeMillis()
                )
                log(message = "📋 Created history entry: ${entry.title}")

                // Add to local history
                log(message = "💾 Adding to local history...")
                val success = watchHistoryManager.addToHistory(entry)
                log(message = "💾 Add to history result: $success")

                if (success) {
                    log(message = "✅ Video added to watch history: $videoId")

                    // Sync with server in background using viewModelScope
                    makeIOCall {
                        try {
                            watchHistoryRepository.recordView(entry)
                            log(message = "✅ Video synced to server: $videoId")
                        } catch (e: Exception) {
                            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                            // Fail silently - local history is already saved
                            log(message = "⚠️ Failed to sync to server (offline?): ${e.message}")
                        }
                    }
                } else {
                    log(message = "⚠️ Failed to add video to watch history: $videoId")
                }

            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                // Log error but don't fail the main flow
                log(message = "❌ Error tracking video view: ${e.message}")
                logError(message = "Watch history error details:", throwable = e)
            }
        }
    }
}

/**
 * Factory for creating RatingViewModel.
 * Accepts Application context to prevent memory leaks.
 */
class RatingViewModelFactory(
    private val application: Application,
    private val apiClient: RatingApiClient,
    private val youTubeInfoService: YouTubeInfoService,
    private val userTokenManager: UserTokenManager,
    private val analyticsManager: AnalyticsManager,
    private val watchHistoryRepository: com.youtube.rating.android.data.WatchHistoryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RatingViewModel::class.java)) {
            return RatingViewModel(application = application, apiClient = apiClient, youTubeInfoService = youTubeInfoService, userTokenManager = userTokenManager, analyticsManager = analyticsManager, watchHistoryRepository = watchHistoryRepository) as T
        }
        val e = IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e,
            tags = mapOf("where" to "RatingViewModelFactory.create")
        )
        error(e.message ?: "Unknown ViewModel class")
    }
}
