package com.youtube.rating.android.ui.screens

import com.youtube.rating.core.coroutines.ioDispatcher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.android.localization.Strings
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.data.BibleBooks
import com.youtube.rating.android.utils.BackupTrigger
import com.youtube.rating.android.utils.ChangeType
import com.youtube.rating.android.utils.BibleApiService
import com.youtube.rating.android.utils.LocalBibleRepository
import com.youtube.rating.shared.models.RandomPassageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import com.youtube.rating.shared.api.RatingApiClient
import kotlin.math.max
import kotlin.math.roundToInt
import com.youtube.rating.android.data.prefs.BibleReaderPrefs

private val whitespaceRegex = Regex("\\s+")
private val sentenceSplitRegex = Regex("(?<=[.!?])\\s+")
private val headingPrefixRegex = Regex("^#{1,6}\\s*")

/**
 * Reader-first UI (Compose + Markdown):
 * - maksimalno prostora za tekst
 * - kontrole su u TopBar/BottomBar
 * - tap na tekst: sakrije/prikaže chrome (reader mode)
 * - offline .md se rendera kao Markdown (MarkdownText)
 * - offline čitanje ide na ioDispatcher
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequentialBibleReaderScreen(
    initialBookIndex: Int,
    initialChapter: Int,
    languageOverride: BibleApiService.BibleLanguage? = null,
    initialHighlightLine: Int? = null,
    showDrawerIcons: Boolean = false,
    onProgressChanged: (bookIndex: Int, chapter: Int) -> Unit,
    onChapterRead: () -> Unit,
    onDismiss: () -> Unit,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val apiClient: RatingApiClient = koinInject()
    val backupTrigger: BackupTrigger = koinInject()

    var currentBookIndex by rememberSaveable {
        mutableIntStateOf(initialBookIndex.coerceIn(0, BibleBooks.books.lastIndex))
    }
    var currentChapter by rememberSaveable { mutableIntStateOf(initialChapter.coerceAtLeast(1)) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var bibleContent by remember { mutableStateOf<RandomPassageResponse?>(null) }

    // font (persist)
    val savedFontSp by BibleReaderPrefs.bibleReaderFontSpFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = 14)
    var fontSp by remember(savedFontSp) { mutableIntStateOf(savedFontSp.coerceIn(10, 30)) }

    val offlineBibleBook by BibleReaderPrefs.offlineBibleBookFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)

    val trainingBibleLang by BibleReaderPrefs.trainingBibleLanguageFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = "hr")
    val effectiveLanguage = languageOverride ?: BibleApiService.BibleLanguage.fromCode(trainingBibleLang.lowercase())
    val isEnglish = effectiveLanguage == BibleApiService.BibleLanguage.ENGLISH
    val isGerman = effectiveLanguage == BibleApiService.BibleLanguage.GERMAN

    var showBookSheet by rememberSaveable { mutableStateOf(false) }
    var showChapterSheet by rememberSaveable { mutableStateOf(false) }
    var showFontSheet by rememberSaveable { mutableStateOf(false) }
    var languageMenuExpanded by rememberSaveable { mutableStateOf(false) }

    // tap-to-hide chrome
    var chromeVisible by rememberSaveable { mutableStateOf(true) }

    // highlights
    val highlightTokens by BibleReaderPrefs.bibleHighlightsFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = emptySet())
    var showHighlightsSheet by rememberSaveable { mutableStateOf(false) }
    var activeLineIndex by rememberSaveable { mutableStateOf(initialHighlightLine) }

    LaunchedEffect(initialBookIndex, initialChapter) {
        val newBookIndex = initialBookIndex.coerceIn(0, BibleBooks.books.lastIndex)
        val newChapter = initialChapter.coerceAtLeast(1)
        if (newBookIndex != currentBookIndex || newChapter != currentChapter) {
            currentBookIndex = newBookIndex
            currentChapter = newChapter
            onProgressChanged(newBookIndex, newChapter)
        }
    }

    LaunchedEffect(initialHighlightLine) {
        activeLineIndex = initialHighlightLine
    }

    val currentBook = remember(currentBookIndex) { BibleBooks.getBook(currentBookIndex) }
    val currentBookChapters = remember(currentBookIndex) {
        (currentBook?.chapters ?: 1).coerceAtLeast(1)
    }

    // Osiguraj chapter granice kad se promijeni knjiga
    LaunchedEffect(currentBookIndex) {
        val maxCh = (BibleBooks.getBook(currentBookIndex)?.chapters ?: 1).coerceAtLeast(1)
        val newCh = currentChapter.coerceIn(1, maxCh)
        if (newCh != currentChapter) {
            currentChapter = newCh
            onProgressChanged(currentBookIndex, currentChapter)
        }
    }

    val hasPrevious = remember(currentBookIndex, currentChapter) {
        BibleBooks.getPreviousChapter(currentBookIndex, currentChapter) != null
    }
    val hasNext = remember(currentBookIndex, currentChapter) {
        BibleBooks.getNextChapter(currentBookIndex, currentChapter) != null
    }
    val overallProgress = remember(currentBookIndex, currentChapter) {
        BibleBooks.calculateProgress(currentBookIndex, currentChapter)
    }

    // Stabilan scroll state (ne resetuje se)
    val scrollState = rememberScrollState()

    // Load token da se spriječi “stari” odgovor da prepiše noviji (brzo kliktanje next/prev)
    var loadToken by remember { mutableLongStateOf(0L) }

    suspend fun loadChapter() {
        val token = ++loadToken
        val book = BibleBooks.getBook(currentBookIndex) ?: return

        isLoading = true
        errorMessage = null

        try {
            if (isEnglish) {
                val response = BibleApiService.getPassage(
                    bookId = book.id,
                    chapter = currentChapter,
                    language = effectiveLanguage
                )
                if (token != loadToken) return // zastarjelo
                if (response.isSuccess) {
                    val data = response.getOrNull()
                    bibleContent = RandomPassageResponse(
                        success = true,
                        book = book.id,
                        chapter = currentChapter.toString(),
                        title = "${book.name} $currentChapter",
                        text = data?.text.orEmpty()
                    )
                } else {
                    errorMessage = response.exceptionOrNull()?.message ?: "Greška pri učitavanju"
                }
            } else if (isGerman) {
                val local = LocalBibleRepository.readGermanChapterOrNull(
                    context = context,
                    bookId = book.id,
                    chapter = currentChapter
                )
                if (token != loadToken) return // zastarjelo
                if (local != null) {
                    bibleContent = RandomPassageResponse(
                        success = true,
                        book = book.id,
                        chapter = currentChapter.toString(),
                        title = "${book.name} $currentChapter",
                        text = local
                    )
                } else {
                    errorMessage = "Nema njemačkog teksta za ${book.name} $currentChapter"
                }
            } else {
                // Prefer local bundled assets (assets/output1/**). If missing, fallback to API.
                val resolvedBookId = LocalBibleRepository.resolveBookId(context, book.id)
                val local = LocalBibleRepository.readChapterOrNull(
                    context = context,
                    bookId = resolvedBookId,
                    chapterFile = currentChapter.toString()
                )

                if (token != loadToken) return // zastarjelo

                if (local != null) {
                    bibleContent = RandomPassageResponse(
                        success = true,
                        book = resolvedBookId,
                        chapter = currentChapter.toString(),
                        title = "${book.name} $currentChapter",
                        text = local
                    )
                } else {
                    val response = withContext(ioDispatcher) {
                        apiClient.getBiblePassage(resolvedBookId, currentChapter.toString())
                    }
                    if (token != loadToken) return // zastarjelo
                    if (response.success) bibleContent = response
                    else errorMessage = response.message ?: "Greška pri učitavanju"
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            if (token != loadToken) return
            errorMessage = e.message ?: "Greška pri učitavanju"
        } finally {
            if (token == loadToken) isLoading = false
        }
    }

    LaunchedEffect(currentBookIndex, currentChapter, offlineBibleBook, effectiveLanguage) {
        // reset scroll na vrh kad promijeniš poglavlje
        // (ako želiš da ostane na poziciji, obriši ova 2 reda)
        scrollState.scrollTo(0)
        loadChapter()
    }

    fun goToPrevious() {
        BibleBooks.getPreviousChapter(currentBookIndex, currentChapter)?.let { (newBook, newChapter) ->
            currentBookIndex = newBook
            currentChapter = newChapter
            onProgressChanged(newBook, newChapter)
        }
    }

    fun goToNext() {
        BibleBooks.getNextChapter(currentBookIndex, currentChapter)?.let { (newBook, newChapter) ->
            currentBookIndex = newBook
            currentChapter = newChapter
            onProgressChanged(newBook, newChapter)
            onChapterRead()
        }
    }

    fun retryLoad() {
        scope.launch { loadChapter() }
    }

    val lineHeightSp = remember(fontSp) { (fontSp + 10).coerceIn(fontSp + 6, fontSp + 14) }

    Scaffold(
        modifier = Modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
                Surface(tonalElevation = 2.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                            }
                            if (showDrawerIcons) {
                                IconButton(onClick = onOpenLeftDrawer) {
                                    Icon(Icons.Default.Menu, contentDescription = "Izbornik")
                                }
                            }
                            Text(
                                text = "${(overallProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (showDrawerIcons) {
                                IconButton(onClick = onOpenRightDrawer) {
                                    Icon(Icons.Default.Bookmark, contentDescription = "Označene rečenice")
                                }
                            }

                            if (languageOverride == null) {
                                Box {
                                    TextButton(
                                        onClick = { languageMenuExpanded = true },
                                        enabled = !isLoading,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            effectiveLanguage.code.uppercase(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = languageMenuExpanded,
                                        onDismissRequest = { languageMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(Strings.croatian) },
                                            onClick = {
                                                languageMenuExpanded = false
                                                scope.launch { BibleReaderPrefs.setTrainingBibleLanguage(context, "hr") }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(Strings.english) },
                                            onClick = {
                                                languageMenuExpanded = false
                                                scope.launch { BibleReaderPrefs.setTrainingBibleLanguage(context, "en") }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(Strings.german) },
                                            onClick = {
                                                languageMenuExpanded = false
                                                scope.launch { BibleReaderPrefs.setTrainingBibleLanguage(context, "de") }
                                            }
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.width(0.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { if (!isLoading) showBookSheet = true }) {
                                Icon(Icons.Default.Book, contentDescription = "Odaberi knjigu")
                            }
                            IconButton(onClick = { if (!isLoading) showChapterSheet = true }) {
                                Icon(Icons.Default.Numbers, contentDescription = "Odaberi poglavlje")
                            }
                            IconButton(onClick = { showHighlightsSheet = true }) {
                                Icon(Icons.Default.Bookmark, contentDescription = "Označene rečenice")
                            }
                            IconButton(onClick = { showFontSheet = true }) {
                                Icon(Icons.Default.TextFields, contentDescription = "Veličina slova")
                            }
                            IconButton(
                                onClick = { if (!isLoading) onChapterRead() },
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Označi pročitano")
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = ::goToPrevious,
                            enabled = hasPrevious && !isLoading
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Text(" ${Strings.previous}")
                        }
                        Button(
                            onClick = ::goToNext,
                            enabled = hasNext && !isLoading
                        ) {
                            Text("${Strings.next} ")
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                errorMessage ?: "Greška",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(onClick = ::retryLoad) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.tryAgain)
                            }
                        }
                    }
                }

                bibleContent != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { chromeVisible = !chromeVisible })
                            }
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        bibleContent?.title?.let { title ->
                            Text(
                                title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        bibleContent?.text?.let { md ->
                            val languageCode = effectiveLanguage.code
                            val contentKey = remember(currentBook, currentChapter, languageCode) {
                                "bible:$languageCode:${currentBook?.id ?: "unknown"}/$currentChapter"
                            }
                            val yellowBg = Color(0xFFFFF59D)
                            val lines = remember(md, isEnglish) {
                                val normalized = md.replace("\r\n", "\n")
                                if (isEnglish) {
                                    val hasBreaks = normalized.contains('\n')
                                    val base = if (hasBreaks) normalized else normalized.replace(whitespaceRegex, " ").trim()
                                    val parts = if (hasBreaks) {
                                        base.split("\n")
                                    } else {
                                        // Split into sentences when API returns a single paragraph
                                        base.split(sentenceSplitRegex)
                                    }
                                    parts.map { it.trimEnd() }
                                } else {
                                    normalized.split("\n").map { it.trimEnd() }
                                }
                            }

                            val bringRequesters = remember(lines.size) {
                                List(lines.size) { BringIntoViewRequester() }
                            }

                            LaunchedEffect(lines, activeLineIndex) {
                                val idx = activeLineIndex ?: -1
                                if (idx in lines.indices) {
                                    bringRequesters[idx].bringIntoView()
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                lines.forEachIndexed { idx, line ->
                                    if (line.isBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        return@forEachIndexed
                                    }
                                    val token = "$contentKey:$idx"
                                    val legacyToken = "bible:${currentBook?.id ?: "unknown"}/$currentChapter:$idx"
                                    val isYellow = if (languageCode == "en") {
                                        token in highlightTokens
                                    } else {
                                        token in highlightTokens || legacyToken in highlightTokens
                                    }
                                    val isActive = activeLineIndex == idx
                                    val isHeading = line.trimStart().startsWith("#")
                                    val displayText = line
                                        .replace(headingPrefixRegex, "")
                                        .replace("**", "")
                                        .replace("__", "")

                                    Text(
                                        text = displayText,
                                        style = if (isHeading) {
                                            MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Normal,
                                                fontSize = (fontSp + 2).sp
                                            )
                                        } else {
                                            MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = fontSp.sp,
                                                lineHeight = lineHeightSp.sp
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(bringRequesters[idx])
                                            .background(
                                                when {
                                                    isActive -> yellowBg.copy(alpha = 0.65f)
                                                    isYellow -> yellowBg.copy(alpha = 0.45f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                activeLineIndex = idx
                                                scope.launch {
                                                    val next = highlightTokens.toMutableSet()
                                                    if (isYellow) {
                                                        next.remove(token)
                                                        BibleReaderPrefs.removeBibleHighlightText(context, token)
                                                        if (languageCode != "en") {
                                                            next.remove(legacyToken)
                                                            BibleReaderPrefs.removeBibleHighlightText(context, legacyToken)
                                                        }
                                                    } else {
                                                        next.add(token)
                                                        BibleReaderPrefs.putBibleHighlightText(context, token, displayText)
                                                    }
                                                    BibleReaderPrefs.setBibleHighlights(context, next)
                                                    backupTrigger.trigger(ChangeType.BIBLE_HIGHLIGHT_UPDATED)
                                                    try {
                                                    } catch (e: Exception) {
                                                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Strings.selectChapterToRead)
                    }
                }
            }
        }
    }

    if (showBookSheet) {
        BookPickerSheet(
            currentBookIndex = currentBookIndex,
            onDismiss = { showBookSheet = false },
            onPick = { newIndex ->
                showBookSheet = false
                if (newIndex != currentBookIndex) {
                    currentBookIndex = newIndex
                    val maxCh = (BibleBooks.getBook(newIndex)?.chapters ?: 1).coerceAtLeast(1)
                    currentChapter = currentChapter.coerceIn(1, maxCh)
                    onProgressChanged(currentBookIndex, currentChapter)
                }
            }
        )
    }

    if (showChapterSheet) {
        ChapterPickerSheet(
            bookName = currentBook?.name ?: "Biblija",
            currentChapter = currentChapter,
            maxChapters = max(1, currentBookChapters),
            onDismiss = { showChapterSheet = false },
            onPick = { ch ->
                showChapterSheet = false
                if (ch != currentChapter) {
                    currentChapter = ch
                    onProgressChanged(currentBookIndex, currentChapter)
                }
            }
        )
    }

    if (showFontSheet) {
        FontSizeSheet(
            currentFontSp = fontSp,
            onDismiss = { showFontSheet = false },
            onChange = { newSp ->
                fontSp = newSp
                scope.launch { BibleReaderPrefs.setBibleReaderFontSp(context, newSp) }
            },
            onReset = {
                val def = 14
                fontSp = def
                scope.launch { BibleReaderPrefs.setBibleReaderFontSp(context, def) }
            }
        )
    }

    if (showHighlightsSheet) {
        BibleHighlightsSheet(
            highlightTokens = highlightTokens,
            currentBookIndex = currentBookIndex,
            currentChapter = currentChapter,
            languageCode = effectiveLanguage.code,
            onDismiss = { showHighlightsSheet = false },
            onNavigate = { bookIndex, chapter ->
                showHighlightsSheet = false
                if (bookIndex != currentBookIndex || chapter != currentChapter) {
                    currentBookIndex = bookIndex
                    currentChapter = chapter
                    onProgressChanged(bookIndex, chapter)
                }
            },
            onClearAll = {
                scope.launch {
                    BibleReaderPrefs.setBibleHighlights(context, emptySet())
                    backupTrigger.trigger(ChangeType.BIBLE_HIGHLIGHT_UPDATED)
                    BibleReaderPrefs.clearBibleHighlightTexts(context)
                    try {
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookPickerSheet(
    currentBookIndex: Int,
    onDismiss: () -> Unit,
    onPick: (bookIndex: Int) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var search by rememberSaveable { mutableStateOf("") }
    var selectedTestament by rememberSaveable { mutableStateOf<BibleBooks.Testament?>(null) }

    val offlineBibleBook by BibleReaderPrefs.offlineBibleBookFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)

    val filtered by remember(search, selectedTestament) {
        derivedStateOf {
            val q = search.trim().lowercase()
            BibleBooks.books
                .mapIndexed { idx, book -> idx to book }
                .filter { (_, book) ->
                    val okTest = selectedTestament?.let { it == book.testament } ?: true
                    val okSearch = if (q.isBlank()) true else book.name.lowercase().contains(q)
                    okTest && okSearch
                }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        PickerSheetContent(title = "Odaberi knjigu") {
            PickerSearchField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = Icons.Default.Search,
                placeholder = "Traži… (npr. Ivan, Psalmi)"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedTestament == null, onClick = { selectedTestament = null }, label = { Text(Strings.all) })
                FilterChip(selected = selectedTestament == BibleBooks.Testament.OLD, onClick = { selectedTestament = BibleBooks.Testament.OLD }, label = { Text(Strings.oldTestament) })
                FilterChip(selected = selectedTestament == BibleBooks.Testament.NEW, onClick = { selectedTestament = BibleBooks.Testament.NEW }, label = { Text(Strings.newTestament) })
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.first }) { (idx, book) ->
                    val selected = idx == currentBookIndex
                    val isOfflineBook = book.id == offlineBibleBook

                    Card(
                        onClick = { onPick(idx) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(book.name, fontWeight = FontWeight.SemiBold)
                                if (isOfflineBook) {
                                    Icon(
                                        imageVector = Icons.Filled.OfflinePin,
                                        contentDescription = "Offline dostupno",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                "Poglavlja: ${book.chapters} • ${
                                    if (book.testament == BibleBooks.Testament.OLD) "Stari zavjet" else "Novi zavjet"
                                }${if (isOfflineBook) " • Offline" else ""}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterPickerSheet(
    bookName: String,
    currentChapter: Int,
    maxChapters: Int,
    onDismiss: () -> Unit,
    onPick: (chapter: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by rememberSaveable { mutableStateOf("") }

    val filtered by remember(search, maxChapters) {
        derivedStateOf {
            val q = search.trim()
            val wanted = q.toIntOrNull()
            if (wanted == null) (1..maxChapters).toList()
            else listOf(wanted.coerceIn(1, maxChapters))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        PickerSheetContent(title = "Poglavlja – $bookName") {
            PickerSearchField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = Icons.Default.Numbers,
                placeholder = "Upiši broj poglavlja (npr. 3)",
                filter = { input -> input.filter { it.isDigit() }.take(3) }
            )

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it }) { ch ->
                    val selected = ch == currentChapter
                    Card(
                        onClick = { onPick(ch) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Poglavlje $ch",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) Text("✓", fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerSheetContent(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
        content()
    }
}

@Composable
private fun PickerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    placeholder: String,
    filter: ((String) -> String)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(filter?.invoke(input) ?: input) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Očisti")
                }
            }
        },
        placeholder = { Text(placeholder) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSizeSheet(
    currentFontSp: Int,
    onDismiss: () -> Unit,
    onChange: (newFontSp: Int) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var local by rememberSaveable { mutableIntStateOf(currentFontSp.coerceIn(10, 30)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TextFields, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(Strings.fontSize, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
                Spacer(Modifier.weight(1f))
                Text("$local sp", fontWeight = FontWeight.SemiBold)
            }

            Slider(
                value = local.toFloat(),
                onValueChange = { local = it.roundToInt().coerceIn(10, 30) },
                valueRange = 10f..30f,
                steps = (30 - 10) - 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { local = (local - 1).coerceAtLeast(10) },
                    enabled = local > 10
                ) { Icon(Icons.Default.Remove, contentDescription = "Smanji") }

                OutlinedButton(
                    onClick = { local = (local + 1).coerceAtMost(30) },
                    enabled = local < 30
                ) { Icon(Icons.Default.Add, contentDescription = "Povećaj") }

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = {
                        local = 14
                        onReset()
                    }
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.reset)
                }
            }

            Button(
                onClick = {
                    onChange(local)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(Strings.save) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BibleHighlightsSheet(
    highlightTokens: Set<String>,
    currentBookIndex: Int,
    currentChapter: Int,
    languageCode: String,
    onDismiss: () -> Unit,
    onNavigate: (bookIndex: Int, chapter: Int) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val highlightTexts by BibleReaderPrefs.bibleHighlightTextsFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = emptyMap())

    data class HighlightItem(
        val token: String,
        val language: String,
        val bookId: String,
        val bookName: String,
        val bookIndex: Int,
        val chapter: Int,
        val lineIndex: Int,
        val text: String
    )

    val items = remember(highlightTokens, highlightTexts, languageCode) {
        highlightTokens
            .mapNotNull { token ->
                val withoutPrefix = token.removePrefix("bible:")
                val langAndRest = withoutPrefix.split(":", limit = 2)
                val (lang, rest) = if (langAndRest.size == 2 && langAndRest[0].length == 2) {
                    langAndRest[0] to langAndRest[1]
                } else {
                    "hr" to withoutPrefix
                }
                if (languageCode == "en" && lang != "en") return@mapNotNull null
                if (languageCode != "en" && lang != "hr") return@mapNotNull null
                val colonIdx = rest.lastIndexOf(':')
                if (colonIdx < 0) return@mapNotNull null
                val lineIndex = rest.substring(colonIdx + 1).toIntOrNull()
                    ?: return@mapNotNull null
                val bookChapter = rest.substring(0, colonIdx)
                val slashIdx = bookChapter.indexOf('/')
                if (slashIdx < 0) return@mapNotNull null
                val bookId = bookChapter.substring(0, slashIdx)
                val chapter = bookChapter.substring(slashIdx + 1).toIntOrNull()
                    ?: return@mapNotNull null
                val bookIndex = BibleBooks.books.indexOfFirst { it.id == bookId }
                val bookName = BibleBooks.books.getOrNull(bookIndex)?.name ?: bookId
                val text = highlightTexts[token] ?: ""
                HighlightItem(token = token, language = lang, bookId = bookId, bookName = bookName, bookIndex = bookIndex, chapter = chapter, lineIndex = lineIndex, text = text)
            }
            .sortedWith(compareBy({ it.bookIndex }, { it.chapter }, { it.lineIndex }))
    }

    // Group by book/chapter for section headers
    val grouped = remember(items) {
        items.groupBy { "${it.language}:${it.bookId}/${it.chapter}" }
    }

    val yellowBg = Color(0xFFFFF59D)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Označene rečenice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(Strings.deleteAll, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider()

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nema označenih rečenica",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Dodirni rečenicu za označavanje",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (key, highlightItems) ->
                        val first = highlightItems.first()
                        val isCurrentLocation =
                            first.bookIndex == currentBookIndex && first.chapter == currentChapter

                        // Section header
                        item(key = "header_$key") {
                            Card(
                                onClick = {
                                    if (first.bookIndex >= 0) onNavigate(
                                        first.bookIndex,
                                        first.chapter
                                    )
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentLocation)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${first.bookName} – Poglavlje ${first.chapter}",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (isCurrentLocation) {
                                        Text("✓", fontWeight = FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        // Highlighted lines with text
                        items(
                            highlightItems,
                            key = { it.token }
                        ) { item ->
                            if (item.text.isNotBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
                                    color = yellowBg.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = {
                                        if (item.bookIndex >= 0) onNavigate(
                                            item.bookIndex,
                                            item.chapter
                                        )
                                    }
                                ) {
                                    Text(
                                        text = item.text,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 6.dp
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Spacer between chapters
                        item(key = "spacer_$key") {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}
