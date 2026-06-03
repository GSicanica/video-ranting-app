package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youtube.rating.android.cache.GospelCache
import com.youtube.rating.android.viewmodel.GospelOfDayViewModel
import com.youtube.rating.android.viewmodel.GospelOfDayViewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.android.localization.Strings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.youtube.rating.android.data.prefs.GospelPrefs
import com.youtube.rating.core.designsystem.components.RatingCalloutCard
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingLoadingState
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.components.RatingScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GospelOfDayScreen(
    initialDate: LocalDate? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val apiClient: RatingApiClient = koinInject()
    val gospelCache: GospelCache = koinInject()
    val vm: GospelOfDayViewModel = viewModel(factory = GospelOfDayViewModelFactory(apiClient = apiClient, gospelCache = gospelCache))
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedDate by rememberSaveable(initialDate) { mutableStateOf(initialDate ?: LocalDate.now()) }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val dateLabel = remember(selectedDate) {
        selectedDate.format(dayFormatter)
    }
    val dateParam = remember(selectedDate) { selectedDate.toString() }
    val scope = rememberCoroutineScope()
    val markedDates by GospelPrefs.gospelMarkedDatesFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val isMarked = remember(markedDates, dateParam) { dateParam in markedDates }

    LaunchedEffect(selectedDate) {
        vm.load(date = dateParam, force = false)
    }

    RatingScaffold(
        topBar = {
            RatingTopAppBar(
                title = Strings.gospelTitle,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            GospelPrefs.toggleGospelMarkedDate(context, dateParam)
                        }
                    }) {
                        Icon(
                            imageVector = if (isMarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Označi"
                        )
                    }
                    IconButton(onClick = { vm.load(date = dateParam, force = true) }, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = Strings.refresh)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.loading -> {
                    RatingLoadingState(title = Strings.loading)
                }
                state.error != null -> {
                    RatingCalloutCard(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        title = Strings.error,
                        body = state.error.orEmpty(),
                        ctaLabel = Strings.retryAgain,
                        onCta = { vm.load(force = true) },
                        toneSurfaceVariant = false,
                    )
                }
                state.payload != null -> {
                    state.payload?.let { data ->
                        GospelDateSelectorCard(
                            dateLabel = dateLabel,
                            onPrevDate = { selectedDate = selectedDate.minusDays(1) },
                            onNextDate = { selectedDate = selectedDate.plusDays(1) }
                        )
                        GospelSectionCard(title = Strings.gospelSection, section = data.evandjelje)
                        GospelSectionCard(title = Strings.firstReadingSection, section = data.prvo_citanje)
                        GospelSectionCard(title = Strings.psalmSection, section = data.psalam)
                    }
                }
                else -> {
                    RatingEmptyState(title = Strings.noData)
                }
            }
        }
    }
}
