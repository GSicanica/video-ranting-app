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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.android.localization.Strings
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtube.rating.android.cache.GospelCache
import com.youtube.rating.android.viewmodel.BiblePlannerViewModel
import com.youtube.rating.android.viewmodel.BiblePlannerViewModelFactory
import com.youtube.rating.shared.models.GospelDayResponse
import com.youtube.rating.shared.models.GospelSection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.android.data.prefs.GospelPrefs
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.components.RatingCalloutCard
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingLoadingState
import com.youtube.rating.core.designsystem.components.RatingScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiblePlannerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient: RatingApiClient = koinInject()
    val gospelCache: GospelCache = koinInject()
    val vm: BiblePlannerViewModel = viewModel(factory = BiblePlannerViewModelFactory(apiClient = apiClient, gospelCache = gospelCache))
    val netState by vm.state.collectAsStateWithLifecycle()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var gospelDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val gospelDateLabel = remember(gospelDate) {
        gospelDate.format(dayFormatter)
    }
    val gospelDateParam = remember(gospelDate) { gospelDate.toString() }
    val markedDates by GospelPrefs.gospelMarkedDatesFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = emptySet())
    val isGospelMarked = remember(markedDates, gospelDateParam) {
        gospelDateParam in markedDates
    }

    LaunchedEffect(gospelDate) {
        vm.loadGospelOfDay(date = gospelDateParam, force = false)
    }

    RatingScaffold(
        topBar = {
            RatingTopAppBar(
                title = "Evanđelja dana",
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            GospelPrefs.toggleGospelMarkedDate(context, gospelDateParam)
                        }
                    }) {
                        Icon(
                            imageVector = if (isGospelMarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Označi"
                        )
                    }
                    IconButton(
                        onClick = { vm.loadGospelOfDay(date = gospelDateParam, force = true) },
                        enabled = !netState.gospelLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Osvježi")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GospelDayCard(
                        isLoading = netState.gospelLoading,
                        error = netState.gospelError,
                        payload = netState.gospel,
                        dateLabel = gospelDateLabel,
                        onPrevDate = { gospelDate = gospelDate.minusDays(1) },
                        onNextDate = { gospelDate = gospelDate.plusDays(1) },
                        showAllReadings = true
                )
            }

        }
    }


}

@Composable
private fun GospelDayCard(
    isLoading: Boolean,
    error: String?,
    payload: GospelDayResponse?,
    dateLabel: String,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    showAllReadings: Boolean
) {
    when {
        isLoading -> {
            RatingLoadingState(title = Strings.loading)
        }

        error != null -> {
            RatingCalloutCard(
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                title = Strings.error,
                body = error,
                toneSurfaceVariant = false,
            )
        }

        payload == null -> {
            RatingEmptyState(
                title = Strings.noData,
                body = "Pokušaj osvježiti.",
            )
        }

        else -> {
            PlannerGospelDateSelectorCard(
                dateLabel = dateLabel,
                onPrevDate = onPrevDate,
                onNextDate = onNextDate
            )
            PlannerGospelSectionCard(title = Strings.gospelSection, section = payload.evandjelje)
            if (showAllReadings) {
                PlannerGospelSectionCard(title = Strings.firstReadingSection, section = payload.prvo_citanje)
                PlannerGospelSectionCard(title = Strings.psalmSection, section = payload.psalam)
            }
        }
    }
}

@Composable
private fun PlannerGospelDateSelectorCard(
    dateLabel: String,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevDate) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = Strings.yesterday
                    )
                }
                Text(
                    text = Strings.dateLabel(dateLabel),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNextDate) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = Strings.tomorrow
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerGospelSectionCard(
    title: String,
    section: GospelSection?
) {
    if (section == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal
            )
            if (!section.reference.isNullOrBlank()) {
                Text(
                    text = section.reference.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!section.title.isNullOrBlank()) {
                Text(
                    text = section.title.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!section.text.isNullOrBlank()) {
                Text(
                    text = section.text.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
