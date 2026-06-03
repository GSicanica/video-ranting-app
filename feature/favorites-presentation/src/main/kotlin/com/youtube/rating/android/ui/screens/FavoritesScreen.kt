@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.youtube.rating.android.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.Divider as M2Divider
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.components.MediaThumbnail
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.youtube.rating.android.data.repository.PsalmsRepository
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.FavoriteItemType
import com.youtube.rating.android.storage.FavoriteVideo
import com.youtube.rating.android.utils.LocalBibleRepository
import com.youtube.rating.android.utils.ThumbnailHelper
import com.youtube.rating.android.utils.rememberHapticFeedback
import com.youtube.rating.core.designsystem.theme.spacing
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtube.rating.shared.models.PsalmDto
import com.youtube.rating.shared.common.multiLet
import kotlinx.coroutines.launch
import com.youtube.rating.core.coroutines.makeIOCall
import org.json.JSONArray
import com.youtube.rating.android.data.prefs.BibleReaderPrefs
import com.youtube.rating.android.data.prefs.PrayerPrefs
import com.youtube.rating.android.data.prefs.PsalmPrefs

// ✅ zaštita: 3 potvrde + na kraju mora upisati "OBRISI"
private const val FINAL_DELETE_WORD = "OBRISI"

private const val GROUP_PSALMS = "Psalmi"
private const val GROUP_PSALMS_HIGHLIGHTED = "Psalm text"
private const val GROUP_BIBLE = "Biblija"
private const val GROUP_LOCAL_PRAYERS = "Molitve"

internal enum class SortMode { NEWEST, OLDEST, BEST }
internal enum class GroupMode { GROUPED, NONE }

private fun normalize(s: String?): String? =
    s?.trim()?.lowercase(Locale.getDefault())?.takeIf { it.isNotBlank() }

private fun FavoriteVideo.isPsalm(): Boolean =
    videoId.startsWith("psalm:") ||
        type == FavoriteItemType.PSALM ||
        type == FavoriteItemType.PSALM_HIGHLIGHTED ||
        normalize(channelName) == "psalmi"

private fun FavoriteVideo.isBibleHighlight(): Boolean =
    videoId.startsWith("bible:")

private fun FavoriteVideo.isImage(): Boolean = type == FavoriteItemType.IMAGE

private fun FavoriteVideo.groupKey(): String {
    if (isPsalm()) return GROUP_PSALMS
    if (isBibleHighlight()) return GROUP_BIBLE
    return "Videi"
}

private fun isProtectedGroup(): Boolean {
    return false
}

private data class GroupBlock(val key: String, val items: List<FavoriteVideo>)
private data class UndoBatch(val message: String, val items: List<FavoriteVideo>)
private data class TextSheetData(
    val title: String,
    val subtitle: String? = null,
    val lines: List<String>,
    val highlightIndex: Int = 0
)

private data class LocalPrayerFavoriteItem(
    val id: String,
    val title: String,
    val text: String,
    val url: String?
)

private fun loadLocalPrayerFavorites(context: android.content.Context): List<LocalPrayerFavoriteItem> {
    val raw = context.resources.openRawResource(com.youtube.rating.core.data.R.raw.molitve)
        .bufferedReader()
        .use { it.readText() }
    val json = JSONArray(raw)
    return buildList {
        for (i in 0 until json.length()) {
            val obj = json.optJSONObject(i) ?: continue
            val title = obj.optString("title").trim()
            val text = obj.optString("text").trim()
            if (title.isBlank() || text.isBlank()) continue
            val id = obj.opt("psalm")?.toString()?.trim().orEmpty()
            if (id.isBlank()) continue
            add(
                LocalPrayerFavoriteItem(
                    id = id,
                    title = title,
                    text = text,
                    url = obj.optString("url").takeIf { it.isNotBlank() }
                )
            )
        }
    }
}

@Composable
fun FavoritesScreen(
    onOpenOfflineGallery: () -> Unit = {}, // ✅ klik na slike vodi u Offline/Galerija
    videoDetailsContent: @Composable (FavoriteVideo, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val spacing = MaterialTheme.spacing
    val deps = rememberFavoritesScreenDependencies()

    val favoritesGateway = deps.favoritesGateway
    val offlineRepository = deps.offlineRepository
    val apiClient = deps.apiClient
    val favorites by favoritesGateway.favoritesFlow.collectAsStateWithLifecycle()
    val psalmHighlightTokens by PsalmPrefs.highlightedPsalmLinesFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val biblePassageCache by BibleReaderPrefs.biblePassageCacheFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    val savedLocalPrayerIds by PrayerPrefs.savedLocalPrayersFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val localPrayerPool by produceState<List<LocalPrayerFavoriteItem>>(initialValue = emptyList()) {
        makeIOCall {
            value = runCatching { loadLocalPrayerFavorites(context) }.getOrDefault(emptyList())
        }
    }
    val favoriteLocalPrayers by remember(savedLocalPrayerIds, localPrayerPool) {
        derivedStateOf {
            if (savedLocalPrayerIds.isEmpty() || localPrayerPool.isEmpty()) emptyList()
            else {
                val map = localPrayerPool.associateBy { it.id }
                savedLocalPrayerIds.mapNotNull { map[it] }
                    .sortedBy { it.title.lowercase(Locale.getDefault()) }
            }
        }
    }

    val offlineVideos by offlineRepository.getAllVideosFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val offlineIds by remember(offlineVideos) {
        derivedStateOf { offlineVideos.mapNotNull { it.youtubeId }.toHashSet() }
    }

    // ✅ odvoji slike od ostalih favorita
    val galleryFavorites by remember(favorites) { derivedStateOf { favorites.filter { it.isImage() } } }
    val mainFavorites by remember(favorites) { derivedStateOf { favorites.filterNot { it.isImage() } } }

    var uiState by rememberSaveable { mutableStateOf(FavoritesContract.State()) }
    val dispatch: (FavoritesContract.Intent) -> Unit = remember {
        { intent -> uiState = FavoritesContract.reduce(uiState, intent) }
    }
    val query = uiState.query
    val sortBy = uiState.sortBy
    val groupMode = uiState.groupMode
    val collapsedSet = uiState.collapsedSet

    var showVideoDetailsDialog by remember { mutableStateOf<FavoriteVideo?>(null) }
    var showTextSheet by remember { mutableStateOf<TextSheetData?>(null) }
    var pendingPsalmOpen by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val psalmsRepo = remember { PsalmsRepository(context) }
    val psalmsList by produceState<List<PsalmDto>?>(initialValue = null) {
        makeIOCall {
            value = psalmsRepo.loadPsalms()
        }
    }
    val scope = rememberCoroutineScope()

    val psalmHighlightsMap by remember(psalmHighlightTokens) {
        derivedStateOf {
            buildMap<Int, MutableSet<Int>> {
                psalmHighlightTokens.forEach { token ->
                    val parts = token.split(":")
                    val p = parts.getOrNull(0)?.toIntOrNull()
                    val line = parts.getOrNull(1)?.toIntOrNull()
                    multiLet(p, line) { psalm, ln ->
                        getOrPut(psalm) { mutableSetOf() }.add(ln)
                    }
                }
            }
        }
    }

    fun groupKeyFor(video: FavoriteVideo): String {
        if (video.isPsalm()) {
            val psalmNumber = video.videoId.removePrefix("psalm:").toIntOrNull()
            val hasHighlights = psalmNumber != null && (psalmHighlightsMap[psalmNumber]?.isNotEmpty() == true)
            return if (hasHighlights) {
                GROUP_PSALMS_HIGHLIGHTED
            } else {
                GROUP_PSALMS
            }
        }
        return video.groupKey()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var lastDeletedSingle by remember { mutableStateOf<FavoriteVideo?>(null) }
    var lastDeletedBatch by remember { mutableStateOf<UndoBatch?>(null) }

    val shownFavorites by remember(mainFavorites, query, sortBy) {
        derivedStateOf {
            val q = query.trim().lowercase(Locale.getDefault())
            val filtered = if (q.isEmpty()) mainFavorites else mainFavorites.filter {
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                        it.channelName.lowercase(Locale.getDefault()).contains(q)
            }

            when (sortBy) {
                SortMode.NEWEST -> filtered.sortedByDescending { it.timestamp }
                SortMode.OLDEST -> filtered.sortedBy { it.timestamp }
                SortMode.BEST -> filtered.sortedByDescending { (it.avgLove + it.avgFaith + it.avgHope) / 3.0 }
            }
        }
    }

    val groups: List<GroupBlock> by remember(shownFavorites, groupMode) {
        derivedStateOf {
            if (groupMode == GroupMode.NONE) {
                listOf(GroupBlock(key = "", items = shownFavorites))
            } else {
                val map = LinkedHashMap<String, MutableList<FavoriteVideo>>()
                for (v in shownFavorites) {
                    val k = groupKeyFor(v)
                    map.getOrPut(k) { mutableListOf() }.add(v)
                }

                val keys = map.keys.toList()
                val orderedKeys = keys.sortedWith(
                    compareBy<String> { k ->
                        when {
                            k == GROUP_PSALMS_HIGHLIGHTED -> 0
                            k == GROUP_PSALMS -> 1
                            k == GROUP_BIBLE -> 2
                            isProtectedGroup() -> 2
                            else -> 3
                        }
                    }.thenBy { it.lowercase(Locale.getDefault()) }
                )

                orderedKeys.map { k -> GroupBlock(k, map[k].orEmpty()) }
            }
        }
    }

    LaunchedEffect(pendingPsalmOpen, psalmsList) {
        val target = pendingPsalmOpen ?: return@LaunchedEffect
        val list = psalmsList ?: return@LaunchedEffect
        val (psalmNumber, lineIndex) = target
        val psalm = list.firstOrNull { it.psalm == psalmNumber } ?: return@LaunchedEffect
        val lines = psalm.text
            .trim()
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
        showTextSheet = TextSheetData(
            title = "Psalam ${psalm.psalm} - ${psalm.title}",
            subtitle = psalm.url,
            lines = lines,
            highlightIndex = lineIndex.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
        )
        pendingPsalmOpen = null
    }

    val clearAllHasProtected by remember(mainFavorites) {
        derivedStateOf { mainFavorites.any { isProtectedGroup() } }
    }

    fun toggleCollapsed(key: String) {
        dispatch(FavoritesContract.Intent.ToggleGroupCollapsed(key))
    }

    fun openConfirm(kind: FavoritesContract.DeleteKind, key: String, protected: Boolean) {
        dispatch(FavoritesContract.Intent.OpenDeleteConfirmation(kind, key, protected))
    }

    fun closeConfirm() {
        dispatch(FavoritesContract.Intent.CloseDeleteConfirmation)
    }

    fun runDeleteSingle(videoId: String) {
        val video = favorites.find { it.videoId == videoId }
        favoritesGateway.removeFavorite(videoId)
        if (video != null) lastDeletedSingle = video
    }

    fun runDeleteGroup(groupKey: String) {
        val items = mainFavorites.filter { it.groupKey() == groupKey }
        if (items.isEmpty()) return
        items.forEach { favoritesGateway.removeFavorite(it.videoId) }
        lastDeletedBatch = UndoBatch(
            message = "Uklonjeno: ${items.size} iz \"$groupKey\"",
            items = items
        )
    }

    fun runClearAllMainFavorites() {
        val snapshot = mainFavorites.toList()
        if (snapshot.isEmpty()) return
        snapshot.forEach { favoritesGateway.removeFavorite(it.videoId) }
        lastDeletedBatch = UndoBatch(
            message = "Uklonjeno: ${snapshot.size}",
            items = snapshot
        )
    }

    // Undo single
    LaunchedEffect(lastDeletedSingle) {
        val v = lastDeletedSingle ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "${v.title} uklonjeno",
            actionLabel = "VRATI",
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            favoritesGateway.addFavorite(v)
            haptic.success()
        }
        lastDeletedSingle = null
    }

    // Undo batch
    LaunchedEffect(lastDeletedBatch) {
        val batch = lastDeletedBatch ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = batch.message,
            actionLabel = "VRATI",
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            batch.items.forEach { favoritesGateway.addFavorite(it) }
            haptic.success()
        }
        lastDeletedBatch = null
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    action = {
                        val label = data.visuals.actionLabel ?: "VRATI"
                        TextButton(onClick = { data.performAction() }) { Text(label) }
                    },
                    dismissAction = {
                        IconButton(onClick = { data.dismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Zatvori")
                        }
                    }
                ) { Text(data.visuals.message) }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                favorites.isEmpty() && favoriteLocalPrayers.isEmpty() -> FavoritesEmptyState(modifier = Modifier.padding(spacing.lg))

                mainFavorites.isEmpty() && galleryFavorites.isNotEmpty() && favoriteLocalPrayers.isEmpty() -> FavoritesOnlyGalleryState(
                    galleryCount = galleryFavorites.size,
                    onOpenGallery = onOpenOfflineGallery,
                    modifier = Modifier.padding(spacing.lg)
                )

                shownFavorites.isEmpty() && query.isNotBlank() -> EmptySearchState(modifier = Modifier.padding(spacing.lg))

                else -> {
                    val dragListState = rememberLazyListState()
                    val localFavorites = remember { mutableStateListOf<FavoriteVideo>() }
                    val dragEnabled = groupMode == GroupMode.NONE && query.isBlank() && galleryFavorites.isEmpty()
                    var draggingIndex by remember { mutableStateOf<Int?>(null) }
                    var dragStartPointer by remember { mutableStateOf(Offset.Zero) }
                    var currentPointer by remember { mutableStateOf(Offset.Zero) }
                    var dragItemStartOffset by remember { mutableStateOf(IntOffset.Zero) }

                    LaunchedEffect(shownFavorites, dragEnabled) {
                        if (dragEnabled) {
                            localFavorites.clear()
                            localFavorites.addAll(shownFavorites)
                        }
                    }

                    fun findItemIndexAt(position: Offset): Int? {
                        dragListState.layoutInfo.visibleItemsInfo.forEach { info ->
                            val top = info.offset.toFloat()
                            val bottom = (info.offset + info.size).toFloat()
                            if (position.y in top..bottom) return info.index
                        }
                        return null
                    }

                    LazyColumn(
                        state = dragListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (dragEnabled) {
                                    Modifier.pointerInput(localFavorites) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                dragStartPointer = offset
                                                currentPointer = offset
                                                draggingIndex = findItemIndexAt(offset)
                                                haptic.lightTap()
                                                dragItemStartOffset = draggingIndex?.let { index ->
                                                    val info = dragListState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == index }
                                                    IntOffset(0, info?.offset ?: 0)
                                                } ?: IntOffset.Zero
                                            },
                                            onDrag = { _, dragAmount ->
                                                currentPointer += dragAmount
                                                val fromIndex = draggingIndex
                                                if (fromIndex != null) {
                                                    val overIndex = findItemIndexAt(currentPointer)
                                                    if (overIndex != null && overIndex != fromIndex &&
                                                        overIndex in localFavorites.indices && fromIndex in localFavorites.indices
                                                    ) {
                                                        val moved = localFavorites.removeAt(fromIndex)
                                                        localFavorites.add(overIndex, moved)
                                                        haptic.lightTap()
                                                        draggingIndex = overIndex
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggingIndex = null
                                                scope.launch { favoritesGateway.reorderFavoritesSuspend(localFavorites.toList()) }
                                            },
                                            onDragCancel = { draggingIndex = null }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {

                        // ✅ GALERIJA BLOK (bolji: hero + strip, bez Card grid-a za svaku sliku)
                        if (galleryFavorites.isNotEmpty() && query.isBlank()) {
                            item(key = "gallery_block") {
                                FavoritesGalleryBlockV2(
                                    images = galleryFavorites,
                                    onOpenGallery = onOpenOfflineGallery
                                )
                            }
                        }

                        if (favoriteLocalPrayers.isNotEmpty()) {
                            stickyHeader(key = "header_local_prayers") {
                                GroupHeaderV2(
                                    title = GROUP_LOCAL_PRAYERS,
                                    count = favoriteLocalPrayers.size,
                                    protected = false,
                                    collapsed = false,
                                    onToggleCollapse = {},
                                    onDeleteGroup = null
                                )
                            }

                            items(
                                items = favoriteLocalPrayers,
                                key = { "local_prayer_" + it.id },
                                contentType = { "local_prayer" }
                            ) { prayer ->
                                FavoriteLocalPrayerCard(
                                    title = prayer.title,
                                    subtitle = prayer.url,
                                    onOpen = {
                                        showTextSheet = TextSheetData(
                                            title = prayer.title,
                                            subtitle = prayer.url,
                                            lines = prayer.text
                                                .replace("\r\n", "\n")
                                                .split("\n")
                                                .map { it.trimEnd() }
                                                .filter { it.isNotBlank() },
                                            highlightIndex = 0
                                        )
                                    },
                                    onRemove = {
                                        scope.launch {
                                            PrayerPrefs.toggleSavedLocalPrayer(context, prayer.id)
                                        }
                                    }
                                )
                            }
                        }

                        // ✅ ostali favoriti (video + psalmi)
                        if (shownFavorites.isNotEmpty()) {

                            if (groupMode == GroupMode.NONE) {
                                val list = if (dragEnabled) localFavorites else shownFavorites
                                items(
                                    items = list,
                                    key = { it.videoId },
                                    contentType = { "favorite_video" }
                                ) { video ->
                                    val isDragging = dragEnabled && draggingIndex == list.indexOf(video)
                                    val dragScale by animateFloatAsState(
                                        targetValue = if (isDragging) 1.02f else 1f,
                                        label = "favoriteDragScale"
                                    )
                                    val dragElevation by animateDpAsState(
                                        targetValue = if (isDragging) 10.dp else 2.dp,
                                        label = "favoriteDragElevation"
                                    )
                                    val isOffline = remember(video.videoId, offlineIds) {
                                        !video.isPsalm() && offlineIds.contains(video.videoId)
                                    }
                                    val psalmHighlightedText = remember(video.videoId, psalmsList, psalmHighlightsMap) {
                                        if (video.type == FavoriteItemType.PSALM_HIGHLIGHTED) {
                                            val psalmNumber = video.videoId.removePrefix("psalm:").toIntOrNull()
                                            val lineIdx = psalmNumber?.let { psalmHighlightsMap[it]?.firstOrNull() } ?: 0
                                            val psalm = psalmNumber?.let { num -> psalmsList?.firstOrNull { it.psalm == num } }
                                            val lines = psalm?.text
                                                ?.trim()
                                                ?.replace("\r\n", "\n")
                                                ?.split("\n")
                                                ?.map { it.trimEnd() }
                                                ?.filter { it.isNotBlank() }
                                                .orEmpty()
                                            lines.getOrNull(lineIdx)?.takeIf { it.isNotBlank() } ?: video.title
                                        } else {
                                            null
                                        }
                                    }

                                    SwipeToDismissFavoriteCard(
                                        video = video,
                                        isOffline = isOffline,
                                        enabledRemove = !uiState.confirmation.open,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = dragScale
                                                scaleY = dragScale
                                            }
                                            .animateContentSize(),
                                        elevation = dragElevation,
                                        onRequestRemove = {
                                            if (uiState.confirmation.open) return@SwipeToDismissFavoriteCard
                                            haptic.lightTap()
                                            val g = video.groupKey()
                                            openConfirm(FavoritesContract.DeleteKind.SINGLE, video.videoId, isProtectedGroup())
                                        },
                                        onClick = {
                                            haptic.click()
                                            if (video.isBibleHighlight()) {
                                                val contentKey = video.videoId.substringBeforeLast(":")
                                                val backupText = favorites.firstOrNull {
                                                    it.videoId.startsWith("$contentKey:") && it.thumbnail.isNotBlank()
                                                }?.thumbnail.orEmpty()
                                                val cachedText = biblePassageCache[contentKey].orEmpty()
                                                var fullText = (video.thumbnail.ifBlank { backupText }
                                                    .ifBlank { cachedText }
                                                    .ifBlank { video.title })
                                                    .replace("\r\n", "\n")
                                                val canFetch = contentKey.startsWith("bible:") &&
                                                    contentKey.removePrefix("bible:").contains("/")
                                                val needFetch = fullText == video.title && canFetch
                                                if (needFetch) {
                                                    scope.launch {
                                                        val keyBody = contentKey.removePrefix("bible:")
                                                        val parts = keyBody.split("/")
                                                        val book = parts.getOrNull(0).orEmpty()
                                                        val chapter = parts.getOrNull(1).orEmpty()
                                                        if (book.isNotBlank() && chapter.isNotBlank()) {
                                                            val resolvedBook = try {
                                                                LocalBibleRepository.resolveBookId(context, book)
                                                            } catch (e: Exception) {
                                                                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                                                                book
                                                            }
                                                            val resp = runCatching { apiClient.getBiblePassage(resolvedBook, chapter) }.getOrNull()
                                                            val text = resp?.text?.takeIf { it.isNotBlank() }
                                                            if (text != null) {
                                                                BibleReaderPrefs.putBiblePassageCache(context, contentKey, text)
                                                                favoritesGateway.removeFavoriteSuspend(video.videoId)
                                                                favoritesGateway.addFavoriteSuspend(video.copy(thumbnail = text))
                                                                val linesFetched = text.split("\n")
                                                                    .map { it.trim() }
                                                                    .filter { it.isNotBlank() }
                                                                val idxFetched = video.videoId.substringAfterLast(":")
                                                                    .toIntOrNull()
                                                                    ?.coerceIn(0, (linesFetched.size - 1).coerceAtLeast(0))
                                                                    ?: 0
                                                                showTextSheet = TextSheetData(
                                                                    title = video.channelName.ifBlank { "Biblija" },
                                                                    subtitle = null,
                                                                    lines = linesFetched,
                                                                    highlightIndex = idxFetched
                                                                )
                                                            } else {
                                                                showTextSheet = TextSheetData(
                                                                    title = video.channelName.ifBlank { "Biblija" },
                                                                    subtitle = null,
                                                                    lines = listOf(video.title),
                                                                    highlightIndex = 0
                                                                )
                                                            }
                                                        }
                                                    }
                                                    return@SwipeToDismissFavoriteCard
                                                }
                                                val lines = fullText
                                                    .split("\n")
                                                    .map { it.trim() }
                                                    .filter { it.isNotBlank() }
                                                    .ifEmpty { listOf(video.title) }
                                                val idx = video.videoId.substringAfterLast(":")
                                                    .toIntOrNull()
                                                    ?.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
                                                    ?: 0
                                                if (video.thumbnail.isBlank() && fullText != video.title) {
                                                    scope.launch {
                                                        favoritesGateway.removeFavoriteSuspend(video.videoId)
                                                        favoritesGateway.addFavoriteSuspend(video.copy(thumbnail = fullText))
                                                    }
                                                }
                                                showTextSheet = TextSheetData(
                                                    title = video.channelName.ifBlank { "Biblija" },
                                                    subtitle = null,
                                                    lines = lines,
                                                    highlightIndex = idx
                                                )
                                            } else if (video.videoId.startsWith("psalm:")) {
                                                val psalmNumber = video.videoId.removePrefix("psalm:").toIntOrNull()
                                                if (psalmNumber != null) {
                                                    val line = psalmHighlightsMap[psalmNumber]?.firstOrNull() ?: 0
                                                    pendingPsalmOpen = psalmNumber to line
                                                }
                                            } else {
                                                showVideoDetailsDialog = video
                                            }
                                        },
                                        onLongClick = null,
                                        highlightTextOverride = psalmHighlightedText
                                    )
                                }
                            } else {
                                // ✅ sticky headeri + animirani collapse
                                for (block in groups) {
                                    val groupKey = block.key
                                    val itemsInGroup = block.items
                                    val collapsed = collapsedSet.contains(groupKey)
                                    val protected = isProtectedGroup()

                                    stickyHeader(key = "header_$groupKey") {
                                        GroupHeaderV2(
                                            title = groupKey,
                                            count = itemsInGroup.size,
                                            protected = protected,
                                            collapsed = collapsed,
                                            onToggleCollapse = { toggleCollapsed(groupKey) },
                                            onDeleteGroup = if (itemsInGroup.isNotEmpty()) (fun() {
                                                if (uiState.confirmation.open) return
                                                haptic.lightTap()
                                                openConfirm(FavoritesContract.DeleteKind.GROUP, groupKey, protected)
                                            }) else null
                                        )
                                    }

                                    item(key = "body_$groupKey") {
                                        AnimatedVisibility(
                                            visible = !collapsed,
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateContentSize(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Spacer(Modifier.height(2.dp))
                                                itemsInGroup.forEach { video ->
                                                    val isOffline = remember(video.videoId, offlineIds) {
                                                        !video.isPsalm() && offlineIds.contains(video.videoId)
                                                    }
                                                    val psalmHighlightedText = remember(video.videoId, psalmsList, psalmHighlightsMap) {
                                                        if (video.type == FavoriteItemType.PSALM_HIGHLIGHTED) {
                                                            val psalmNumber = video.videoId.removePrefix("psalm:").toIntOrNull()
                                                            val lineIdx = psalmNumber?.let { psalmHighlightsMap[it]?.firstOrNull() } ?: 0
                                                            val psalm = psalmNumber?.let { num -> psalmsList?.firstOrNull { it.psalm == num } }
                                                            val lines = psalm?.text
                                                                ?.trim()
                                                                ?.replace("\r\n", "\n")
                                                                ?.split("\n")
                                                                ?.map { it.trimEnd() }
                                                                ?.filter { it.isNotBlank() }
                                                                .orEmpty()
                                                            lines.getOrNull(lineIdx)?.takeIf { it.isNotBlank() } ?: video.title
                                                        } else {
                                                            null
                                                        }
                                                    }

                                                    SwipeToDismissFavoriteCard(
                                                        video = video,
                                                        isOffline = isOffline,
                                                        enabledRemove = !uiState.confirmation.open,
                                                        onRequestRemove = {
                                                            if (uiState.confirmation.open) return@SwipeToDismissFavoriteCard
                                                            haptic.lightTap()
                                                            openConfirm(FavoritesContract.DeleteKind.SINGLE, video.videoId, protected)
                                                        },
                                                        onClick = {
                                                            haptic.click()
                                                            if (video.isBibleHighlight()) {
                                                                val contentKey = video.videoId.substringBeforeLast(":")
                                                                val backupText = favorites.firstOrNull {
                                                                    it.videoId.startsWith("$contentKey:") && it.thumbnail.isNotBlank()
                                                                }?.thumbnail.orEmpty()
                                                                val cachedText = biblePassageCache[contentKey].orEmpty()
                                                                var fullText = (video.thumbnail.ifBlank { backupText }
                                                                    .ifBlank { cachedText }
                                                                    .ifBlank { video.title })
                                                                    .replace("\r\n", "\n")
                                                                val canFetch = contentKey.startsWith("bible:") &&
                                                                    contentKey.removePrefix("bible:").contains("/")
                                                                val needFetch = fullText == video.title && canFetch
                                                                if (needFetch) {
                                                                    scope.launch {
                                                                        val keyBody = contentKey.removePrefix("bible:")
                                                                        val parts = keyBody.split("/")
                                                                        val book = parts.getOrNull(0).orEmpty()
                                                                        val chapter = parts.getOrNull(1).orEmpty()
                                                                        if (book.isNotBlank() && chapter.isNotBlank()) {
                                                                            val resolvedBook = try {
                                                                                LocalBibleRepository.resolveBookId(context, book)
                                                                            } catch (e: Exception) {
                                                                                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                                                                                book
                                                                            }
                                                                            val resp = runCatching { apiClient.getBiblePassage(resolvedBook, chapter) }.getOrNull()
                                                                            val text = resp?.text?.takeIf { it.isNotBlank() }
                                                                            if (text != null) {
                                                                                BibleReaderPrefs.putBiblePassageCache(context, contentKey, text)
                                                                                favoritesGateway.removeFavoriteSuspend(video.videoId)
                                                                                favoritesGateway.addFavoriteSuspend(video.copy(thumbnail = text))
                                                                                val linesFetched = text.split("\n")
                                                                                    .map { it.trim() }
                                                                                    .filter { it.isNotBlank() }
                                                                                val idxFetched = video.videoId.substringAfterLast(":")
                                                                                    .toIntOrNull()
                                                                                    ?.coerceIn(0, (linesFetched.size - 1).coerceAtLeast(0))
                                                                                    ?: 0
                                                                                showTextSheet = TextSheetData(
                                                                                    title = video.channelName.ifBlank { "Biblija" },
                                                                                    subtitle = null,
                                                                                    lines = linesFetched,
                                                                                    highlightIndex = idxFetched
                                                                                )
                                                                            } else {
                                                                                showTextSheet = TextSheetData(
                                                                                    title = video.channelName.ifBlank { "Biblija" },
                                                                                    subtitle = null,
                                                                                    lines = listOf(video.title),
                                                                                    highlightIndex = 0
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    return@SwipeToDismissFavoriteCard
                                                                }
                                                                val lines = fullText
                                                                    .split("\n")
                                                                    .map { it.trim() }
                                                                    .filter { it.isNotBlank() }
                                                                    .ifEmpty { listOf(video.title) }
                                                                val idx = video.videoId.substringAfterLast(":")
                                                                    .toIntOrNull()
                                                                    ?.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
                                                                    ?: 0
                                                                if (video.thumbnail.isBlank() && fullText != video.title) {
                                                                    scope.launch {
                                                                        favoritesGateway.removeFavoriteSuspend(video.videoId)
                                                                        favoritesGateway.addFavoriteSuspend(video.copy(thumbnail = fullText))
                                                                    }
                                                                }
                                                                showTextSheet = TextSheetData(
                                                                    title = video.channelName.ifBlank { "Biblija" },
                                                                    subtitle = null,
                                                                    lines = lines,
                                                                    highlightIndex = idx
                                                                )
                                                            } else if (video.videoId.startsWith("psalm:")) {
                                                                val psalmNumber = video.videoId.removePrefix("psalm:").toIntOrNull()
                                                                if (psalmNumber != null) {
                                                                    val line = psalmHighlightsMap[psalmNumber]?.firstOrNull() ?: 0
                                                                    pendingPsalmOpen = psalmNumber to line
                                                                }
                                                            } else {
                                                                showVideoDetailsDialog = video
                                                            }
                                                        },
                                                        onLongClick = null,
                                                        highlightTextOverride = psalmHighlightedText
                                                    )
                                                }
                                                Spacer(Modifier.height(6.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // dialogs
        showVideoDetailsDialog?.let { video ->
            videoDetailsContent(video) { showVideoDetailsDialog = null }
        }

        showTextSheet?.let { data ->
            TextHighlightSheet(
                title = data.title,
                subtitle = data.subtitle,
                lines = data.lines,
                highlightIndex = data.highlightIndex,
                onDismiss = { showTextSheet = null }
            )
        }

        // ✅ confirm dialog (1 ili 3 + typed word)
        if (uiState.confirmation.open) {
            val confirmation = uiState.confirmation
            val step = confirmation.step
            val total = confirmation.total
            val badge = if (total > 1) " ($step/$total)" else ""
            val finalStep = confirmation.isFinalStep
            val needsWord = confirmation.needsWord && finalStep
            val wordOk = confirmation.word.trim().equals(FINAL_DELETE_WORD, ignoreCase = true)

            val (title, body, confirmLabel) = when (confirmation.kind) {
                FavoritesContract.DeleteKind.SINGLE -> {
                    val video = mainFavorites.find { it.videoId == confirmation.key }
                    if (video == null) {
                        Triple("Stavka nije pronađena", "Možda je već obrisana.", "Zatvori")
                    } else {
                        val group = video.groupKey()
                        val protected = isProtectedGroup()
                        Triple(
                            "Ukloniti iz spremljenih?$badge",
                            buildString {
                                if (protected) append("Zaštićena grupa: \"$group\".\n\n")
                                append(video.title)
                            },
                            if (total == 1) "Ukloni" else if (!finalStep) "Nastavi" else "Ukloni"
                        )
                    }
                }

                FavoritesContract.DeleteKind.GROUP -> {
                    val groupKey = confirmation.key
                    val count = mainFavorites.count { it.groupKey() == groupKey }
                    val protected = isProtectedGroup()
                    Triple(
                        "Obrisati grupu \"$groupKey\"?$badge",
                        buildString {
                            if (protected) append("Ovo je zaštićena grupa.\n\n")
                            append("Uklonit će se $count stavki.")
                        },
                        if (total == 1) "Obriši" else if (!finalStep) "Nastavi" else "Obriši"
                    )
                }

                else -> {
                    Triple(
                        "Obrisati sve spremljene?$badge",
                        buildString {
                            if (confirmation.needsWord) append("Postoje zaštićene grupe.\n\n")
                            append("Ukupno: ${mainFavorites.size}")
                        },
                        if (total == 1) "Obriši sve" else if (!finalStep) "Nastavi" else "Obriši sve"
                    )
                }
            }

            AlertDialog(
                onDismissRequest = { closeConfirm() },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title)
                        if (total > 1) {
                            Text(
                                text = when (step) {
                                    1 -> "Prva potvrda."
                                    2 -> "Druga potvrda – jesi li siguran?"
                                    else -> "Treća potvrda – zadnja potvrda."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(body)

                        if (needsWord) {
                            Text(
                                "Upiši \"$FINAL_DELETE_WORD\" da potvrdiš brisanje.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = confirmation.word,
                                onValueChange = {
                                    dispatch(FavoritesContract.Intent.DeleteConfirmationWordChanged(it))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !needsWord || wordOk,
                        onClick = {
                            if (confirmation.kind == FavoritesContract.DeleteKind.SINGLE &&
                                mainFavorites.none { it.videoId == confirmation.key }
                            ) {
                                closeConfirm(); return@TextButton
                            }
                            if (step < total) {
                                haptic.lightTap()
                                dispatch(FavoritesContract.Intent.AdvanceDeleteConfirmation)
                                return@TextButton
                            }

                            haptic.strongFeedback()
                            when (confirmation.kind) {
                                FavoritesContract.DeleteKind.SINGLE -> runDeleteSingle(confirmation.key)
                                FavoritesContract.DeleteKind.GROUP -> runDeleteGroup(confirmation.key)
                                FavoritesContract.DeleteKind.CLEAR_ALL -> runClearAllMainFavorites()
                            }
                            closeConfirm()
                        }
                    ) { Text(confirmLabel) }
                },
                dismissButton = {
                    Row {
                        if (total > 1 && step > 1) {
                            TextButton(
                                onClick = {
                                    haptic.lightTap()
                                    dispatch(FavoritesContract.Intent.BackDeleteConfirmation)
                                }
                            ) { Text(Strings.back) }

                            Spacer(Modifier.width(8.dp))
                        }
                        TextButton(onClick = { closeConfirm() }) { Text(Strings.cancel) }
                    }
                }
            )
        }
    }
}

/* ---------------------------
   GALERIJA BLOK (V2) – hero + strip, bez pojedinačnih Card-ova
   --------------------------- */

@Composable
private fun FavoritesGalleryBlockV2(
    images: List<FavoriteVideo>,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    val hero = images.firstOrNull()
    val count = images.size
    val spacing = MaterialTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenGallery),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(spacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(Strings.galleryTitle, fontWeight = FontWeight.SemiBold)
                        Text(
                            "$count slika • otvori u Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                // hero preview (jedna veća)
                if (hero != null) {
                    val heroImageRequest = remember(context, hero.thumbnail) {
                        ImageRequest.Builder(context)
                            .data(hero.thumbnail)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .size(900)
                            .build()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        MediaThumbnail(
                            data = heroImageRequest,
                            contentDescription = hero.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), shape = RoundedCornerShape(999.dp))
                                .padding(horizontal = spacing.sm, vertical = spacing.xs)
                        ) {
                            Text(
                                text = "Otvori Galeriju",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        FavoritesGalleryStripV2(
            images = images,
            onClick = onOpenGallery
        )
    }
}

@Composable
private fun FavoritesGalleryStripV2(
    images: List<FavoriteVideo>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val maxThumbs = 14
    val shown = images.take(maxThumbs)
    val remaining = (images.size - shown.size).coerceAtLeast(0)
    val spacing = MaterialTheme.spacing

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        contentPadding = PaddingValues(horizontal = spacing.xs, vertical = spacing.xs)
    ) {
        items(
            items = shown,
            key = { it.videoId },
            contentType = { "gallery_thumb" }
        ) { img ->
            val thumbRequest = remember(context, img.thumbnail) {
                ImageRequest.Builder(context)
                    .data(img.thumbnail)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .size(250)
                    .build()
            }
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onClick)
            ) {
                MediaThumbnail(
                    data = thumbRequest,
                    contentDescription = img.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (remaining > 0) {
            item(key = "more") {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$remaining",
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/* ---------------------------
   GROUP HEADER (V2) – sticky, malo “čistiji”
   --------------------------- */

@Composable
private fun GroupHeaderV2(
    title: String,
    count: Int,
    protected: Boolean,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onDeleteGroup: (() -> Unit)?
) {
    val spacing = MaterialTheme.spacing
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                protected -> {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(spacing.xs))
                }
                title == GROUP_PSALMS -> {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(spacing.xs))
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            AssistChip(
                onClick = {},
                modifier = Modifier.height(26.dp),
                label = { Text("$count", style = MaterialTheme.typography.labelSmall) }
            )

            Spacer(Modifier.width(spacing.xs))

            IconButton(onClick = onToggleCollapse) {
                Icon(
                    if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null
                )
            }

            if (onDeleteGroup != null) {
                IconButton(onClick = onDeleteGroup) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun FavoriteLocalPrayerCard(
    title: String,
    subtitle: String?,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Ukloni iz favorita",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/* ---------------------------
   SWIPE + VIDEO CARD (videos only)
   --------------------------- */

@Composable
fun SwipeToDismissFavoriteCard(
    video: FavoriteVideo,
    isOffline: Boolean,
    enabledRemove: Boolean,
    modifier: Modifier = Modifier,
    elevation: Dp = 1.dp,
    onRequestRemove: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    highlightTextOverride: String? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.35f }
    )

    var fired by remember(video.videoId) { mutableStateOf(false) }

    LaunchedEffect(dismissState.currentValue, enabledRemove) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                if (enabledRemove && !fired) {
                    fired = true
                    onRequestRemove()
                }
                dismissState.reset()
            }
            SwipeToDismissBoxValue.Settled -> fired = false
            else -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        content = {
            FavoriteVideoCard(
                video = video,
                isOffline = isOffline,
                modifier = modifier,
                elevation = elevation,
                onClick = onClick,
                onLongClick = onLongClick,
                highlightTextOverride = highlightTextOverride
            )
        }
    )
}

@Composable
private fun FavoriteVideoCard(
    video: FavoriteVideo,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier,
    elevation: Dp = 1.dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    highlightTextOverride: String? = null
) {
    val context = LocalContext.current
    val isPsalm = video.isPsalm()
    val isBible = video.isBibleHighlight()
    val isPsalmHighlighted = video.type == FavoriteItemType.PSALM_HIGHLIGHTED

    if (isBible || isPsalmHighlighted) {
        SmallBibleHighlightCard(text = highlightTextOverride ?: video.title, onClick = onClick)
        return
    }
    val thumbnailUrl = remember(video.videoId, video.thumbnail) {
        video.thumbnail.ifEmpty { ThumbnailHelper.getThumbnailUrl(video.videoId) }
    }
    val thumbnailRequest = remember(context, thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .size(450)
            .build()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier.padding(if (isPsalm) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isPsalm) {
                Box(
                    modifier = Modifier
                        .size(width = 112.dp, height = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    MediaThumbnail(
                        data = thumbnailRequest,
                        contentDescription = video.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isOffline) {
                        AssistChip(
                            onClick = { },
                            label = { Text(Strings.offline, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Default.OfflinePin, contentDescription = null) },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .height(26.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (video.channelName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // ✅ ocjene samo za video
                if (!isPsalm) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiniRatingPill("❤️", video.avgLove)
                        MiniRatingPill("✝️", video.avgFaith)
                        MiniRatingPill("⭐", video.avgHope)
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MiniRatingPill(emoji: String, rating: Double, modifier: Modifier = Modifier) {
    val text = remember(rating) { "$emoji ${String.format(Locale.getDefault(), "%.1f", rating)}" }
    Surface(
        modifier = modifier.height(24.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
