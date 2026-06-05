@file:Suppress("FunctionName")
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.youtube.rating.android.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.utils.DeepLinkUtil
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.utils.rememberHapticFeedback
import com.youtube.rating.android.utils.SaintOfDayManager
import com.youtube.rating.android.ui.screens.home.dialogs.VideoDetailsDialog
import com.youtube.rating.core.designsystem.theme.spacing
import com.youtube.rating.android.viewmodel.HomeViewModel
import com.youtube.rating.android.viewmodel.HomeViewModelFactory
import com.youtube.rating.android.viewmodel.RatingViewModel
import com.youtube.rating.android.viewmodel.RatingViewModelFactory
import com.youtube.rating.android.youtube.YouTubeInfoService
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.android.domain.usecase.home.GetHomeCategoriesUseCase
import com.youtube.rating.android.domain.usecase.home.GetPopularSearchTermsUseCase
import com.youtube.rating.android.domain.usecase.home.GetTopVideosUseCase
import com.youtube.rating.android.domain.usecase.home.ReportVideoUseCase
import com.youtube.rating.android.domain.usecase.home.SearchVideosUseCase
import com.youtube.rating.shared.models.VideoSearchResult
import org.koin.compose.koinInject
import java.util.Locale

private fun Context.findGalleryActivityOrNull(): ComponentActivity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    com.youtube.rating.android.sentry.SentryLogger.captureMessage(
        "FeedHomeScreen: Context is not a ComponentActivity",
        tags = mapOf("context_class" to this::class.java.name)
    )
    return null
}

@Composable
fun GalleryHomeScreen(
    isInPipMode: Boolean,
    onFullScreenVideoActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val deps = rememberHomeScreenDependencies()
    val app = remember(context) { context.applicationContext as? Application }

    val galleryActivity = context.findGalleryActivityOrNull()
    if (galleryActivity == null || app == null) {
        androidx.compose.material3.Text(Strings.missingActivityContext)
        return
    }
    val ratingViewModel: RatingViewModel = viewModel(
        viewModelStoreOwner = galleryActivity,
        factory = deps.ratingViewModelFactory(application = app)
    )
    val homeViewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = galleryActivity,
        factory = deps.homeViewModelFactory(owner = galleryActivity)
    )

    val scope = rememberCoroutineScope()
    val haptic = rememberHapticFeedback()

    // ✅ OPTIMIZOVANO: Koristi homeState umjesto individual StateFlows
    val homeState by homeViewModel.homeState.collectAsStateWithLifecycle()
    val browsePagingItems = homeViewModel.browsePagingData.collectAsLazyPagingItems()
    val isBrowsing = browsePagingItems.loadState.refresh is androidx.paging.LoadState.Loading
    val browseError = (browsePagingItems.loadState.refresh as? androidx.paging.LoadState.Error)
        ?.error?.message
    val appendState = browsePagingItems.loadState.append
    val isLoadingMore = appendState is androidx.paging.LoadState.Loading
    val hasMore = (appendState as? androidx.paging.LoadState.NotLoading)?.endOfPaginationReached != true
    val totalResults = homeState.totalResults
    val favoriteIds = homeState.favoriteIds
    val isOnline = homeState.isOnline
    val languageCodes = homeState.languageCodes
    val submitState by ratingViewModel.submitState.collectAsStateWithLifecycle()

    val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val isFavoriteMemo = remember(favoriteIds) { { id: String -> favoriteIds.contains(id) } }

    var showVideoDetailsDialog by remember { mutableStateOf<VideoSearchResult?>(null) }
    val spacing = MaterialTheme.spacing

    fun refreshBrowse(resetPage: Boolean = true, forceRefresh: Boolean = false) {
        homeViewModel.loadBrowseVideos(languageCodes, resetPage = resetPage, forceRefresh = forceRefresh)
    }

    val toggleFavorite: (VideoSearchResult) -> Unit = remember(homeViewModel, haptic, isFavoriteMemo) {
        { video: VideoSearchResult ->
            if (isFavoriteMemo(video.videoId)) {
                homeViewModel.removeFromFavorites(video.videoId) { if (it) haptic.lightTap() }
            } else {
                homeViewModel.addToFavorites(video) { if (it) haptic.lightTap() }
            }
        }
    }

    val shareVideo: (String) -> Unit = remember(context) {
        { videoId: String ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, DeepLinkUtil.getAppShareUrl(videoId))
            }
            val chooser = Intent.createChooser(shareIntent, Strings.share)
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    // Initial load
    LaunchedEffect(languageCodes) {
        if (browsePagingItems.itemCount == 0) {
            refreshBrowse(resetPage = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            GalleryHeader(totalResults = totalResults)

            // Content
            when {
                !isOnline -> {
                    GalleryEmptyState(
                        icon = Icons.Default.Error,
                        title = "Offline",
                        subtitle = "Nema internet veze",
                        onAction = { refreshBrowse() },
                        actionText = "Pokušaj ponovno"
                    )
                }
                isBrowsing && browsePagingItems.itemCount == 0 && browseError == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                browseError != null && browsePagingItems.itemCount == 0 -> {
                    GalleryEmptyState(
                        icon = Icons.Default.Error,
                        title = "Greška",
                        subtitle = browseError ?: "",
                        onAction = { refreshBrowse() },
                        actionText = "Pokušaj ponovno"
                    )
                }
                browsePagingItems.itemCount == 0 -> {
                    GalleryEmptyState(
                        icon = Icons.Outlined.VideoLibrary,
                        title = "Nema videa",
                        subtitle = "Pokušaj osvježiti ili promijeni kategoriju",
                        onAction = { refreshBrowse() },
                        actionText = "Osvježi"
                    )
                }
                else -> {
                    com.youtube.rating.core.designsystem.components.PullToRefreshBox(
                        isRefreshing = isBrowsing && browsePagingItems.itemCount == 0,
                        onRefresh = { refreshBrowse(resetPage = true, forceRefresh = true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(
                                start = spacing.lg, end = spacing.lg,
                                top = spacing.xs, bottom = 80.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            items(
                                count = browsePagingItems.itemCount,
                                key = { idx -> browsePagingItems[idx]?.videoId ?: "placeholder_$idx" }
                            ) { index ->
                                val video = browsePagingItems[index] ?: return@items
                                Box(modifier = Modifier.animateItem()) {
                                    GalleryTile(
                                        video = video,
                                        onClick = { showVideoDetailsDialog = video },
                                        onLongPress = { toggleFavorite(video) }
                                    )
                                }
                            }

                            if (isLoadingMore) {
                                item(
                                    key = "gallery_loading",
                                    span = { GridItemSpan(2) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(spacing.md),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Video Details Dialog
    showVideoDetailsDialog?.let { video ->
        VideoDetailsDialog(
            video = video,
            onQuickRate = { },
            submitState = submitState,
            isFavorite = isFavoriteMemo(video.videoId),
            onToggleFavorite = { toggleFavorite(video) },
            onShare = { shareVideo(video.videoId) },
            onCloseNoMini = { showVideoDetailsDialog = null },
            onDismiss = { showVideoDetailsDialog = null },
            isInPipMode = isInPipMode,
            onFullScreenVideoActiveChange = onFullScreenVideoActiveChange
        )
    }
}

// ---------------------------------------------------------------------------
// Gallery Header – title
// ---------------------------------------------------------------------------
@Composable
private fun GalleryHeader(
    totalResults: Int
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.sm, bottom = spacing.xs)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Galerija",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                if (totalResults > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "$totalResults",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 2.dp)
                        )
                    }
                }
            }
        }

    }
}

// ---------------------------------------------------------------------------
// Gallery Tile – square-ish thumbnail with gradient overlay info
// ---------------------------------------------------------------------------
@Composable
private fun GalleryTile(
    video: VideoSearchResult,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    var thumbUrl by remember(video.videoId) {
        mutableStateOf(video.thumbnail.ifEmpty { ThumbnailHelper.getThumbnailUrl(video.videoId) })
    }
    val imageRequest = remember(context, thumbUrl) {
        ImageRequest.Builder(context)
            .data(thumbUrl)
            .size(400, 300)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .error(android.R.drawable.ic_menu_gallery)
            .build()
    }

    val avgText = remember(video.totalRatings, video.avgTotal) {
        if (video.totalRatings > 0) String.format(Locale.getDefault(), "%.1f", video.avgTotal)
        else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail - slightly taller aspect for gallery feel
            AsyncImage(
                model = imageRequest,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
                onError = {
                    val hq = "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg"
                    if (thumbUrl != hq) thumbUrl = hq
                }
            )

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
            )

            // Rating star top-right
            if (avgText != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(spacing.xs),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.xs, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color(0xFFFFD54F)
                        )
                        Text(
                            text = avgText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(spacing.sm)
            ) {
                Text(
                    text = video.title.ifBlank { "Bez naslova" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Text(
                    text = video.channelName,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Gallery Empty State
// ---------------------------------------------------------------------------
@Composable
private fun GalleryEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onAction: () -> Unit,
    actionText: String
) {
    val spacing = MaterialTheme.spacing
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xl),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(spacing.xs))
                TextButton(onClick = onAction) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text(actionText)
                }
            }
        }
    }
}
