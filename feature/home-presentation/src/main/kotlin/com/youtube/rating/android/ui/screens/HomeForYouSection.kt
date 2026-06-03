package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.shared.models.VideoSearchResult

@Composable
internal fun HomeForYouSection(
    videos: List<VideoSearchResult>,
    isLoading: Boolean,
    error: String?,
    isColdStart: Boolean,
    isGridView: Boolean,
    isAdminMode: Boolean,
    disableScrollEffects: Boolean,
    gridState: LazyGridState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onVideoClick: (VideoSearchResult) -> Unit,
    onFavoriteClick: (VideoSearchResult) -> Unit,
    onQuickRateClick: (VideoSearchResult) -> Unit,
    onLongPress: (VideoSearchResult) -> Unit,
    onReportClick: (VideoSearchResult) -> Unit,
    isFavorite: (String) -> Boolean
) {
    if (isLoading && videos.isEmpty()) {
        if (isGridView) GridSkeleton() else ListSkeleton()
        return
    }

    if (error != null && videos.isEmpty()) {
        ErrorCard(
            title = Strings.loadError,
            message = error,
            onRetry = onRetry
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        if (videos.isEmpty()) {
            ErrorCard(
                title = "For You",
                message = Strings.noVideosDisplay,
                onRetry = onRetry
            )
            return@Column
        }

        if (isGridView) {
            LazyVideoGrid(
                videos = videos,
                onVideoClick = onVideoClick,
                onFavoriteClick = onFavoriteClick,
                onLongPress = onLongPress,
                onReportClick = onReportClick,
                isFavorite = isFavorite,
                state = gridState,
                disableScrollEffects = disableScrollEffects,
                onLoadMore = null,
                isLoadingMore = false,
                hasMore = false,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVideoList(
                videos = videos,
                onVideoClick = onVideoClick,
                onFavoriteClick = onFavoriteClick,
                onQuickRateClick = onQuickRateClick,
                onLongPress = onLongPress,
                onReportClick = onReportClick,
                isFavorite = isFavorite,
                isAdminMode = isAdminMode,
                state = listState,
                disableScrollEffects = disableScrollEffects,
                onLoadMore = null,
                isLoadingMore = false,
                hasMore = false,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

