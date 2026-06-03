package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val INFINITE_SCROLL_THRESHOLD_GRID = 4
private const val INFINITE_SCROLL_THRESHOLD_LIST = 3

@Composable
private fun InfiniteScrollGridEffect(
    state: LazyGridState,
    totalItems: Int,
    threshold: Int = 6,
    hasMore: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(state, totalItems, hasMore, isLoading) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collectLatest { lastVisible ->
                if (!isLoading && hasMore && totalItems > 0 && lastVisible >= totalItems - threshold) {
                    onLoadMore()
                }
            }
    }
}

@Composable
private fun InfiniteScrollListEffect(
    state: LazyListState,
    totalItems: Int,
    threshold: Int = 4,
    hasMore: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(state, totalItems, hasMore, isLoading) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collectLatest { lastVisible ->
                if (!isLoading && hasMore && totalItems > 0 && lastVisible >= totalItems - threshold) {
                    onLoadMore()
                }
            }
    }
}

@Composable
fun LazyVideoGrid(
    videos: List<VideoSearchResult>,
    onVideoClick: (VideoSearchResult) -> Unit,
    onFavoriteClick: (VideoSearchResult) -> Unit,
    onLongPress: (VideoSearchResult) -> Unit,
    onReportClick: (VideoSearchResult) -> Unit,
    isFavorite: (String) -> Boolean,
    modifier: Modifier = Modifier,
    onLoadMore: (() -> Unit)? = null,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    state: LazyGridState,
    disableScrollEffects: Boolean = false,
    enablePullDownOpenFilters: Boolean = false,
    onPullDownOpenFilters: (() -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
) {
    val effectiveIsScrolling by remember(state, disableScrollEffects) {
        derivedStateOf { !disableScrollEffects && state.isScrollInProgress }
    }

    InfiniteScrollGridEffect(
        state = state,
        totalItems = videos.size,
        threshold = INFINITE_SCROLL_THRESHOLD_GRID,
        hasMore = hasMore,
        isLoading = isLoadingMore,
        onLoadMore = { onLoadMore?.invoke() ?: Unit }
    )

    val pullConn = rememberPullDownToOpenSheet(
        enabled = enablePullDownOpenFilters && onPullDownOpenFilters != null,
        isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        onTriggered = { onPullDownOpenFilters?.invoke() },
        threshold = 16.dp
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = modifier.nestedScroll(pullConn),
        contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (headerContent != null) {
            item(
                key = "header",
                contentType = "header",
                span = { GridItemSpan(2) }
            ) {
                headerContent()
            }
        }

        items(
            items = videos,
            key = { it.videoId },
            contentType = { "video" }
        ) { video ->
            VideoGridCard(
                video = video,
                onClick = { onVideoClick(video) },
                onLongPress = { onLongPress(video) },
                onFavoriteClick = { onFavoriteClick(video) },
                isFavorite = isFavorite(video.videoId),
                onReport = { onReportClick(video) },
                isScrolling = effectiveIsScrolling,
                allowOverlays = true,
                modifier = Modifier.padding(2.dp)
            )
        }

        if (isLoadingMore && hasMore) {
            item(
                key = "loading",
                contentType = "loading",
                span = { GridItemSpan(2) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
fun LazyVideoList(
    videos: List<VideoSearchResult>,
    onVideoClick: (VideoSearchResult) -> Unit,
    onFavoriteClick: (VideoSearchResult) -> Unit,
    onQuickRateClick: (VideoSearchResult) -> Unit,
    onLongPress: (VideoSearchResult) -> Unit,
    onReportClick: (VideoSearchResult) -> Unit,
    isFavorite: (String) -> Boolean,
    isAdminMode: Boolean = false,
    modifier: Modifier = Modifier,
    onLoadMore: (() -> Unit)? = null,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    state: LazyListState,
    disableScrollEffects: Boolean = false,
    enablePullDownOpenFilters: Boolean = false,
    onPullDownOpenFilters: (() -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
) {
    val itemShape = remember { RoundedCornerShape(12.dp) }
    val latestOnVideoClick by rememberUpdatedState(onVideoClick)
    val latestOnLongPress by rememberUpdatedState(onLongPress)
    val latestOnFavoriteClick by rememberUpdatedState(onFavoriteClick)
    val latestOnQuickRateClick by rememberUpdatedState(onQuickRateClick)
    val latestOnReportClick by rememberUpdatedState(onReportClick)
    val swipePositionalThreshold = remember { { distance: Float -> distance * 0.35f } }

    val effectiveIsScrolling by remember(state, disableScrollEffects) {
        derivedStateOf { !disableScrollEffects && state.isScrollInProgress }
    }

    InfiniteScrollListEffect(
        state = state,
        totalItems = videos.size,
        threshold = INFINITE_SCROLL_THRESHOLD_LIST,
        hasMore = hasMore,
        isLoading = isLoadingMore,
        onLoadMore = { onLoadMore?.invoke() ?: Unit }
    )

    val pullConn = rememberPullDownToOpenSheet(
        enabled = enablePullDownOpenFilters && onPullDownOpenFilters != null,
        isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        onTriggered = { onPullDownOpenFilters?.invoke() },
        threshold = 16.dp
    )

    LazyColumn(
        state = state,
        modifier = modifier.nestedScroll(pullConn),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (headerContent != null) {
            item(key = "header", contentType = "header") {
                headerContent()
            }
        }

        items(
            items = videos,
            key = { it.videoId },
            contentType = { "video" }
        ) { video ->
            if (effectiveIsScrolling) {
                Box(Modifier.clip(itemShape)) {
                    VideoSearchCard(
                        video = video,
                        onClick = { latestOnVideoClick(video) },
                        onLongPress = { latestOnLongPress(video) },
                        onQuickRateClick = { latestOnQuickRateClick(video) },
                        onFavoriteClick = { latestOnFavoriteClick(video) },
                        isFavorite = isFavorite(video.videoId),
                        isScrolling = true,
                        onDelete = {},
                        onReport = { latestOnReportClick(video) },
                        isAdminMode = isAdminMode
                    )
                }
            } else {
                val dismissState = rememberSwipeToDismissBoxState(
                    positionalThreshold = swipePositionalThreshold,
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                latestOnFavoriteClick(video)
                                false
                            }
                            SwipeToDismissBoxValue.EndToStart -> {
                                latestOnQuickRateClick(video)
                                false
                            }
                            SwipeToDismissBoxValue.Settled -> true
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    modifier = Modifier.clip(itemShape),
                    backgroundContent = {
                        val isStart = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
                        val isEnd = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

                        val bgColor = when {
                            isStart -> MaterialTheme.colorScheme.primaryContainer
                            isEnd -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(itemShape)
                                .background(bgColor)
                                .padding(horizontal = 16.dp),
                            contentAlignment = if (isStart) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            if (isStart || isEnd) {
                                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = if (isStart) Icons.Default.Favorite else Icons.Default.Star
                                    val label = if (isStart) "Favorit" else "Brza ocjena"
                                    Icon(icon, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    androidx.compose.material3.Text(label)
                                }
                            }
                        }
                    }
                ) {
                    Box(Modifier.clip(itemShape)) {
                        VideoSearchCard(
                            video = video,
                            onClick = { latestOnVideoClick(video) },
                            onLongPress = { latestOnLongPress(video) },
                            onQuickRateClick = { latestOnQuickRateClick(video) },
                            onFavoriteClick = { latestOnFavoriteClick(video) },
                            isFavorite = isFavorite(video.videoId),
                            isScrolling = false,
                            onDelete = {},
                            onReport = { latestOnReportClick(video) },
                            isAdminMode = isAdminMode
                        )
                    }
                }
            }
        }

        if (isLoadingMore && hasMore) {
            item(key = "loading", contentType = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
fun LazyPagingVideoGrid(
    items: LazyPagingItems<VideoSearchResult>,
    onVideoClick: (VideoSearchResult) -> Unit,
    onFavoriteClick: (VideoSearchResult) -> Unit,
    onLongPress: (VideoSearchResult) -> Unit,
    onReportClick: (VideoSearchResult) -> Unit,
    isFavorite: (String) -> Boolean,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    state: LazyGridState,
    disableScrollEffects: Boolean = false,
    enablePullDownOpenFilters: Boolean = false,
    onPullDownOpenFilters: (() -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
) {
    val effectiveIsScrolling by remember(state, disableScrollEffects) {
        derivedStateOf { !disableScrollEffects && state.isScrollInProgress }
    }

    val pullConn = rememberPullDownToOpenSheet(
        enabled = enablePullDownOpenFilters && onPullDownOpenFilters != null,
        isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        onTriggered = { onPullDownOpenFilters?.invoke() },
        threshold = 16.dp
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = modifier.nestedScroll(pullConn),
        contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (headerContent != null) {
            item(
                key = "header",
                contentType = "header",
                span = { GridItemSpan(2) }
            ) {
                headerContent()
            }
        }

        items(
            count = items.itemCount,
            key = { idx -> items[idx]?.videoId ?: "placeholder_$idx" },
            contentType = { "video" }
        ) { index ->
            val video = items[index] ?: return@items
            VideoGridCard(
                video = video,
                onClick = { onVideoClick(video) },
                onLongPress = { onLongPress(video) },
                onFavoriteClick = { onFavoriteClick(video) },
                isFavorite = isFavorite(video.videoId),
                onReport = { onReportClick(video) },
                isScrolling = effectiveIsScrolling,
                allowOverlays = true,
                modifier = Modifier.padding(2.dp)
            )
        }

        if (isLoadingMore && hasMore) {
            item(
                key = "loading",
                contentType = "loading",
                span = { GridItemSpan(2) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
fun LazyPagingVideoList(
    items: LazyPagingItems<VideoSearchResult>,
    onVideoClick: (VideoSearchResult) -> Unit,
    onFavoriteClick: (VideoSearchResult) -> Unit,
    onQuickRateClick: (VideoSearchResult) -> Unit,
    onLongPress: (VideoSearchResult) -> Unit,
    onReportClick: (VideoSearchResult) -> Unit,
    isFavorite: (String) -> Boolean,
    isAdminMode: Boolean = false,
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    state: LazyListState,
    disableScrollEffects: Boolean = false,
    enablePullDownOpenFilters: Boolean = false,
    onPullDownOpenFilters: (() -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
) {
    val itemShape = remember { RoundedCornerShape(12.dp) }
    val effectiveIsScrolling by remember(state, disableScrollEffects) {
        derivedStateOf { !disableScrollEffects && state.isScrollInProgress }
    }

    val pullConn = rememberPullDownToOpenSheet(
        enabled = enablePullDownOpenFilters && onPullDownOpenFilters != null,
        isAtTop = { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 },
        onTriggered = { onPullDownOpenFilters?.invoke() },
        threshold = 16.dp
    )

    LazyColumn(
        state = state,
        modifier = modifier.nestedScroll(pullConn),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (headerContent != null) {
            item(key = "header", contentType = "header") {
                headerContent()
            }
        }

        items(
            count = items.itemCount,
            key = { idx -> items[idx]?.videoId ?: "placeholder_$idx" },
            contentType = { "video" }
        ) { index ->
            val video = items[index] ?: return@items
            VideoSearchCard(
                video = video,
                isFavorite = isFavorite(video.videoId),
                onClick = { onVideoClick(video) },
                onFavoriteClick = { onFavoriteClick(video) },
                onQuickRateClick = { onQuickRateClick(video) },
                onLongPress = { onLongPress(video) },
                onReport = { onReportClick(video) },
                isAdminMode = isAdminMode,
                isScrolling = effectiveIsScrolling,
            )
        }

        if (isLoadingMore && hasMore) {
            item(key = "loading", contentType = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
fun rememberPullDownToOpenSheet(
    enabled: Boolean,
    isAtTop: () -> Boolean,
    onTriggered: () -> Unit,
    threshold: Dp = 56.dp
): NestedScrollConnection {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = remember(threshold, density) { with(density) { threshold.toPx() } }

    val latestIsAtTop by rememberUpdatedState(isAtTop)
    val latestOnTriggered by rememberUpdatedState(onTriggered)

    var dragPx by remember { mutableFloatStateOf(0f) }
    var fired by remember { mutableStateOf(false) }

    return remember(enabled, thresholdPx) {
        object : NestedScrollConnection {

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                if (available.y > 0f && latestIsAtTop()) {
                    dragPx += available.y

                    if (!fired && dragPx >= thresholdPx) {
                        fired = true
                        dragPx = 0f
                        latestOnTriggered()
                        return Offset.Zero
                    }
                }

                if (available.y < 0f) {
                    dragPx = 0f
                    fired = false
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                dragPx = 0f
                fired = false
                return Velocity.Zero
            }
        }
    }
}
