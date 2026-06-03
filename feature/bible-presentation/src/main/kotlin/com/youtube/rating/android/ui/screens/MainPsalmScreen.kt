package com.youtube.rating.android.ui.screens

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.data.FavoritesRepository
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.FavoriteItemType
import com.youtube.rating.android.storage.FavoriteVideo
import com.youtube.rating.android.utils.BibleApiService
import com.youtube.rating.android.viewmodel.PsalmsViewModel
import com.youtube.rating.shared.models.PsalmDto
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.random.Random
import com.youtube.rating.android.data.prefs.BibleReaderPrefs
import com.youtube.rating.android.data.prefs.PsalmPrefs

@Composable
fun MainPsalmScreen(
    initialPsalm: Int? = null,
    refreshSignal: StateFlow<Long>? = null,
    languageOverride: BibleApiService.BibleLanguage? = null,
    useEnglishLabels: Boolean = false
) {
    val vm: PsalmsViewModel = koinViewModel()
    PsalmScreen(
        viewModel = vm,
        initialPsalm = initialPsalm,
        refreshSignal = refreshSignal,
        languageOverride = languageOverride,
        useEnglishLabels = useEnglishLabels
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PsalmScreen(
    viewModel: PsalmsViewModel,
    initialPsalm: Int? = null,
    refreshSignal: StateFlow<Long>? = null,
    languageOverride: BibleApiService.BibleLanguage? = null,
    useEnglishLabels: Boolean = false
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val favoritesRepository: FavoritesRepository = koinInject()
    val favorites by favoritesRepository.getFavoritesFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteIds by remember(favorites) {
        derivedStateOf { favorites.map { it.videoId }.toSet() }
    }

    val savedPsalmIds by PsalmPrefs.savedPsalmsFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val textScale by BibleReaderPrefs.bibleReaderTextScaleFlow(context)
        .collectAsStateWithLifecycle(initialValue = 1.0f)

    val trainingBibleLang by BibleReaderPrefs.trainingBibleLanguageFlow(context)
        .collectAsStateWithLifecycle(initialValue = "hr")
    val effectiveLanguage = languageOverride ?: BibleApiService.BibleLanguage.fromCode(trainingBibleLang.lowercase())
    val isEnglish = effectiveLanguage == BibleApiService.BibleLanguage.ENGLISH

    // Žuta mjesta: token format "23:5" (psalam:linija)
    val highlightTokens by PsalmPrefs.highlightedPsalmLinesFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val savedPsalmNumbers by remember(savedPsalmIds) {
        derivedStateOf { savedPsalmIds.mapNotNull { it.toIntOrNull() }.sorted() }
    }

    // Map<psalmNumber, Set<lineIndex>>
    val highlightsMap by remember(highlightTokens) {
        derivedStateOf {
            buildMap<Int, MutableSet<Int>> {
                highlightTokens.forEach { token ->
                    val parts = token.split(":")
                    val p = parts.getOrNull(0)?.toIntOrNull()
                    val line = parts.getOrNull(1)?.toIntOrNull()
                    if (p != null && line != null) {
                        getOrPut(p) { mutableSetOf() }.add(line)
                    }
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // koji žuti red trenutno "fokusiramo" (radi bringIntoView unutar kartice)
    var activeYellow by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) } // (psalm, line)

    // ako smo u pretrazi, očistimo query pa čekamo da se lista vrati prije skoka
    var pendingJump by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }
    var initialJumpDone by rememberSaveable { mutableStateOf(false) }

    // Random mode - uvijek aktivan
    var randomMode by rememberSaveable { mutableStateOf(true) }
    var randomPsalm by rememberSaveable { mutableStateOf<Int?>(null) }
    var randomOrigin by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(trainingBibleLang, languageOverride) {
        viewModel.setLanguage(effectiveLanguage)
    }

    val refreshTick = refreshSignal
        ?.collectAsStateWithLifecycle(initialValue = 0L)

    fun openRandomPsalm() {
        if (state.psalms.isEmpty()) return
        if (!randomMode) {
            randomOrigin = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
        randomMode = true
        val psalm = state.psalms.random().psalm
        randomPsalm = psalm
        if (state.query.isNotBlank()) viewModel.onQueryChange("")
        pendingJump = psalm to 0
    }

    LaunchedEffect(state.psalms.size) {
        if (state.psalms.isNotEmpty() && randomPsalm == null) {
            openRandomPsalm()
        }
    }

    LaunchedEffect(refreshTick?.value) {
        val tick = refreshTick?.value ?: 0L
        if (tick > 0L) {
            openRandomPsalm()
        }
    }

    fun exitRandomMode() {
        randomMode = false
        randomPsalm = null
        val origin = randomOrigin
        randomOrigin = null
        if (origin != null) {
            scope.launch { listState.scrollToItem(origin.first, origin.second) }
        }
    }

    // Kad pendingJump postoji i psalam je vidljiv u filtered -> scroll + expand + fokus
    LaunchedEffect(pendingJump, state.filtered, state.query) {
        val target = pendingJump ?: return@LaunchedEffect
        val (p, line) = target
        val idx = state.filtered.indexOfFirst { it.psalm == p }
        if (idx >= 0) {
            listState.scrollToItem(idx)
            viewModel.expandPsalm(p)
            activeYellow = p to line
            pendingJump = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth()) {

                // PRVI RED: kontrole
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        modifier = Modifier.height(36.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    // Žuto (random jump)
                    val highlightCount = highlightTokens.size
                    AssistChip(
                        onClick = {
                            if (highlightTokens.isEmpty()) return@AssistChip

                            val tokens = highlightTokens.toList()
                            val token = tokens[Random.nextInt(tokens.size)]
                            val parts = token.split(":")
                            val p = parts.getOrNull(0)?.toIntOrNull()
                            val line = parts.getOrNull(1)?.toIntOrNull()
                            if (p == null || line == null) return@AssistChip

                            if (state.query.isNotBlank()) viewModel.onQueryChange("")
                            pendingJump = p to line
                        },
                        label = {
                            Text(
                                if (useEnglishLabels) {
                                    if (highlightCount > 0) "Highlighted ($highlightCount)" else "Empty"
                                } else {
                                    if (highlightCount > 0) "Oznaceno ($highlightCount)" else "prazno"
                                }
                            )
                        },
                        modifier = Modifier.height(36.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    if (languageOverride == null) {
                        TextButton(
                            onClick = {
                                val next = if (isEnglish) "hr" else "en"
                                scope.launch { BibleReaderPrefs.setTrainingBibleLanguage(context, next) }
                            }
                        ) {
                            Text(if (isEnglish) "EN" else "HR")
                        }
                    }

                    Spacer(Modifier.weight(1f))
                }

                // DRUGI RED: chipovi spremljenih (dodani srcem) -> novi red + prelamanje
                if (savedPsalmNumbers.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedPsalmNumbers.forEach { psalmNumber ->
                            AssistChip(
                                onClick = {
                                    val idx = state.filtered.indexOfFirst { it.psalm == psalmNumber }
                                    if (idx >= 0) {
                                        scope.launch { listState.scrollToItem(idx) }
                                        viewModel.expandPsalm(psalmNumber)
                                    } else {
                                        if (state.query.isNotBlank()) viewModel.onQueryChange("")
                                        pendingJump =
                                            psalmNumber to (highlightsMap[psalmNumber]?.firstOrNull() ?: 0)
                                    }
                                },
                                label = { Text("Ps $psalmNumber") }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val next = !searchOpen
                    searchOpen = next
                    if (!next) {
                        viewModel.onQueryChange("")
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (searchOpen) {
                        Strings.closeSearch
                    } else {
                        Strings.search
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (searchOpen) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    label = {
                        Text(Strings.psalmSearchHint)
                    },
                    singleLine = true,
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = Strings.clearText
                                )
                            }
                        }
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }

                    state.error != null -> {
                        Text(
                            text = "Greška: ${state.error}",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        if (state.filtered.isEmpty()) {
                            Text(
                                text = "Nema rezultata.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = state.filtered,
                                    key = { it.psalm },
                                    contentType = { "psalm" }
                                ) { psalm ->
                                    val highlightedLines = highlightsMap[psalm.psalm] ?: emptySet()
                                    val activeLine = activeYellow
                                        ?.takeIf { it.first == psalm.psalm }
                                        ?.second
                                    val psalmLines = remember(psalm.text) {
                                        psalm.text
                                            .trim()
                                            .replace("\r\n", "\n")
                                            .split("\n")
                                            .map { it.trimEnd() }
                                            .filter { it.isNotBlank() }
                                    }

                                    PsalmCard(
                                        item = psalm,
                                        expanded = psalm.psalm in state.expanded,
                                        textScale = textScale,
                                        isFavorite = ("psalm:${psalm.psalm}" in favoriteIds),
                                        isSaved = psalm.psalm in savedPsalmNumbers,
                                        highlightedLines = highlightedLines,
                                        activeLine = activeLine,
                                        showRandomControls = randomMode && randomPsalm == psalm.psalm,
                                        onRandomNext = { openRandomPsalm() },
                                        onExitRandom = { exitRandomMode() },
                                        onToggleHighlightLine = { lineIndex, makeYellow ->
                                            scope.launch {
                                                val token = "${psalm.psalm}:$lineIndex"
                                                val next = highlightTokens.toMutableSet()
                                                if (makeYellow) next.add(token) else next.remove(token)
                                                PsalmPrefs.setHighlightedPsalmLines(context, next)

                                                activeYellow = psalm.psalm to lineIndex

                                                val id = "psalm:${psalm.psalm}"
                                                val existing = favorites.firstOrNull { it.videoId == id }
                                                if (makeYellow) {
                                                    val lineText = psalmLines.getOrNull(lineIndex)
                                                        ?.takeIf { it.isNotBlank() }
                                                        ?: psalm.title
                                                    if (id in favoriteIds) {
                                                        favoritesRepository.removeFavorite(id)
                                                    }
                                                    favoritesRepository.addFavorite(
                                                        FavoriteVideo(
                                                            videoId = id,
                                                            title = lineText,
                                                            thumbnail = existing?.thumbnail.orEmpty(),
                                                            channelName = existing?.channelName?.ifBlank { "Psalmi" } ?: "Psalmi",
                                                            avgLove = 0.0,
                                                            avgFaith = 0.0,
                                                            avgHope = 0.0,
                                                            totalRatings = 0,
                                                            category = null,
                                                            timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                                                            type = FavoriteItemType.PSALM_HIGHLIGHTED
                                                        )
                                                    )
                                                } else {
                                                    val remainingHighlights = next.any { it.startsWith("${psalm.psalm}:") }
                                                    if (!remainingHighlights && id in favoriteIds) {
                                                        favoritesRepository.removeFavorite(id)
                                                        favoritesRepository.addFavorite(
                                                            FavoriteVideo(
                                                                videoId = id,
                                                                title = "Psalam ${psalm.psalm} - ${psalm.title}",
                                                                thumbnail = existing?.thumbnail.orEmpty(),
                                                                channelName = existing?.channelName?.ifBlank { "Psalmi" } ?: "Psalmi",
                                                                avgLove = 0.0,
                                                                avgFaith = 0.0,
                                                                avgHope = 0.0,
                                                                totalRatings = 0,
                                                                category = null,
                                                                timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                                                                type = FavoriteItemType.PSALM
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onToggle = { viewModel.toggleExpanded(psalm.psalm) },
                                        onOpenUrl = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, psalm.url.toUri()))
                                        },
                                        onToggleFavorite = { makeFavorite ->
                                            scope.launch {
                                                val id = "psalm:${psalm.psalm}"
                                                if (makeFavorite) {
                                                    val hasHighlights = highlightedLines.isNotEmpty()
                                                    favoritesRepository.addFavorite(
                                                        FavoriteVideo(
                                                            videoId = id,
                                                            title = "Psalam ${psalm.psalm} - ${psalm.title}",
                                                            thumbnail = "",
                                                            channelName = "Psalmi",
                                                            avgLove = 0.0,
                                                            avgFaith = 0.0,
                                                            avgHope = 0.0,
                                                            totalRatings = 0,
                                                            category = null,
                                                            type = if (hasHighlights) {
                                                                FavoriteItemType.PSALM_HIGHLIGHTED
                                                            } else {
                                                                FavoriteItemType.PSALM
                                                            }
                                                        )
                                                    )
                                                } else {
                                                    favoritesRepository.removeFavorite(id)
                                                }
                                            }
                                        },
                                        onToggleSaved = { save ->
                                            scope.launch {
                                                val next = savedPsalmIds.toMutableSet()
                                                if (save) next.add(psalm.psalm.toString())
                                                else next.remove(psalm.psalm.toString())
                                                PsalmPrefs.setSavedPsalms(context, next)
                                            }
                                        },
                                        onShareClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Psalam ${psalm.psalm} - ${psalm.title}\n\n${psalm.text}\n\n${psalm.url}"
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, Strings.share)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // initialPsalm: brzo scroll + expand
                        LaunchedEffect(initialPsalm, state.psalms) {
                            initialPsalm?.let { desired ->
                                val idx = state.filtered.indexOfFirst { it.psalm == desired }
                                if (idx >= 0) {
                                    listState.scrollToItem(idx)
                                    viewModel.expandPsalm(desired)
                                } else {
                                    if (state.query.isNotBlank()) viewModel.onQueryChange("")
                                    pendingJump = desired to 0
                                }
                            }
                        }

                        // Ako ima spremljenih: expand + scroll na prvi
                        LaunchedEffect(savedPsalmNumbers, state.psalms) {
                            if (initialPsalm == null && savedPsalmNumbers.isNotEmpty()) {
                                savedPsalmNumbers.forEach { viewModel.expandPsalm(it) }
                                val first = savedPsalmNumbers.first()
                                val idx = state.filtered.indexOfFirst { it.psalm == first }
                                if (idx >= 0) listState.scrollToItem(idx)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PsalmCard(
    item: PsalmDto,
    expanded: Boolean,
    textScale: Float = 1.0f,
    isFavorite: Boolean = false,
    isSaved: Boolean = false,
    highlightedLines: Set<Int> = emptySet(),
    activeLine: Int? = null,
    showRandomControls: Boolean = false,
    onRandomNext: () -> Unit = {},
    onExitRandom: () -> Unit = {},
    onToggleHighlightLine: (lineIndex: Int, makeYellow: Boolean) -> Unit = { _, _ -> },
    onToggle: () -> Unit,
    onOpenUrl: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit = {},
    onToggleSaved: (Boolean) -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    val trimmedText = remember(item.text) { item.text.trim() }

    val lines = remember(trimmedText) {
        trimmedText
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
    }

    val bodySize = (16f * textScale).sp
    val bodyLine = (22f * textScale).sp

    val yellowBg = Color(0xFFFFF59D)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = (18f * textScale).sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onOpenUrl() }
                    )
                }

                IconButton(onClick = onShareClick) {
                    Icon(Icons.Default.Share, contentDescription = Strings.share)
                }

                IconButton(onClick = { onToggleSaved(!isSaved) }) {
                    if (isSaved) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Ukloni iz spremljenih"
                        )
                    } else {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Spremi psalam"
                        )
                    }
                }

                IconButton(onClick = { onToggleFavorite(!isFavorite) }) {
                    if (isFavorite) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Unfavorite")
                    } else {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Favorite")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (highlightedLines.isNotEmpty()) {
                Text(
                    text = "Označeno: ${highlightedLines.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (showRandomControls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(onClick = onRandomNext, label = { Text("Random") })
                    AssistChip(onClick = onExitRandom, label = { Text("X") })
                }
                Spacer(Modifier.height(6.dp))
            }

            val visibleLines = if (expanded) lines else lines.take(4)

            Column {
                visibleLines.forEachIndexed { idx, line ->
                    val bring = remember(item.psalm, idx) { BringIntoViewRequester() }
                    val isYellow = idx in highlightedLines
                    val isActive = activeLine == idx

                    LaunchedEffect(isActive, expanded) {
                        if (isActive && expanded) bring.bringIntoView()
                    }

                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = bodySize,
                            lineHeight = bodyLine
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bring)
                            .background(
                                when {
                                    isYellow && isActive -> yellowBg.copy(alpha = 0.75f)
                                    isYellow -> yellowBg.copy(alpha = 0.45f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable {
                                val next = !(idx in highlightedLines)
                                onToggleHighlightLine(idx, next)
                            }
                            .padding(vertical = 2.dp)
                    )
                }

                if (!expanded && lines.size > 4) {
                    Text(
                        text = "…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onToggle) {
                    Text(if (expanded) "Sakrij" else "Prikaži više")
                }
            }
        }
    }
}
