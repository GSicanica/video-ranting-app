package com.youtube.rating.android.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.viewmodel.CallsViewModel
import com.youtube.rating.core.designsystem.components.RatingCalloutCard
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.core.designsystem.components.RatingSectionHeader
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.theme.spacing
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(viewModel: CallsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prettyDateTimeFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    val zoneId = remember { ZoneId.systemDefault() }
    val spacing = MaterialTheme.spacing

    fun formatIsoOrRaw(value: String?): String =
        com.youtube.rating.android.ui.screens.formatIsoOrRaw(value = value, formatter = prettyDateTimeFormatter)

    fun parseIsoToEpochMillis(value: String?): Long? =
        com.youtube.rating.android.ui.screens.isoToEpochMillis(value = value)

    fun isoFromLocal(dateMillis: Long, hour: Int, minute: Int): String =
        com.youtube.rating.android.ui.screens.isoFromLocal(
            dateMillis = dateMillis,
            hour = hour,
            minute = minute,
            zoneId = zoneId
        )

    fun openNativeDateTimePicker() {
        val currentIso = uiState.availabilityFrom
        val initialMillis = parseIsoToEpochMillis(value = currentIso) ?: (System.currentTimeMillis() + 10 * 60 * 1000L)
        val initial = Instant.ofEpochMilli(initialMillis).atZone(zoneId)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, initial.year)
            set(Calendar.MONTH, initial.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, initial.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, initial.hour)
            set(Calendar.MINUTE, initial.minute)
        }

        DatePickerDialog(
            context,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    context,
                    { _, hh, mm ->
                        cal.set(Calendar.HOUR_OF_DAY, hh)
                        cal.set(Calendar.MINUTE, mm)
                        viewModel.onAvailabilityFromChange(
                            isoFromLocal(dateMillis = cal.timeInMillis, hour = hh, minute = mm)
                        )
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    @Composable
    fun StatusPill(text: String) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }

    RatingScaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            RatingTopAppBar(
                title = Strings.calls,
                subtitle = "Dostupnost i matching"
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.lg),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                RatingCalloutCard(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    title = "Kako radi",
                    body = "Spajanje je po istom omiljenom psalmu (muško + žensko)."
                )
            }

            item {
                uiState.errorMessage?.let {
                    RatingCalloutCard(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        title = "Status",
                        body = it,
                        toneSurfaceVariant = false
                    )
                }
            }

            item {
                RatingSectionHeader("Profil")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = uiState.displayName,
                                onValueChange = viewModel::onDisplayNameChange,
                                label = { Text("Ime") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.ageYears,
                                onValueChange = { raw ->
                                    viewModel.onAgeYearsChange(raw.filter { it.isDigit() }.take(2))
                                },
                                label = { Text("Godine") },
                                singleLine = true,
                                modifier = Modifier.width(110.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        val favorite = uiState.favoritePsalm?.trim().orEmpty()
                        val favoriteLabel = favorite.toIntOrNull()?.let { "Psalm $it" } ?: favorite.ifBlank { "-" }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Omiljeni psalm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(favoriteLabel, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            if (uiState.favoritePsalmLocked) {
                                Text("Zaključano", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (!uiState.favoritePsalmLocked) {
                            var favoriteInput by remember(uiState.favoritePsalm) { mutableStateOf(uiState.favoritePsalm.orEmpty()) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = favoriteInput,
                                    onValueChange = { favoriteInput = it.filter { ch -> ch.isDigit() }.take(3) },
                                    label = { Text("1-150") },
                                    supportingText = { Text("Unesi broj psalma.") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Button(onClick = { viewModel.onFavoritePsalmChange(favoriteInput) }) {
                                    Text("Postavi")
                                }
                            }
                        }

                        Text("Spol", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.gender == "male",
                                onClick = { viewModel.onGenderChange("male") },
                                label = { Text("Muško") }
                            )
                            FilterChip(
                                selected = uiState.gender == "female",
                                onClick = { viewModel.onGenderChange("female") },
                                label = { Text("Žensko") }
                            )
                        }
                    }
                }
            }

            item {
                RatingSectionHeader("Dostupnost")

                LaunchedEffect(uiState.gender, uiState.favoritePsalm) {
                    viewModel.refreshAvailability()
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Odaberi kada si dostupan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text("Kada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatIsoOrRaw(value = uiState.availabilityFrom), style = MaterialTheme.typography.bodyMedium)
                            }
                            OutlinedButton(onClick = ::openNativeDateTimePicker) {
                                Text("Odaberi")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = viewModel::saveAvailability,
                                enabled = !uiState.isLoading,
                                modifier = Modifier.weight(1f)
                            ) { Text("Spremi") }

                            OutlinedButton(
                                onClick = viewModel::refreshAvailability,
                                enabled = !uiState.isLoading,
                                modifier = Modifier.weight(1f)
                            ) { Text("Osvježi") }
                        }

                        if (uiState.availabilityItems.isEmpty()) {
                            RatingEmptyState(
                                title = "Nema dostupnih termina",
                                body = "Provjeri kasnije ili promijeni profil (spol/psalm).",
                            )
                        }
                    }
                }
            }

            if (uiState.availabilityItems.isNotEmpty()) {
                item {
                    Text("Tko je dostupan", style = MaterialTheme.typography.titleSmall)
                }
                items(uiState.availabilityItems.take(5)) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatIsoOrRaw(value = item.availableFrom),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                RatingSectionHeader("Matching")

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.roomName?.let { room ->
                            StatusPill("Room: $room")
                        }

                        Button(
                            onClick = viewModel::join,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text(
                                if (uiState.isLoading) "Tražim..." else "Pronađi termin",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatIsoOrRaw(value: String?, formatter: DateTimeFormatter): String {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return "-"
    return runCatching {
        OffsetDateTime.parse(raw).toLocalDateTime().format(formatter)
    }.getOrDefault(raw)
}

private fun isoToEpochMillis(value: String?): Long? {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return null
    return runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }.getOrNull()
}

private fun isoFromLocal(dateMillis: Long, hour: Int, minute: Int, zoneId: ZoneId): String {
    val local = Instant.ofEpochMilli(dateMillis)
        .atZone(zoneId)
        .withHour(hour)
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
    return local.toOffsetDateTime().toString()
}
