package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.FASTING_TYPE_BREAD
import com.youtube.rating.android.storage.FASTING_TYPE_FULL
import com.youtube.rating.android.storage.FASTING_TYPE_WATER
import com.youtube.rating.android.storage.FastingEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun FastingTrackerCard(
    fastingEntries: Map<String, FastingEntry>,
    onSaveEntry: (FastingEntry) -> Unit,
    onOpenDetails: (() -> Unit)?
) {
    var monthOffset by remember { mutableStateOf(0) }
    val locale = Locale.getDefault()
    val monthFormatter = remember { SimpleDateFormat("LLLL yyyy", locale) }

    val displayCal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, monthOffset)
    }

    val year = displayCal.get(Calendar.YEAR)
    val month = displayCal.get(Calendar.MONTH)
    val monthLabel = monthFormatter.format(displayCal.time)

    val todayCal = Calendar.getInstance()
    val todayKey = dateKeyFor(cal = todayCal)
    val fastingDays = fastingEntries.keys

    val monthCells = buildMonthCells(year = year, month = month)
    val monthKeys = monthCells.filterNotNull().map { day ->
        dateKeyFor(year = year, month = month, day = day)
    }.toSet()
    val monthTotal = fastingDays.count { it in monthKeys }
    val currentStreak = calculateCurrentStreak(days = fastingDays, todayKey = todayKey)
    val longestStreak = calculateLongestStreak(days = fastingDays)

    var editDayKey by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = Strings.fastingTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = Strings.fastingSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenDetails != null) {
                        IconButton(onClick = onOpenDetails) {
                            Icon(Icons.Default.Edit, contentDescription = Strings.fastingDetails)
                        }
                    }
                    IconButton(onClick = { monthOffset -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = Strings.previousMonth)
                    }
                    Text(
                        text = monthLabel.replaceFirstChar { it.uppercase(locale) },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { monthOffset += 1 }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = Strings.nextMonth)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(label = Strings.thisMonth, value = "$monthTotal dana")
                StatPill(label = Strings.currentStreak, value = "$currentStreak")
                StatPill(label = Strings.longestStreak, value = "$longestStreak")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(Strings.dayMon, Strings.dayTue, Strings.dayWed, Strings.dayThu, Strings.dayFri, Strings.daySat, Strings.daySun).forEach { name ->
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            monthCells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { day ->
                        val isEmpty = day == null
                        val dayKey = if (day != null) dateKeyFor(year = year, month = month, day = day) else ""
                        val isFasting = dayKey.isNotEmpty() && fastingDays.contains(dayKey)
                        val isToday = dayKey == todayKey

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEmpty) {
                                Spacer(modifier = Modifier.size(36.dp))
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isFasting) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    border = if (isToday) {
                                        androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    } else null
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { editDayKey = dayKey },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isFasting) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isFasting) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (isFasting) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (onOpenDetails != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onOpenDetails) {
                        Text(Strings.openDetails)
                    }
                }
            }
        }
    }

    editDayKey?.let { dayKey ->
        val existing = fastingEntries[dayKey]
        var isFasting by remember { mutableStateOf(existing?.isFasting ?: true) }
        var type by remember { mutableStateOf(existing?.type ?: FASTING_TYPE_WATER) }
        var duration by remember { mutableStateOf(existing?.durationHours ?: 16) }
        var note by remember { mutableStateOf(existing?.note ?: "") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editDayKey = null },
            title = { Text("${Strings.fastingTitle} - $dayKey") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isFasting) Strings.fastingActive else Strings.fastingInactive,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Switch(
                            checked = isFasting,
                            onCheckedChange = { isFasting = it }
                        )
                    }

                    if (isFasting) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = type == FASTING_TYPE_WATER,
                                onClick = { type = FASTING_TYPE_WATER },
                                label = { Text(Strings.fastingWater) },
                                colors = FilterChipDefaults.filterChipColors()
                            )
                            FilterChip(
                                selected = type == FASTING_TYPE_BREAD,
                                onClick = { type = FASTING_TYPE_BREAD },
                                label = { Text(Strings.fastingBread) },
                                colors = FilterChipDefaults.filterChipColors()
                            )
                            FilterChip(
                                selected = type == FASTING_TYPE_FULL,
                                onClick = { type = FASTING_TYPE_FULL },
                                label = { Text(Strings.fastingFull) },
                                colors = FilterChipDefaults.filterChipColors()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = Strings.durationLabel(duration),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = duration.toFloat(),
                            onValueChange = { duration = it.toInt() },
                            valueRange = 6f..24f,
                            steps = 17
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(Strings.noteLabel) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveEntry(
                            FastingEntry(
                                dateKey = dayKey,
                                type = type,
                                durationHours = duration,
                                note = note.trim(),
                                isFasting = isFasting
                            )
                        )
                        editDayKey = null
                    }
                ) {
                    Text(Strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editDayKey = null }) {
                    Text(Strings.close)
                }
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
