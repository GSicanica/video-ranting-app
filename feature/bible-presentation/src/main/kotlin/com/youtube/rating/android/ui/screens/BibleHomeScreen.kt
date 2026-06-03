package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.utils.BibleApiService
import com.youtube.rating.android.viewmodel.BibleHomeEvent
import com.youtube.rating.android.viewmodel.BibleHomeViewModel
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.youtube.rating.android.data.prefs.TrainingPrefs
import com.youtube.rating.android.cache.GospelCache
import com.youtube.rating.android.viewmodel.BiblePlannerViewModel
import com.youtube.rating.android.viewmodel.BiblePlannerViewModelFactory
import com.youtube.rating.android.viewmodel.BiblePlannerNetworkState
import com.youtube.rating.shared.api.RatingApiClient
import org.koin.compose.koinInject
import com.youtube.rating.core.designsystem.components.RatingTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleHomeScreen(
    onOpenPsalm: (Int) -> Unit,
    vm: BibleHomeViewModel = viewModel(),
    languageOverride: BibleApiService.BibleLanguage? = null,
    useEnglishLabels: Boolean = false,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val apiClient: RatingApiClient = koinInject()
    val gospelCache: GospelCache = koinInject()
    val randomVm: BiblePlannerViewModel =
        viewModel(factory = BiblePlannerViewModelFactory(apiClient = apiClient, gospelCache = gospelCache))
    val randomState by randomVm.state.collectAsStateWithLifecycle()

    // events -> snackbar
    LaunchedEffect(vm) {
        vm.events.collectLatest { e ->
            when (e) {
                is BibleHomeEvent.Message -> snackbarHostState.showSnackbar(e.text.resolve(context))
            }
        }
    }

    // --- UI local state ---
    var openBiblePlanner     by rememberSaveable { mutableStateOf(false) }
    var openSequentialReader by rememberSaveable { mutableStateOf(false) }
    var openBibleSearch by rememberSaveable { mutableStateOf(false) }
    var openRandomPassage by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var psalmsToday by remember { mutableStateOf<List<Int>>(emptyList()) }
    val psalmsDone by TrainingPrefs.trainingPsalmsDoneTodayFlow(context)
        .collectAsStateWithLifecycle(emptySet())

    LaunchedEffect(Unit) {
        val stored = TrainingPrefs.getTrainingPsalmsToday(context)
        val parsed = stored.mapNotNull { it.toIntOrNull() }
            .filter { it in 1..150 }
            .distinct()
        val final = if (parsed.size >= 3) {
            parsed.take(3)
        } else {
            val seed = LocalDate.now().toString().hashCode()
            val random = Random(seed)
            buildSet {
                while (size < 3) {
                    add(random.nextInt(1, 151))
                }
            }.toList()
        }.sorted()

        psalmsToday = final
        if (parsed.size < 3) {
            TrainingPrefs.setTrainingPsalmsToday(
                context,
                final.map { it.toString() }.toSet()
            )
        }
    }
    val psalmsDoneCount = psalmsToday.count { psalmsDone.contains(it.toString()) }
    // If the user explicitly selected a content language in the Training dropdown (languageOverride),
    // that selection must win over the device/app UI language.
    // Requirement: when Croatian is selected, show ALL training items regardless of device language.
    val limitToSequentialReading = when (languageOverride) {
        BibleApiService.BibleLanguage.ENGLISH,
        BibleApiService.BibleLanguage.GERMAN,
        -> true

        BibleApiService.BibleLanguage.CROATIAN,
        -> false

        null,
        -> Strings.currentLanguage == Strings.Language.ENGLISH ||
            Strings.currentLanguage == Strings.Language.GERMAN
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {}
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!limitToSequentialReading) {
            item { SectionHeader(title = if (useEnglishLabels) "Daily habits" else "Dnevne navike") }

            item {
                TrainingCard(
                    title = if (useEnglishLabels) "Reading" else Strings.reading,
                    subtitle = if (useEnglishLabels) "Today: ${ui.bibleReadToday}" else Strings.todayCount(ui.bibleReadToday),
                    progress = if (ui.bibleReadToday > 0) 1f else 0f,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = { openBiblePlanner = true }
                ) {
                    StatRow(
                        primary = if (useEnglishLabels) "Today's reading" else "Današnje čitanje",
                        secondary = "${ui.bibleReadToday}x"
                    )
                    QuickActionRow {
                        AssistChip(
                            onClick = vm::addRead,
                            label = { Text("+1") }
                        )
                        AssistChip(
                            onClick = { vm.subReadIfPossible(ui.bibleReadToday) },
                            label = { Text("-1") },
                            enabled = ui.bibleReadToday > 0
                        )
                        AssistChip(
                            onClick = { openBiblePlanner = true },
                            label = { Text(if (useEnglishLabels) "Plan" else "Plan") }
                        )
                    }
                    PrimaryActionButton(
                        text = if (useEnglishLabels) "Open" else Strings.open,
                        onClick = { openBiblePlanner = true }
                    )
                }
            }

            item {
                TrainingCard(
                    title = if (useEnglishLabels) "Meditation" else Strings.meditationTitle,
                    subtitle = if (useEnglishLabels) "Today: ${ui.thinkingMin} min" else Strings.meditationSubtitle(ui.thinkingMin),
                    progress = (ui.thinkingMin / 10f).coerceIn(0f, 1f),
                    icon = Icons.Default.Psychology,
                ) {
                    StatRow(
                        primary = if (useEnglishLabels) "Today" else "Danas",
                        secondary = "${ui.thinkingMin} min"
                    )
                    QuickActionRow {
                        AssistChip(onClick = { vm.addMeditation(5) }, label = { Text("+5") })
                        AssistChip(onClick = { vm.addMeditation(10) }, label = { Text("+10") })
                        AssistChip(
                            onClick = { vm.subMeditationIfPossible(ui.thinkingMin, 5) },
                            label = { Text("-5") },
                            enabled = ui.thinkingMin >= 5
                        )
                    }
                }
            }

            item { SectionHeader(title = if (useEnglishLabels) "Psalms today" else "Psalmi danas") }

            item {
                TrainingCard(
                    title = if (useEnglishLabels) "Psalms (3 daily)" else Strings.dailyPsalmsTitle,
                    subtitle = if (useEnglishLabels) {
                        "$psalmsDoneCount of ${psalmsToday.size.coerceAtLeast(3)} done"
                    } else {
                        Strings.dailyPsalmsSubtitle(
                        psalmsDoneCount,
                        psalmsToday.size.coerceAtLeast(3)
                        )
                    },
                    progress = if (psalmsToday.isNotEmpty()) psalmsDoneCount / psalmsToday.size.toFloat() else 0f,
                    icon = Icons.Default.FavoriteBorder,
                ) {
                    if (psalmsToday.isEmpty()) {
                        Text(
                            if (useEnglishLabels) "Loading..." else Strings.loading,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        StatRow(
                            primary = if (useEnglishLabels) "Progress" else "Napredak",
                            secondary = "${psalmsDoneCount}/${psalmsToday.size.coerceAtLeast(3)}"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            psalmsToday.forEach { psalm ->
                                val done = psalmsDone.contains(psalm.toString())
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = done,
                                        onClick = {
                                            scope.launch {
                                                TrainingPrefs.toggleTrainingPsalmDoneToday(
                                                    context,
                                                    psalm
                                                )
                                            }
                                        },
                                        label = {
                                            Text(if (useEnglishLabels) "Psalm $psalm" else Strings.psalmNumber(psalm))
                                        },
                                        leadingIcon = if (done) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        colors = FilterChipDefaults.filterChipColors()
                                    )
                                    AssistChip(
                                        onClick = { onOpenPsalm(psalm) },
                                        label = { Text(if (useEnglishLabels) "Open" else Strings.open) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            }

            item { SectionHeader(title = if (useEnglishLabels) "Sequential reading" else "Kontinuirano čitanje") }

            item {
                val seqSubtitle =
                    if (ui.currentBookName != null && ui.currentBookChapters != null) {
                        Strings.sequentialSubtitle(
                            ui.currentBookName ?: "",
                            ui.bibleChapter,
                            ui.currentBookChapters ?: 0,
                            (ui.seqProgress * 100).toInt()
                        )
                    } else {
                        Strings.noSelection
                    }

                TrainingCard(
                    title = Strings.sequentialReading,
                    subtitle = seqSubtitle,
                    progress = ui.seqProgress,
                    icon = Icons.Default.AutoStories,
                    onClick = { openSequentialReader = true }
                ) {
                    val bookName = ui.currentBookName ?: Strings.noSelection
                    val chapterText = if (ui.currentBookChapters != null) {
                        if (useEnglishLabels) "Chapter ${ui.bibleChapter}/${ui.currentBookChapters}"
                        else "Poglavlje ${ui.bibleChapter}/${ui.currentBookChapters}"
                    } else {
                        if (useEnglishLabels) "Select a book" else "Odaberi knjigu"
                    }
                    StatRow(primary = bookName, secondary = chapterText)
                    QuickActionRow {
                        AssistChip(
                            onClick = { openSequentialReader = true },
                            label = { Text(if (useEnglishLabels) "Open" else Strings.open) }
                        )
                        AssistChip(
                            onClick = vm::resetSequential,
                            label = { Text(if (useEnglishLabels) "From beginning" else Strings.fromBeginning) }
                        )
                    }
                    PrimaryActionButton(
                        text = if (useEnglishLabels) "Continue reading" else "Nastavi čitanje",
                        onClick = { openSequentialReader = true }
                    )
                }
            }

            if (!limitToSequentialReading) {
            item { SectionHeader(title = if (useEnglishLabels) "Passage" else "Odlomak") }

            item {
                TrainingCard(
                    title = if (useEnglishLabels) "Random passage" else "Random odlomak",
                    subtitle = if (useEnglishLabels) "Get a random Bible passage" else "Daj random odlomak iz Biblije",
                    progress = 0f,
                    icon = Icons.Default.AutoStories,
                    onClick = { openRandomPassage = true }
                ) {
                    when {
                        randomState.randomLoading -> {
                            Text(
                                if (useEnglishLabels) "Loading..." else Strings.loading,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        randomState.randomError != null -> {
                            Text(
                                randomState.randomError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        randomState.random?.text != null -> {
                            val title = randomState.random?.title?.takeIf { it.isNotBlank() }
                            if (title != null) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                        }

                        else -> {
                            Text(
                                if (useEnglishLabels) "Tap to generate." else "Dodirni za generiranje.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    QuickActionRow {
                        AssistChip(
                            onClick = { openRandomPassage = true },
                            label = { Text(if (useEnglishLabels) "Open" else Strings.open) }
                        )

                    }
                }
            }

            item { SectionHeader(title = if (useEnglishLabels) "Tools" else "Alati") }

            item {
                TrainingCard(
                    title = if (useEnglishLabels) "Bible Search" else "Pretraga Biblije",
                    subtitle = if (useEnglishLabels) "Search the whole Bible by keyword" else "Pretraži cijelu Bibliju po pojmu",
                    progress = 0f,
                    icon = Icons.Default.Search,
                    onClick = { openBibleSearch = true }
                ) {
                    PrimaryActionButton(
                        text = if (useEnglishLabels) "Search the Bible" else "Traži u Bibliji",
                        onClick = { openBibleSearch = true }
                    )
                }
            }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (openBiblePlanner) {
        FullscreenOverlayDialog(onDismiss = { openBiblePlanner = false }) {
            BiblePlannerScreen()
        }
    }

    if (openSequentialReader) {
        FullscreenOverlayDialog(onDismiss = { openSequentialReader = false }) {
            SequentialBibleReaderScreen(
                initialBookIndex = ui.bibleBookIndex,
                initialChapter = ui.bibleChapter,
                languageOverride = languageOverride,
                onProgressChanged = { newBook, newCh ->
                    vm.setSequentialProgress(newBook, newCh)
                },
                onChapterRead = {
                    vm.onChapterRead()
                },
                onDismiss = { openSequentialReader = false },
                showDrawerIcons = false,
                onOpenLeftDrawer = onOpenLeftDrawer,
                onOpenRightDrawer = onOpenRightDrawer
            )
        }
    }

    if (openBibleSearch) {
        FullscreenOverlayDialog(onDismiss = { openBibleSearch = false }) {
            BibleSearchScreen(onDismiss = { openBibleSearch = false })
        }
    }

    if (openRandomPassage) {
        LaunchedEffect(openRandomPassage) {
            if (openRandomPassage && randomState.random?.text.isNullOrBlank() && !randomState.randomLoading) {
                randomVm.loadRandomPassage()
            }
        }
        FullscreenOverlayDialog(
            onDismiss = { openRandomPassage = false },
            showCloseButton = false
        ) {
            RandomPassageReader(
                title = Strings.randomPassageTitle,
                state = randomState,
                onRandom = { randomVm.loadRandomPassage() },
                onDismiss = { openRandomPassage = false }
            )
        }
    }
}

@Composable
private fun RandomPassageReader(
    title: String,
    state: BiblePlannerNetworkState,
    onRandom: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = Strings.close)
                    }
                },
                actions = {
                    AssistChip(
                        onClick = onRandom,
                        label = { Text(Strings.random) },
                        enabled = !state.randomLoading
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    state.randomLoading -> {
                        Text(
                            Strings.loading,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    state.randomError != null -> {
                        Text(
                            state.randomError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    state.random?.text != null -> {
                        state.random?.title?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            state.random?.text.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    else -> {
                        Text(
                            Strings.noData,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrainingOverviewCard(
    todayLabel: String,
    readCount: Int,
    thinkingMin: Int,
    psalmsDone: Int,
    psalmsTotal: Int
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        "Dnevni fokus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        todayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OverviewTile(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = Strings.reading,
                    value = readCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                OverviewTile(
                    text = "🧠",
                    label = Strings.meditationTitle,
                    value = thinkingMin.toString(),
                    modifier = Modifier.weight(1f)
                )
                OverviewTile(
                    icon = Icons.Default.FavoriteBorder,
                    label = "Psalmi",
                    value = "$psalmsDone/$psalmsTotal",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverviewTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (text != null) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrainingCard(
    title: String,
    subtitle: String,
    progress: Float,
    onClick: (() -> Unit)? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = Modifier.padding(horizontal = 16.dp)
    val contentBlock = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            val p = progress.coerceIn(0f, 1f)
            if (p in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
            content()
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            contentBlock()
        }
    } else {
        Card(
            modifier = cardModifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            contentBlock()
        }
    }
}

@Composable
private fun FullscreenOverlayDialog(
    onDismiss: () -> Unit,
    showCloseButton: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            content()
            if (showCloseButton) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = Strings.close)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun StatRow(
    primary: String,
    secondary: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            secondary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionRow(
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text)
    }
}
