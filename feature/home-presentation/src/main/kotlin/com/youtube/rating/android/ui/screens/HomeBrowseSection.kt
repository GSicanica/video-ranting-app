package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import androidx.paging.compose.LazyPagingItems
import com.youtube.rating.shared.models.VideoSearchResult

// -----------------------------------------------------------------------------
// Browse section
// -----------------------------------------------------------------------------
@Composable
internal fun BrowseSection(
    isOnline: Boolean,
    searchQuery: String,
    browseItems: LazyPagingItems<VideoSearchResult>,
    orderedBrowseVideos: List<VideoSearchResult> = emptyList(),
    isBrowsing: Boolean,
    browseError: String?,
    randomLoading: Boolean,
    randomProgress: Float,
    isGridView: Boolean,
    isAdminMode: Boolean,
    disableScrollEffects: Boolean,
    onResetFilters: () -> Unit,
    onClearSearch: () -> Unit,
    onRetry: () -> Unit,
    onAddVideo: () -> Unit,
    onVideoClick: (VideoSearchResult) -> Unit,
    onFavoriteClick: (VideoSearchResult) -> Unit,
    onQuickRateClick: (VideoSearchResult) -> Unit,
    onLongPress: (VideoSearchResult) -> Unit,
    onReportClick: (VideoSearchResult) -> Unit,
    isFavorite: (String) -> Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    gridState: LazyGridState,
    listState: LazyListState,
    headerContent: (@Composable () -> Unit)? = null
) {
    if (randomLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Random videi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = { randomProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }
    if (!isOnline) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Offline - Nema internet konekcije",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.retry, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }

    val hasOrderedVideos = orderedBrowseVideos.isNotEmpty()
    val itemCount = if (hasOrderedVideos) orderedBrowseVideos.size else browseItems.itemCount

    if (isBrowsing && itemCount == 0 && browseError == null) {
        if (isGridView) GridSkeleton() else ListSkeleton()
        return
    }

    when {
        // -------------------------------------------------------------
        // ✅ Ima videa → grid ili list view
        // -------------------------------------------------------------
        itemCount > 0 -> {
            if (isGridView) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (hasOrderedVideos) {
                        LazyVideoGrid(
                            videos = orderedBrowseVideos,
                            onVideoClick = onVideoClick,
                            onFavoriteClick = onFavoriteClick,
                            onLongPress = onLongPress,
                            onReportClick = onReportClick,
                            isFavorite = isFavorite,
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            state = gridState,
                            disableScrollEffects = disableScrollEffects,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            enablePullDownOpenFilters = false,
                            onPullDownOpenFilters = null,
                            headerContent = headerContent
                        )
                    } else {
                        LazyPagingVideoGrid(
                            items = browseItems,
                            onVideoClick = onVideoClick,
                            onFavoriteClick = onFavoriteClick,
                            onLongPress = onLongPress,
                            onReportClick = onReportClick,
                            isFavorite = isFavorite,
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            state = gridState,
                            disableScrollEffects = disableScrollEffects,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            enablePullDownOpenFilters = false,
                            onPullDownOpenFilters = null,
                            headerContent = headerContent
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (hasOrderedVideos) {
                        LazyVideoList(
                            videos = orderedBrowseVideos,
                            onVideoClick = onVideoClick,
                            onFavoriteClick = onFavoriteClick,
                            onQuickRateClick = onQuickRateClick,
                            onLongPress = onLongPress,
                            onReportClick = onReportClick,
                            isFavorite = isFavorite,
                            isAdminMode = isAdminMode,
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            state = listState,
                            disableScrollEffects = disableScrollEffects,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            enablePullDownOpenFilters = false,
                            onPullDownOpenFilters = null,
                            headerContent = headerContent
                        )
                    } else {
                        LazyPagingVideoList(
                            items = browseItems,
                            onVideoClick = onVideoClick,
                            onFavoriteClick = onFavoriteClick,
                            onQuickRateClick = onQuickRateClick,
                            onLongPress = onLongPress,
                            onReportClick = onReportClick,
                            isFavorite = isFavorite,
                            isAdminMode = isAdminMode,
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            state = listState,
                            disableScrollEffects = disableScrollEffects,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            enablePullDownOpenFilters = false,
                            onPullDownOpenFilters = null,
                            headerContent = headerContent
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 🟡 Nema videa (ali nema greške)
        // -------------------------------------------------------------
        !isBrowsing && browseError == null -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(40.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = Strings.noVideosDisplay,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = Strings.pullRefreshOrAdd,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.refresh)
                        }

                        OutlinedButton(
                            onClick = onAddVideo,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.addVideo)
                        }
                    }

                    TextButton(onClick = onResetFilters) { Text(Strings.resetView) }
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "\"$searchQuery\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onClearSearch) {
                            Text(Strings.clearSearch)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 🔴 Greška pri učitavanju
        // -------------------------------------------------------------
        browseError != null -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = Strings.loadError,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = browseError,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.tryAgain)
                    }
                }
            }
        }
    }
}
