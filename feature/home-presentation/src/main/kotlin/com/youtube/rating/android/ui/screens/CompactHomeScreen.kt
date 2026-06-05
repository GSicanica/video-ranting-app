@file:Suppress("FunctionName")
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.youtube.rating.android.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.utils.AdminManager
import com.youtube.rating.android.utils.CompactRatingBadge
import com.youtube.rating.android.utils.DeepLinkUtil
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.utils.SaintOfDayManager
import com.youtube.rating.android.utils.rememberHapticFeedback
import com.youtube.rating.android.ui.screens.home.dialogs.VideoDetailsDialog
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.koinInject
import com.youtube.rating.android.data.prefs.AdminPrefs

private const val COMPACT_INFINITE_SCROLL_THRESHOLD = 4

private fun Context.findCompactActivityOrNull(): ComponentActivity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    com.youtube.rating.android.sentry.SentryLogger.captureMessage(
        "CompactHomeScreen: Context is not a ComponentActivity",
        tags = mapOf("context_class" to this::class.java.name)
    )
    return null
}

@Composable
fun CompactHomeScreen(
    adminManager: AdminManager = koinInject(),
    isInPipMode: Boolean,
    onFullScreenVideoActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val deps = rememberHomeScreenDependencies()
    val app = remember(context) { context.applicationContext as? Application }

    val compactActivity = context.findCompactActivityOrNull()
    if (compactActivity == null || app == null) {
        androidx.compose.material3.Text(Strings.missingActivityContext)
        return
    }
    val ratingViewModel: RatingViewModel = viewModel(
        viewModelStoreOwner = compactActivity,
        factory = deps.ratingViewModelFactory(application = app)
    )
    val homeViewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = compactActivity,
        factory = deps.homeViewModelFactory(owner = compactActivity)
    )

    val scope = rememberCoroutineScope()
    val haptic = rememberHapticFeedback()

    // --- Home VM state ---
    // ✅ OPTIMIZOVANO: Koristi homeState umjesto individual StateFlows
    val homeState by homeViewModel.homeState.collectAsStateWithLifecycle()
    val browsePagingItems = homeViewModel.browsePagingData.collectAsLazyPagingItems()
    val isBrowsing = browsePagingItems.loadState.refresh is androidx.paging.LoadState.Loading
    val browseError = (browsePagingItems.loadState.refresh as? androidx.paging.LoadState.Error)
        ?.error?.message
    val searchQuery = homeState.searchQuery
    val appendState = browsePagingItems.loadState.append
    val isLoadingMore = appendState is androidx.paging.LoadState.Loading
    val hasMore = (appendState as? androidx.paging.LoadState.NotLoading)?.endOfPaginationReached != true
    val totalResults = homeState.totalResults
    val favoriteIds = homeState.favoriteIds
    val isOnline = homeState.isOnline
    val languageCodes = homeState.languageCodes
    val submitState by ratingViewModel.submitState.collectAsStateWithLifecycle()

    val isAdminMode by AdminPrefs.adminModeFlow(context)
        .collectAsStateWithLifecycle(initialValue = adminManager.isAdminMode())

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val isFavoriteMemo = remember(favoriteIds) { { id: String -> favoriteIds.contains(id) } }

    // Dialog / search states
    var showVideoDetailsDialog by remember { mutableStateOf<VideoSearchResult?>(null) }
    var showAddVideoSheet by remember { mutableStateOf(false) }
    var addVideoUrl by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }

    fun refreshBrowse(resetPage: Boolean = true, forceRefresh: Boolean = false) {
        homeViewModel.loadBrowseVideos(languageCodes, resetPage = resetPage, forceRefresh = forceRefresh)
    }

    val toggleFavorite = remember(homeViewModel, haptic, isFavoriteMemo) {
        { video: VideoSearchResult ->
            if (isFavoriteMemo(video.videoId)) {
                homeViewModel.removeFromFavorites(video.videoId) { success ->
                    if (success) haptic.lightTap()
                }
            } else {
                homeViewModel.addToFavorites(video) { success ->
                    if (success) haptic.lightTap()
                }
            }
        }
    }

    val shareVideo = remember(context) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // --- Top Bar ---
    CompactFeedTopBar(
        searchExpanded = searchExpanded,
        searchQuery = searchQuery,
        totalResults = totalResults,
        isOnline = isOnline,
        onToggleSearch = { searchExpanded = !searchExpanded },
        onSearchQueryChange = { homeViewModel.updateSearchQuery(it) },
        onSearchClear = {
            homeViewModel.updateSearchQuery("")
            searchExpanded = false
            refreshBrowse(resetPage = true)
        },
        onSearchSubmit = { refreshBrowse(resetPage = true) },
        onRefresh = { refreshBrowse(resetPage = true, forceRefresh = true) },
        onAddClick = { showAddVideoSheet = true }
    )

            // --- Content ---
            com.youtube.rating.core.designsystem.components.PullToRefreshBox(
                isRefreshing = isBrowsing && browsePagingItems.itemCount == 0,
                onRefresh = { refreshBrowse(resetPage = true, forceRefresh = true) },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    !isOnline -> {
                        CompactOfflineCard()
                    }
                    isBrowsing && browsePagingItems.itemCount == 0 && browseError == null -> {
                        CompactLoadingSkeleton()
                    }
                    browsePagingItems.itemCount > 0 -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                count = browsePagingItems.itemCount,
                                key = { idx -> browsePagingItems[idx]?.videoId ?: "placeholder_$idx" }
                            ) { index ->
                                val video = browsePagingItems[index] ?: return@items
                                CompactFeedCard(
                                    video = video,
                                    onClick = { showVideoDetailsDialog = video },
                                    onLongPress = { toggleFavorite(video) },
                                    onShareClick = { shareVideo(video.videoId) },
                                    modifier = Modifier.animateItem()
                                )
                            }

                            if (isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    browseError != null -> {
                        CompactErrorState(
                            error = browseError ?: "Greška",
                            onRetry = { refreshBrowse(resetPage = true) }
                        )
                    }
                    else -> {
                        CompactEmptyState(
                            onRefresh = { refreshBrowse(resetPage = true) }
                        )
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

    // Add Video Sheet
    if (showAddVideoSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddVideoSheet = false
                addVideoUrl = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Dodaj YouTube video",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = addVideoUrl,
                    onValueChange = { addVideoUrl = it },
                    label = { Text(Strings.enterYoutubeUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (addVideoUrl.isNotBlank()) {
                            val videoId = com.youtube.rating.android.util.extractYouTubeVideoId(addVideoUrl)
                            if (videoId != null) {
                                ratingViewModel.loadVideoInfo(videoId)
                                showAddVideoSheet = false
                                addVideoUrl = ""
                            }
                        }
                    })
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        showAddVideoSheet = false
                        addVideoUrl = ""
                    }) {
                        Text(Strings.cancel)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        if (addVideoUrl.isNotBlank()) {
                            val videoId = com.youtube.rating.android.util.extractYouTubeVideoId(addVideoUrl)
                            if (videoId != null) {
                                ratingViewModel.loadVideoInfo(videoId)
                                showAddVideoSheet = false
                                addVideoUrl = ""
                            }
                        }
                    }) {
                        Text(Strings.add)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top Bar — minimal header with search toggle
// ---------------------------------------------------------------------------
@Composable
private fun CompactFeedTopBar(
    searchExpanded: Boolean,
    searchQuery: String,
    totalResults: Int,
    isOnline: Boolean,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onSearchSubmit: () -> Unit,
    onRefresh: () -> Unit,
    onAddClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Main header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Početna",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "Kompaktni pregled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (isOnline) Color(0xFF22C55E).copy(alpha = 0.16f) else Color(0xFFEF4444).copy(alpha = 0.16f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOnline) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                IconButton(onClick = onToggleSearch, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Pretraži",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Osvježi",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = Strings.addVideo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable search
            AnimatedVisibility(
                visible = searchExpanded,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(Strings.searchPlaceholder) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = onSearchClear) {
                                    Icon(Icons.Default.Clear, contentDescription = "Obriši", modifier = Modifier.size(18.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            onSearchSubmit()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                    if (totalResults > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Rezultata: $totalResults",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Full-width video feed card
// ---------------------------------------------------------------------------
@Composable
private fun CompactFeedCard(
    video: VideoSearchResult,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbnailUrl = remember(video.videoId, video.thumbnail) {
        video.thumbnail.ifEmpty { ThumbnailHelper.getThumbnailUrl(video.videoId) }
    }
    val imageRequest = remember(context, thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .size(720, 405)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .error(android.R.drawable.ic_menu_gallery)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .build()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = video.title.ifBlank { "Bez naslova" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (video.totalRatings > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${video.totalRatings}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactRatingBadge(
                        emoji = "\u2764\uFE0F",
                        value = video.avgLove,
                        color = Color(0xFFE57373)
                    )
                    CompactRatingBadge(
                        emoji = "\u271D\uFE0F",
                        value = video.avgFaith,
                        color = Color(0xFF4DD0E1)
                    )
                    CompactRatingBadge(
                        emoji = "\u2B50",
                        value = video.avgHope,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            IconButton(onClick = onShareClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Podijeli",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------
@Composable
private fun CompactOfflineCard() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(Strings.offlineNoInternet, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun CompactLoadingSkeleton() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Učitavam...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(Strings.tryAgain)
            }
        }
    }
}

@Composable
private fun CompactEmptyState(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Nema videa",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Pokušaj osvježiti ili promijeniti pretragu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(Strings.refresh)
            }
        }
    }
}
