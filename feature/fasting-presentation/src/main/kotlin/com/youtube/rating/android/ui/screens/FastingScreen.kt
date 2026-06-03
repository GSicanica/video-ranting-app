package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.FASTING_TYPE_BREAD
import com.youtube.rating.android.storage.FASTING_TYPE_FULL

import com.youtube.rating.android.storage.FastingEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.youtube.rating.core.designsystem.components.RatingTopAppBar

@Composable
fun FastingScreen(
    fastingEntries: Map<String, FastingEntry>,
    weeklyGoal: Int,
    onSaveEntry: (FastingEntry) -> Unit,
    onUpdateGoal: (Int) -> Unit,
    onBack: () -> Unit,
    showTopBar: Boolean = true
) {
    val locale = Locale.getDefault()
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", locale) }
    val todayKey = dateKeyFor(cal = Calendar.getInstance())
    val fastingDays = fastingEntries.keys

    val currentStreak = calculateCurrentStreak(days = fastingDays, todayKey = todayKey)
    val longestStreak = calculateLongestStreak(days = fastingDays)
    val totalDays = fastingDays.size

    Column(modifier = Modifier.fillMaxSize()) {
        if (showTopBar) {
            RatingTopAppBar(
                title = Strings.fastingTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                FastingTrackerCard(
                    fastingEntries = fastingEntries,
                    onSaveEntry = onSaveEntry,
                    onOpenDetails = null
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(Strings.statistics, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(label = Strings.total, value = "$totalDays")
                            StatItem(label = Strings.currentStreak, value = "$currentStreak")
                            StatItem(label = Strings.longestStreak, value = "$longestStreak")
                        }
                    }
                }
            }

            item {
                WeeklyProgressCard(
                    entries = fastingEntries,
                    weeklyGoal = weeklyGoal
                )
            }

            item {
                GoalSettingsCard(
                    weeklyGoal = weeklyGoal,
                    onUpdateGoal = onUpdateGoal
                )
            }


            item {
                val recentEntries = fastingEntries.values
                    .sortedByDescending { it.dateKey }
                    .take(6)
                if (recentEntries.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(Strings.recentFasts, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            recentEntries.forEach { entry ->
                                val dateText = runCatching {
                                    val parts = entry.dateKey.split("-")
                                    val cal = Calendar.getInstance().apply {
                                        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                    }
                                    dateFormatter.format(cal.time)
                                }.getOrDefault(entry.dateKey)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dateText, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${entry.durationHours}h • ${typeLabel(type = entry.type)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (entry.note.isNotBlank()) {
                                            Text(
                                                entry.note,
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

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeeklyProgressCard(entries: Map<String, FastingEntry>, weeklyGoal: Int) {
    val weekKeys = weekKeysFromToday()
    val completed = weekKeys.count { entries.containsKey(it) }
    val percent = if (weeklyGoal == 0) 0f else completed.toFloat() / weeklyGoal.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(Strings.weeklyProgress, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                Strings.daysOfFasting(completed, weeklyGoal),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weekKeys.forEach { key ->
                    val filled = entries.containsKey(key)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (filled) 48.dp else 20.dp)
                            .padding(horizontal = 2.dp)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Strings.goalPercentage((percent * 100).toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalSettingsCard(weeklyGoal: Int, onUpdateGoal: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(Strings.weeklyGoalTitle, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(Strings.daysOfFastingGoal(weeklyGoal), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = weeklyGoal.toFloat(),
                onValueChange = { onUpdateGoal(it.toInt()) },
                valueRange = 1f..7f,
                steps = 5
            )
        }
    }
}

private fun typeLabel(type: String): String {
    return when (type) {
        FASTING_TYPE_FULL -> Strings.fastingFull
        FASTING_TYPE_BREAD -> Strings.fastingBread
        else -> Strings.fastingWater
    }
}
