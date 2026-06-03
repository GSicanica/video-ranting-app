package com.youtube.rating.android.ui.screens.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.youtube.rating.android.ui.models.HomeScreenUiState
import com.youtube.rating.android.ui.models.RatingDraft
import com.youtube.rating.android.util.VideoSource
import com.youtube.rating.android.util.detectVideoSource
import com.youtube.rating.android.utils.LocalBibleRepository
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.viewmodel.HomeViewModel
import com.youtube.rating.android.viewmodel.RatingViewModel
import com.youtube.rating.shared.models.VideoSearchResult
import com.youtube.rating.shared.models.YouTubeVideoInfo
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * Side effect composables for HomeScreen
 * Extracted LaunchedEffects for better organization and testability
 */

/**
 * Initial loading effect - loads bible books and user token
 */
@Composable
internal fun InitialLoadingEffect(
    userTokenManager: UserTokenManager,
    onUserTokenLoaded: (String?) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Load initial data in parallel on IO dispatcher
        makeIOCall {
            val bibleBooksDeferred = async {
                runCatching { LocalBibleRepository.getBooks(context) }
            }
            val userTokenDeferred = async {
                runCatching { userTokenManager.getUserTokenAsync() }
            }

            // Wait for all to complete
            bibleBooksDeferred.await()
            val userToken = userTokenDeferred.await().getOrNull()
            onUserTokenLoaded(userToken)
        }
    }
}

/**
 * Initial browse mode effect - shows browse when no video to rate
 */
@Composable
internal fun InitialBrowseModeEffect(
    draftUrl: String,
    initialVideoId: String?,
    homeViewModel: HomeViewModel
) {
    LaunchedEffect(draftUrl, initialVideoId) {
        if (draftUrl.isBlank() && initialVideoId.isNullOrBlank()) {
            homeViewModel.updateUiState { it.copy(showBrowse = true) }
        }
    }
}

/**
 * Initial video effect - handles initialVideoId parameter
 */
@Composable
internal fun InitialVideoEffect(
    initialVideoId: String?,
    ratingViewModel: RatingViewModel,
    onDraftUpdate: (String) -> Unit
) {
    LaunchedEffect(initialVideoId) {
        if (!initialVideoId.isNullOrBlank()) {
            ratingViewModel.loadVideoInfo(initialVideoId)
            onDraftUpdate("https://www.youtube.com/watch?v=$initialVideoId")
        }
    }
}

/**
 * Video details auto-open effect - opens details dialog for initial video
 */
@Composable
internal fun VideoDetailsAutoOpenEffect(
    initialVideoId: String?,
    videoInfo: YouTubeVideoInfo?,
    openedInitialVideo: Boolean,
    onOpenVideoDetails: (VideoSearchResult) -> Unit,
    onMarkAsOpened: () -> Unit
) {
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
            onOpenVideoDetails(video)
            onMarkAsOpened()
        }
    }
}

/**
 * URL detection effect - detects video source and loads info
 */
@OptIn(FlowPreview::class)
@Composable
internal fun UrlDetectionEffect(
    draftUrl: String,
    ratingViewModel: RatingViewModel,
    onDraftUpdate: (RatingDraft) -> Unit
) {
    LaunchedEffect(Unit) {
        snapshotFlow { draftUrl }
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(250)
            .collectLatest { rawUrl ->
                if (rawUrl.isBlank()) {
                    onDraftUpdate(RatingDraft(source = VideoSource.Unknown()))
                    return@collectLatest
                }

                val src = detectVideoSource(input = rawUrl)

                // Reset rating inputs on new link
                onDraftUpdate(
                    RatingDraft(
                        url = rawUrl,
                        source = src,
                        love = 0, faith = 0, hope = 0,
                        category = null,
                        manualLanguage = ""
                    )
                )

                when (src) {
                    is VideoSource.YouTube -> ratingViewModel.loadVideoInfo(src.videoId)
                    else -> Unit
                }
            }
    }
}

/**
 * Submit state effect - handles submit success/error with haptic feedback
 */
@Composable
internal fun SubmitStateEffect(
    submitState: RatingViewModel.SubmitState,
    videoInfo: YouTubeVideoInfo?,
    draft: RatingDraft,
    homeViewModel: HomeViewModel,
    ratingViewModel: RatingViewModel,
    onHapticSuccess: () -> Unit,
    onHapticError: () -> Unit,
    onRefreshBrowse: () -> Unit,
    onRefreshDetailsStats: (String) -> Unit,
    showVideoDetailsDialog: VideoSearchResult?
) {
    LaunchedEffect(submitState) {
        when (submitState) {
            is RatingViewModel.SubmitState.Success -> {
                onHapticSuccess()

                // Optimistically insert new video at top of list
                videoInfo?.let { info ->
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

                onRefreshBrowse()
                showVideoDetailsDialog?.videoId?.let { onRefreshDetailsStats(it) }

                delay(3000) // DELAY_SUCCESS_MESSAGE_MS
                ratingViewModel.resetSubmitState()
            }

            is RatingViewModel.SubmitState.Error -> onHapticError()
            else -> Unit
        }
    }
}

/**
 * Scroll position persistence effect for grid
 */
@Composable
@OptIn(FlowPreview::class)
internal fun GridScrollPersistenceEffect(
    gridScrollState: LazyGridState,
    shouldShowBrowse: Boolean,
    homeViewModel: HomeViewModel
) {
    LaunchedEffect(gridScrollState, shouldShowBrowse) {
        if (!shouldShowBrowse) return@LaunchedEffect

        snapshotFlow {
            Triple(
                gridScrollState.firstVisibleItemIndex,
                gridScrollState.firstVisibleItemScrollOffset,
                gridScrollState.isScrollInProgress
            )
        }
            .distinctUntilChanged()
            .debounce(200)
            .collectLatest { (index, offset, isScrolling) ->
                if (isScrolling) return@collectLatest

                homeViewModel.updateUiState {
                    it.copy(
                        gridFirstVisibleItemIndex = index,
                        gridFirstVisibleItemScrollOffset = offset
                    )
                }
            }
    }
}

/**
 * Scroll position persistence effect for list
 */
@Composable
@OptIn(FlowPreview::class)
internal fun ListScrollPersistenceEffect(
    listScrollState: LazyListState,
    shouldShowBrowse: Boolean,
    homeViewModel: HomeViewModel
) {
    LaunchedEffect(listScrollState, shouldShowBrowse) {
        if (!shouldShowBrowse) return@LaunchedEffect

        snapshotFlow {
            Triple(
                listScrollState.firstVisibleItemIndex,
                listScrollState.firstVisibleItemScrollOffset,
                listScrollState.isScrollInProgress
            )
        }
            .distinctUntilChanged()
            .debounce(200)
            .collectLatest { (index, offset, isScrolling) ->
                if (isScrolling) return@collectLatest

                homeViewModel.updateUiState {
                    it.copy(
                        listFirstVisibleItemIndex = index,
                        listFirstVisibleItemScrollOffset = offset
                    )
                }
            }
    }
}

/**
 * UI state consolidation effect - closes sheets when needed
 */
@Composable
internal fun UiStateConsolidationEffect(
    uiState: HomeScreenUiState,
    isHomeActive: Boolean,
    homeViewModel: HomeViewModel
) {
    LaunchedEffect(uiState.showBrowse, uiState.showVideoDetailsDialog, isHomeActive) {
        // Close sheet when leaving browse or opening video player
        if (!uiState.showBrowse) {
            homeViewModel.updateUiState { it.copy(showBrowseControlsSheet = false) }
        }

        // Close sheet when opening video details
        if (uiState.showVideoDetailsDialog != null) {
            homeViewModel.updateUiState { it.copy(showBrowseControlsSheet = false) }
        }

        // Close sheet when leaving home
        if (!isHomeActive) {
            homeViewModel.updateUiState { it.copy(showBrowseControlsSheet = false) }
        }
    }
}

/**
 * Video details update effect - syncs details from browse list
 */
@Composable
internal fun VideoDetailsUpdateEffect(
    browseVideos: List<VideoSearchResult>,
    showVideoDetailsDialog: VideoSearchResult?,
    homeViewModel: HomeViewModel
) {
    LaunchedEffect(browseVideos, showVideoDetailsDialog?.videoId) {
        val currentId = showVideoDetailsDialog?.videoId ?: return@LaunchedEffect
        val updated = browseVideos.firstOrNull { it.videoId == currentId } ?: return@LaunchedEffect
        homeViewModel.updateUiState { it.copy(showVideoDetailsDialog = updated) }
    }
}
