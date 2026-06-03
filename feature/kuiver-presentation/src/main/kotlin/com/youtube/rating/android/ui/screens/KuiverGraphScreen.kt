package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.edges
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.layout.LayoutDirection
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.ui.StyledEdgeContent
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.core.designsystem.components.RatingTopAppBar

enum class KuiverLayoutMode {
    HIERARCHICAL,
    FORCE_DIRECTED,
}

private const val KUIVER_PAGE_SIZE = 2

private data class KuiverScenario(
    val id: String,
    val title: String,
    val description: String,
    val relations: List<Pair<String, String>>,
)

private val kuiverScenarios = listOf(
    KuiverScenario(
        id = "feature-flow",
        title = "Flow feature-a",
        description = "Prikaz kako se glavne cjeline aplikacije nadovezuju.",
        relations = listOf(
            "Molitva" to "Pismo",
            "Pismo" to "Navike",
            "Navike" to "Trcanje",
            "Molitva" to "Favoriti",
            "Favoriti" to "Pismo",
        ),
    ),
    KuiverScenario(
        id = "media-loop",
        title = "Media petlja",
        description = "Veza izmedju playera, ocjenjivanja i historije gledanja.",
        relations = listOf(
            "Home" to "Player",
            "Player" to "Ocjenjivanje",
            "Ocjenjivanje" to "Historija",
            "Historija" to "Preporuke",
            "Preporuke" to "Home",
        ),
    ),
    KuiverScenario(
        id = "backend-sync",
        title = "Backend sync",
        description = "Kretanje podataka od UI sloja prema backend-u.",
        relations = listOf(
            "UI" to "ViewModel",
            "ViewModel" to "Repozitorij",
            "Repozitorij" to "API",
            "API" to "Cache",
            "Cache" to "UI",
        ),
    ),
    KuiverScenario(
        id = "notifications",
        title = "Notifikacije",
        description = "Podsjetnici i notifikacije za dnevne aktivnosti.",
        relations = listOf(
            "Settings" to "Permission",
            "Permission" to "Scheduler",
            "Scheduler" to "Push",
            "Push" to "Open App",
            "Open App" to "Settings",
        ),
    ),
    KuiverScenario(
        id = "analytics",
        title = "Analitika",
        description = "Praćenje događaja kroz feature module.",
        relations = listOf(
            "Screen" to "Event",
            "Event" to "Batch",
            "Batch" to "Upload",
            "Upload" to "Dashboard",
            "Dashboard" to "Screen",
        ),
    ),
)

@Composable
fun KuiverGraphScreen(
    onBack: () -> Unit,
) {
    var currentPage by remember { mutableStateOf(1) }
    var refreshTick by remember { mutableStateOf(0) }
    var scenarioItems by remember { mutableStateOf<List<KuiverScenario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedScenarioId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPage, refreshTick) {
        isLoading = true
        if (currentPage == 1) {
            scenarioItems = emptyList()
        }

        val pageResult = loadScenarioPage(page = currentPage)

        pageResult.fold(
            onSuccess = { loadedPage ->
                loadError = null
                scenarioItems = (scenarioItems + loadedPage).distinctBy { it.id }
            },
            onFailure = { throwable ->
                loadError = throwable.message ?: "Nepoznata greska"
            },
        )
        isLoading = false
    }

    LaunchedEffect(scenarioItems, isLoading) {
        if (scenarioItems.isNotEmpty() && scenarioItems.none { it.id == selectedScenarioId }) {
            selectedScenarioId = scenarioItems.first().id
        }
    }

    val selectedScenario = scenarioItems.firstOrNull { it.id == selectedScenarioId }
    val initialGraph = remember {
        buildKuiver {
            nodes("Ucitaj")
        }
    }

    var layoutMode by remember { mutableStateOf(KuiverLayoutMode.HIERARCHICAL) }
    val layoutConfig = remember(layoutMode) {
        when (layoutMode) {
            KuiverLayoutMode.HIERARCHICAL -> LayoutConfig.Hierarchical(
                direction = LayoutDirection.HORIZONTAL,
                levelSpacing = 150f,
                nodeSpacing = 100f,
            )

            KuiverLayoutMode.FORCE_DIRECTED -> LayoutConfig.ForceDirected()
        }
    }

    val viewerState = rememberKuiverViewerState(initialKuiver = initialGraph, layoutConfig = layoutConfig)

    LaunchedEffect(selectedScenario?.id) {
        selectedScenario?.let {
            viewerState.updateKuiver(it.toKuiver())
            viewerState.centerGraph()
        }
    }

    val hasMorePages = scenarioItems.size < kuiverScenarios.size
    val showInitialLoading = isLoading && scenarioItems.isEmpty()

    RatingScaffold(
        topBar = {
            RatingTopAppBar(
                title = "Kuiver + Paginator",
                subtitle = "Graf scenariji sa paginacijom",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Natrag",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LayoutModeSelector(layoutMode = layoutMode, onModeSelected = { layoutMode = it })

            GraphControls(
                scale = viewerState.scale,
                onZoomOut = { viewerState.zoomOut() },
                onZoomIn = { viewerState.zoomIn() },
                onCenter = { viewerState.centerGraph() },
            )

            KuiverScreenStatus(
                showInitialLoading = showInitialLoading,
                isLoading = isLoading,
                loadError = loadError,
                hasItems = scenarioItems.isNotEmpty(),
                onRetry = {
                    currentPage = 1
                    refreshTick++
                },
            )

            KuiverGraphCard(
                hasSelection = selectedScenario != null,
                viewerContent = {
                    KuiverViewer(
                        state = viewerState,
                        modifier = Modifier.fillMaxSize(),
                        nodeContent = { node ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 2.dp,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = node.id,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                        edgeContent = { edge, from, to ->
                            StyledEdgeContent(
                                edge = edge,
                                from = from,
                                to = to,
                                baseColor = MaterialTheme.colorScheme.outline,
                            )
                        },
                    )
                },
            )

            Text(
                text = "Scenariji (Paginator)",
                style = MaterialTheme.typography.titleMedium,
            )

            ScenarioPaginator(
                scenarios = scenarioItems,
                selectedScenarioId = selectedScenarioId,
                onScenarioSelected = { selectedScenarioId = it },
            )

            if (loadError != null && scenarioItems.isNotEmpty()) {
                Text(
                    text = "Greska pri ucitavanju stranice: $loadError",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            PaginationControls(
                isLoading = isLoading,
                hasMorePages = hasMorePages,
                hasItems = scenarioItems.isNotEmpty(),
                hasLoadError = loadError != null,
                onLoadMore = {
                    if (!isLoading) {
                        currentPage += 1
                    }
                },
                onRetry = { refreshTick++ },
            )
        }
    }
}

private fun loadScenarioPage(page: Int): Result<List<KuiverScenario>> = runCatching {
    val fromIndex = (page - 1) * KUIVER_PAGE_SIZE
    kuiverScenarios.drop(fromIndex).take(KUIVER_PAGE_SIZE)
}

@Composable
private fun LayoutModeSelector(
    layoutMode: KuiverLayoutMode,
    onModeSelected: (KuiverLayoutMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = layoutMode == KuiverLayoutMode.HIERARCHICAL,
            onClick = { onModeSelected(KuiverLayoutMode.HIERARCHICAL) },
            label = { Text("Hijerarhijski") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "Hijerarhijski layout",
                )
            },
        )
        FilterChip(
            selected = layoutMode == KuiverLayoutMode.FORCE_DIRECTED,
            onClick = { onModeSelected(KuiverLayoutMode.FORCE_DIRECTED) },
            label = { Text("Force") },
        )
    }
}

@Composable
private fun GraphControls(
    scale: Float,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onCenter: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onZoomOut) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Smanji")
        }
        IconButton(onClick = onZoomIn) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Povecaj")
        }
        IconButton(onClick = onCenter) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centriraj graf")
        }
        Text(
            text = "${(scale * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun KuiverScreenStatus(
    showInitialLoading: Boolean,
    isLoading: Boolean,
    loadError: String?,
    hasItems: Boolean,
    onRetry: () -> Unit,
) {
    when {
        showInitialLoading -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp))
                Text("Ucitam paginirane scenarije...")
            }
        }

        loadError != null && !hasItems -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Greska: $loadError",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onRetry) {
                    Text("Pokusaj ponovo")
                }
            }
        }

        !isLoading && !hasItems -> {
            Text("Nema dostupnih scenarija za prikaz.")
        }

        else -> Unit
    }
}

@Composable
private fun KuiverGraphCard(
    hasSelection: Boolean,
    viewerContent: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        if (!hasSelection) {
            Text(
                text = "Odaberi scenarij ispod da prikazes graf.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        } else {
            viewerContent()
        }
    }
}

@Composable
private fun ScenarioPaginator(
    scenarios: List<KuiverScenario>,
    selectedScenarioId: String?,
    onScenarioSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = scenarios,
            key = { it.id },
        ) { scenario ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = if (scenario.id == selectedScenarioId) 4.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onScenarioSelected(scenario.id) },
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = scenario.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = scenario.description,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaginationControls(
    isLoading: Boolean,
    hasMorePages: Boolean,
    hasItems: Boolean,
    hasLoadError: Boolean,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    if (isLoading && hasItems) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
    }

    if (hasMorePages) {
        Button(onClick = onLoadMore, enabled = !isLoading) {
            Text("Ucitaj jos")
        }
    } else if (hasItems) {
        Text(
            text = "Sve stranice su ucitane.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (hasLoadError && hasItems) {
        Button(onClick = onRetry, enabled = !isLoading) {
            Text("Pokusaj ponovo")
        }
    }
}

private fun KuiverScenario.toKuiver() = buildKuiver {
    val names = relations.flatMap { listOf(it.first, it.second) }.distinct()
    nodes(names)
    edges(*relations.toTypedArray())
}
