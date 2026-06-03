package com.youtube.rating.android.ui.screens.prayer

import android.content.Intent
import android.net.Uri
import android.util.JsonReader
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.repository.Encouragement
import com.youtube.rating.android.repository.PrayerRequest
import com.youtube.rating.android.storage.FastingEntry
import com.youtube.rating.android.utils.AdminManager
import com.youtube.rating.android.viewmodel.PrayerViewModel
import kotlinx.coroutines.launch
import com.youtube.rating.core.coroutines.makeIOCall
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.localization.UiText
import com.youtube.rating.android.localization.asString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.prefs.BibleReaderPrefs
import com.youtube.rating.android.data.prefs.PrayerPrefs
import com.youtube.rating.core.designsystem.components.RatingCalloutCard
import com.youtube.rating.core.designsystem.components.RatingEmptyState

private const val INFINITE_SCROLL_THRESHOLD = 3

private data class LocalPrayerItem(
    val id: String,
    val title: String,
    val text: String,
    val url: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    viewModel: PrayerViewModel,
    adminManager: AdminManager,
    fastingEntries: Map<String, FastingEntry>,
    fastingWeeklyGoal: Int,
    onSaveFastingEntry: (FastingEntry) -> Unit,
    onUpdateFastingGoal: (Int) -> Unit,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {},
    onHapticLight: (() -> Unit)? = null,
    onHapticSuccess: (() -> Unit)? = null,
    fastingContent: @Composable () -> Unit = {},
    rosaryContent: @Composable () -> Unit = {}
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val isAdminMode by AdminPrefs.adminModeFlow(context)
        .collectAsStateWithLifecycle(initialValue = adminManager.isAdminMode())
    val favoriteLocalPrayerIds by PrayerPrefs.savedLocalPrayersFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val textScale by BibleReaderPrefs.bibleReaderTextScaleFlow(context)
        .collectAsStateWithLifecycle(initialValue = 1.0f)

    var selectedTab by rememberSaveable {
        mutableStateOf(PrayerPrefs.peekPrayerSelectedTab(0))
    }
    var tabStateHydrated by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showEncourageDialogFor by remember { mutableStateOf<PrayerRequest?>(null) }
    var deleteTarget by remember { mutableStateOf<PrayerRequest?>(null) }
    var confirmPrayedFor by remember { mutableStateOf<PrayerRequest?>(null) }
    var confirmPrayedForSecond by remember { mutableStateOf<PrayerRequest?>(null) }
    var pendingPrayedItem by remember { mutableStateOf<PrayerRequest?>(null) }
    var localPrayers by remember { mutableStateOf<List<LocalPrayerItem>>(emptyList()) }
    var localPrayersLoading by remember { mutableStateOf(true) }
    var localPrayersError by remember { mutableStateOf<String?>(null) }
    var selectedLocalPrayer by remember { mutableStateOf<LocalPrayerItem?>(null) }
    var localPrayerQuery by rememberSaveable { mutableStateOf("") }
    var showOnlyFavoritePrayers by rememberSaveable { mutableStateOf(false) }
    val reloadLocalPrayers: () -> Unit = {
        localPrayersLoading = true
        localPrayersError = null
        scope.makeIOCall(
            onCallExecuted = { localPrayersLoading = false },
            onErrorAction = { localPrayersError = it.message ?: Strings.error },
            ioCall = { loadLocalPrayers(context = context) },
            onCalled = {
                localPrayers = it
                localPrayersError = null
            }
        )
    }

    // Show toast for create errors
    LaunchedEffect(ui.createError) {
        ui.createError?.let { error ->
            if (BuildConfig.DEBUG) {
                android.widget.Toast.makeText(
                    context,
                    error.resolve(context),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            // Clear the error after showing toast (in debug) or just clear it (in release)
            viewModel.clearCreateError()
        }
    }

    // Show toast for add encouragement errors
    LaunchedEffect(ui.addEncouragementError) {
        ui.addEncouragementError?.let { error ->
            if (BuildConfig.DEBUG) {
                android.widget.Toast.makeText(
                    context,
                    error.resolve(context),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            // Clear the error after showing toast (in debug) or just clear it (in release)
            viewModel.clearAddEncouragementError()
        }
    }

    // initial load
    LaunchedEffect(Unit) {
        selectedTab = PrayerPrefs.getPrayerSelectedTab(context, selectedTab).coerceIn(0, 3)
        tabStateHydrated = true
        if (ui.items.isEmpty()) viewModel.refresh()
        reloadLocalPrayers()
    }

    LaunchedEffect(selectedTab, tabStateHydrated) {
        if (!tabStateHydrated) return@LaunchedEffect
        PrayerPrefs.setPrayerSelectedTab(context, selectedTab)
    }

    // infinite scroll
    val shouldLoadMore by remember(ui.items.size, ui.hasMore, listState) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= (ui.items.size - INFINITE_SCROLL_THRESHOLD).coerceAtLeast(0)
        }
    }
    LaunchedEffect(shouldLoadMore, ui.hasMore, ui.isLoadingMore, ui.isLoading) {
        if (shouldLoadMore && ui.hasMore && !ui.isLoadingMore && !ui.isLoading) viewModel.loadMore()
    }

    LaunchedEffect(pendingPrayedItem) {
        val item = pendingPrayedItem ?: return@LaunchedEffect
        pendingPrayedItem = null
        viewModel.togglePrayed(item)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {

                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(Strings.forOthers) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(Strings.prayer) }
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(Strings.fastingTitle) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text(Strings.rosaryTitle) }
                )
            }

            when (selectedTab) {
                0 -> {
                    PrayerTopBar(
                        isOnline = ui.isOnline,
                        onRefresh = { viewModel.refresh(force = true) },
                        onAdd = { showAddDialog = true }
                    )

                    com.youtube.rating.core.designsystem.components.PullToRefreshBox(
                        isRefreshing = ui.isLoading,
                        onRefresh = { viewModel.refresh(force = true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        when {
                            ui.isLoading && ui.items.isEmpty() && ui.error == null -> {
                                PrayerListSkeleton()
                            }

                            ui.error != null && ui.items.isEmpty() -> {
                                ErrorState(
                                    message = ui.error ?: UiText.Dynamic(Strings.error),
                                    onRetry = { viewModel.refresh(force = true) }
                                )
                            }

                            ui.items.isEmpty() -> {
                                EmptyState(onAdd = { showAddDialog = true })
                            }

                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 72.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = ui.items,
                                        key = { it.id },
                                        contentType = { "prayer_item" }
                                    ) { item ->
                                        PrayerCard(
                                            item = item,
                                            onPrayed = {
                                                // Confirm only when marking as prayed (not when unmarking).
                                                if (!item.iPrayed) {
                                                    confirmPrayedFor = item
                                                } else {
                                                    onHapticLight?.invoke()
                                                    viewModel.togglePrayed(item)
                                                }
                                            },
                                            onEncourage = { showEncourageDialogFor = item },
                                            onOpenDetails = { viewModel.openDetails(item) },
                                            isAdminMode = isAdminMode,
                                            onDelete = { deleteTarget = item }
                                        )
                                    }

                                    if (ui.isLoadingMore && ui.hasMore) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val filteredPrayers by remember(
                        localPrayers,
                        localPrayerQuery,
                        showOnlyFavoritePrayers,
                        favoriteLocalPrayerIds
                    ) {
                        derivedStateOf {
                            val q = localPrayerQuery.trim().lowercase()
                            val base = if (showOnlyFavoritePrayers) {
                                localPrayers.filter { favoriteLocalPrayerIds.contains(it.id) }
                            } else {
                                localPrayers
                            }
                            if (q.isBlank()) base
                            else base.filter {
                                it.title.lowercase().contains(q) || it.text.lowercase().contains(q)
                            }
                        }
                    }
                    when {
                        localPrayersLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        localPrayersError != null -> {
                            ErrorState(
                                message = UiText.Dynamic(localPrayersError ?: Strings.error),
                                onRetry = reloadLocalPrayers
                            )
                        }

                        localPrayers.isEmpty() -> {
                            EmptyState(onAdd = {})
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                OutlinedTextField(
                                    value = localPrayerQuery,
                                    onValueChange = { localPrayerQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (localPrayerQuery.isNotBlank()) {
                                            IconButton(onClick = { localPrayerQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Obriši")
                                            }
                                        }
                                    },
                                    label = { Text(Strings.searchPrayers) }
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !showOnlyFavoritePrayers,
                                        onClick = { showOnlyFavoritePrayers = false },
                                        label = { Text(Strings.all) }
                                    )
                                    FilterChip(
                                        selected = showOnlyFavoritePrayers,
                                        onClick = { showOnlyFavoritePrayers = true },
                                        label = { Text(Strings.favorites) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Favorite,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            if (filteredPrayers.isNotEmpty()) {
                                                selectedLocalPrayer = filteredPrayers.random()
                                            }
                                        },
                                        label = { Text(Strings.random) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Shuffle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        enabled = filteredPrayers.isNotEmpty()
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${filteredPrayers.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                if (filteredPrayers.isEmpty()) {
                                    EmptyLocalPrayerSearchState(
                                        hasQuery = localPrayerQuery.isNotBlank(),
                                        onReset = {
                                            localPrayerQuery = ""
                                            showOnlyFavoritePrayers = false
                                        }
                                    )
                                    return@Column
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(filteredPrayers, key = { it.id }) { prayer ->
                                        val isFavorite = favoriteLocalPrayerIds.contains(prayer.id)
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedLocalPrayer = prayer },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = prayer.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            scope.launch {
                                                                PrayerPrefs.toggleSavedLocalPrayer(context, prayer.id)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                            contentDescription = "Favorit",
                                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = prayer.text,
                                                    maxLines = 3,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    Box(modifier = Modifier.weight(1f)) {
                        fastingContent()
                    }
                }

                else -> {
                    Box(modifier = Modifier.weight(1f)) {
                        rosaryContent()
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // FAB (u skladu s tvojim stilom)
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = Strings.addNeed)
            }
        }

        if (selectedTab == 0) {
            if (showAddDialog) {
                AddPrayerDialog(
                    onDismiss = { showAddDialog = false },
                    onSubmit = { text, name ->
                        viewModel.createRequest(text, name)
                        showAddDialog = false
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                )
            }

            showEncourageDialogFor?.let { req ->
                AddEncouragementDialog(
                    title = req.text,
                    onDismiss = { showEncourageDialogFor = null },
                    onSubmit = { msg, name ->
                        viewModel.addEncouragement(req.id, msg, name)
                        showEncourageDialogFor = null
                    }
                )
            }

            deleteTarget?.let { req ->
                AlertDialog(
                    onDismissRequest = { deleteTarget = null },
                    title = { Text(Strings.deletePrayer) },
                    text = { Text(Strings.deletePrayerWarning) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Use secure token from AdminManager, fallback to hardcoded for now
                                val adminToken = adminManager.getAdminToken() ?: "admin_test_token_2024"
                                viewModel.deleteRequest(req, adminToken)
                                deleteTarget = null
                            }
                        ) {
                            Text(Strings.delete, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteTarget = null }) { Text(Strings.dismiss) }
                    }
                )
            }

            // Details bottom sheet
            ui.selectedRequest?.let { req ->
                PrayerDetailsSheet(
                    request = req,
                    encouragements = ui.encouragements,
                    isLoading = ui.isLoadingEncouragements,
                    textScale = textScale,
                    onDismiss = { viewModel.closeDetails() }
                )
            }

            confirmPrayedFor?.let { item ->
                AlertDialog(
                    onDismissRequest = { confirmPrayedFor = null },
                    title = { Text(Strings.didIPray) },
                    text = {
                        Text(
                            Strings.confirmPrayedMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmPrayedFor = null
                                confirmPrayedForSecond = item
                            }
                        ) { Text(Strings.confirm) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmPrayedFor = null }) { Text(Strings.dismiss) }
                    }
                )
            }

            confirmPrayedForSecond?.let { item ->
                AlertDialog(
                    onDismissRequest = { confirmPrayedForSecond = null },
                    title = { Text(Strings.areYouReallySure) },
                    text = {
                        Text(
                            Strings.confirmPrayedAgain,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                confirmPrayedForSecond = null
                                onHapticLight?.invoke()
                                pendingPrayedItem = item
                                onHapticSuccess?.invoke()
                            }
                        ) { Text(Strings.yesSure) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmPrayedForSecond = null }) { Text(Strings.dismiss) }
                    }
                )
            }
        }

        selectedLocalPrayer?.let { prayer ->
            val isFavorite = favoriteLocalPrayerIds.contains(prayer.id)
            ModalBottomSheet(
                onDismissRequest = { selectedLocalPrayer = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prayer.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                scope.launch { PrayerPrefs.toggleSavedLocalPrayer(context, prayer.id) }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                scope.launch {
                                    val next = if (textScale >= 1.6f) 0.8f else (textScale + 0.1f)
                                    BibleReaderPrefs.setBibleReaderTextScale(
                                        context,
                                        next.coerceIn(0.8f, 1.6f)
                                    )
                                }
                            },
                            label = { Text("${(textScale * 100).toInt()}%") },
                        )
                        Spacer(Modifier.weight(1f))
                        FilledTonalIconButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText(prayer.title, prayer.text)
                                clipboard?.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Molitva kopirana", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                        }
                        FilledTonalIconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, prayer.title)
                                    putExtra(Intent.EXTRA_TEXT, "${prayer.title}\n\n${prayer.text}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Podijeli molitvu"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                        }
                        prayer.url?.let { sourceUrl ->
                            FilledTonalIconButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                                }
                            }
                        ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                            }
                        }
                    }

                    val bodySize = (16f * textScale).sp
                    val bodyLine = (22f * textScale).sp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = prayer.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = bodySize,
                                lineHeight = bodyLine
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyLocalPrayerSearchState(
    hasQuery: Boolean,
    onReset: () -> Unit
) {
    RatingEmptyState(
        title = if (hasQuery) "Nema rezultata za pretragu" else "Nema molitava u ovom prikazu",
        body = "Pokušaj prikazati sve molitve.",
        icon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
    TextButton(onClick = onReset) { Text(Strings.showAll) }
}

@Composable
private fun PrayerTopBar(
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Strings.prayerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = Strings.refresh) }
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = Strings.add) }
            }

            if (!isOnline) {
                RatingCalloutCard(
                    icon = { Icon(Icons.Default.Error, contentDescription = null) },
                    title = "Offline",
                    body = Strings.offlineNoInternet,
                    toneSurfaceVariant = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PrayerCard(
    item: PrayerRequest,
    onPrayed: () -> Unit,
    onEncourage: () -> Unit,
    onOpenDetails: () -> Unit,
    isAdminMode: Boolean = false,
    onDelete: (() -> Unit)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.60f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = item.authorName?.takeIf { it.isNotBlank() } ?: Strings.anonymous,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(6.dp))
                if (item.tags.isNotEmpty()) {
                    Text(
                        text = item.tags.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {

                    AssistChip(
                        onClick = onPrayed,
                        label = { Text(if (item.iPrayed) Strings.iPrayedChecked else Strings.iPrayed) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (item.iPrayed) Icons.Filled.CheckCircle else Icons.Outlined.FavoriteBorder,
                                contentDescription = null
                            )
                        }
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "🙏 ${item.prayedCount}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        IconButton(onClick = onEncourage) {
                            Icon(Icons.Outlined.Chat, contentDescription = Strings.encourage)
                        }
                        Text(
                            text = "${item.encouragementCount}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onOpenDetails) {
                        Icon(Icons.Outlined.Info, contentDescription = Strings.details)
                    }
                    if (isAdminMode) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Error, contentDescription = Strings.delete, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerDetailsSheet(
    request: PrayerRequest,
    encouragements: List<Encouragement>,
    isLoading: Boolean,
    textScale: Float,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val bodySize = (16f * textScale).sp
            val bodyLine = (22f * textScale).sp

            Text(Strings.needLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
            Text(
                request.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = bodySize,
                    lineHeight = bodyLine
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.encouragements, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Normal)
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${request.encouragementCount}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (encouragements.isEmpty()) {
                Text(
                    Strings.noEncouragements,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                encouragements.forEach { e ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                e.authorName?.takeIf { it.isNotBlank() } ?: Strings.anonymous,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(e.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AddPrayerDialog(
    onDismiss: () -> Unit,
    onSubmit: (text: String, name: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val canSend = remember(text) { text.trim().length >= 5 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.newPrayerNeed) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(Strings.writeNeed) },
                    placeholder = { Text(Strings.needPlaceholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.nameOptional) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (!canSend && text.isNotBlank()) {
                    Text(
                        Strings.minimumCharsRequired5,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text, name.takeIf { it.isNotBlank() }) },
                enabled = canSend
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(Strings.publish)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(Strings.dismiss)
            }
        }
    )
}

@Composable
private fun AddEncouragementDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (message: String, name: String?) -> Unit
) {
    var msg by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val canSend = remember(msg) { msg.trim().length >= 2 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.encouragementTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = msg,
                    onValueChange = { msg = it },
                    label = { Text(Strings.writeMessage) },
                    placeholder = { Text(Strings.encouragementPlaceholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.nameOptional) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (!canSend && msg.isNotBlank()) {
                    Text(
                        "Napiši bar 2 karaktera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(msg, name.takeIf { it.isNotBlank() }) }, enabled = canSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(Strings.send)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.dismiss) } }
    )
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(Strings.noPrayerNeeds, fontWeight = FontWeight.Normal)
        Spacer(Modifier.height(8.dp))
        Text(
            "Dodaj svoju potrebu i zajednica će se moliti.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(Strings.addNeed)
        }
    }
}

private fun loadLocalPrayers(context: android.content.Context): List<LocalPrayerItem> {
    context.resources.openRawResource(com.youtube.rating.core.data.R.raw.molitve).use { input ->
        JsonReader(input.reader()).use { reader ->
            val items = mutableListOf<LocalPrayerItem>()
            var index = 0
            reader.beginArray()
            while (reader.hasNext()) {
                var title = ""
                var text = ""
                var id: String? = null
                var url: String? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "title" -> title = reader.nextString().trim()
                        "text" -> text = reader.nextString().trim()
                        "psalm" -> id = reader.nextString().trim().takeIf { it.isNotBlank() }
                        "url" -> url = reader.nextString().trim().takeIf { it.isNotBlank() }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (title.isNotBlank() && text.isNotBlank()) {
                    items.add(
                        LocalPrayerItem(
                            id = id ?: index.toString(),
                            title = title,
                            text = text,
                            url = url
                        )
                    )
                }
                index++
            }
            reader.endArray()
            return items
        }
    }
}

@Composable
private fun ErrorState(message: UiText, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Text(
                UiText.StringResource(com.youtube.rating.core.data.R.string.ui_error_loading_title).asString(),
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(8.dp))
            Text(message.asString(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(UiText.StringResource(com.youtube.rating.core.data.R.string.ui_try_again).asString())
            }
        }
    }
}

@Composable
private fun PrayerListSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = 8,
            contentType = { "prayer_skeleton" }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.35f)
                            .height(18.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .width(120.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                        )
                        Box(
                            Modifier
                                .width(70.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                        )
                    }
                }
            }
        }
    }
}
