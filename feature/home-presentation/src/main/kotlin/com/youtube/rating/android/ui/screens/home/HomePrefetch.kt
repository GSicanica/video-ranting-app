package com.youtube.rating.android.ui.screens.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.youtube.rating.android.utils.PerformanceProfile
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

internal fun idsSignature(ids: List<String>): Long {
    var acc = 1125899906842597L
    for (s in ids) acc = (acc * 31L) + s.hashCode().toLong()
    return acc
}

@Composable
internal fun HomePrefetchController(
    shouldShowBrowse: Boolean,
    isGridView: Boolean,
    gridScrollState: LazyGridState,
    listScrollState: LazyListState,
    browseVideos: List<VideoSearchResult>,
    isBrowsing: Boolean,
    isLoadingMore: Boolean,
    isShuffling: Boolean,
    perfProfile: PerformanceProfile.Profile,
    shouldPrefetch: (List<String>) -> Boolean,
    performPrefetch: (List<String>) -> Unit
) {
    val browseVideosState by rememberUpdatedState(browseVideos)
    val isBrowsingState by rememberUpdatedState(isBrowsing)
    val isLoadingMoreState by rememberUpdatedState(isLoadingMore)
    val isShufflingState by rememberUpdatedState(isShuffling)

    LaunchedEffect(shouldShowBrowse, browseVideos, isBrowsing, isLoadingMore, isShuffling) {
        if (!shouldShowBrowse || browseVideos.isEmpty()) return@LaunchedEffect
        if (isBrowsing || isLoadingMore || isShuffling) return@LaunchedEffect

        val initialIds = browseVideos.take(perfProfile.prefetchBatchSize).map { it.videoId }
        if (shouldPrefetch(initialIds)) {
            performPrefetch(initialIds)
        }
    }

    LaunchedEffect(shouldShowBrowse, isGridView) {
        if (!shouldShowBrowse) return@LaunchedEffect

        if (isGridView) {
            snapshotFlow {
                Triple(
                    gridScrollState.firstVisibleItemIndex,
                    browseVideosState.size,
                    gridScrollState.isScrollInProgress
                )
            }
                .distinctUntilChanged()
                .collectLatest { (firstIndex, size, isScrolling) ->
                    if (isScrolling) return@collectLatest
                    if (size == 0) return@collectLatest
                    if (isBrowsingState || isLoadingMoreState || isShufflingState) return@collectLatest
                    val ids = (firstIndex until (firstIndex + perfProfile.prefetchBatchSize))
                        .mapNotNull { browseVideosState.getOrNull(it)?.videoId }
                    if (shouldPrefetch(ids)) {
                        performPrefetch(ids)
                    }
                }
        } else {
            snapshotFlow {
                Triple(
                    listScrollState.firstVisibleItemIndex,
                    browseVideosState.size,
                    listScrollState.isScrollInProgress
                )
            }
                .distinctUntilChanged()
                .collectLatest { (firstIndex, size, isScrolling) ->
                    if (isScrolling) return@collectLatest
                    if (size == 0) return@collectLatest
                    if (isBrowsingState || isLoadingMoreState || isShufflingState) return@collectLatest
                    val ids = (firstIndex until (firstIndex + perfProfile.prefetchBatchSize))
                        .mapNotNull { browseVideosState.getOrNull(it)?.videoId }
                    if (shouldPrefetch(ids)) {
                        performPrefetch(ids)
                    }
                }
        }
    }
}

