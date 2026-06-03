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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.youtube.rating.android.localization.Strings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import kotlin.math.max
import com.youtube.rating.android.data.prefs.TrainingPrefs
import com.youtube.rating.core.designsystem.components.RatingTopAppBar

private data class TrainingDayStats(
    val date: String,
    val psalms: Int,
    val bibleArticles: Int,
    val bibleMeditationMin: Int,
    val prayer: Int,
    val rosary: Int,
    val encouragement: Int,
    val points: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiritualTrainingStatsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val statsJson by TrainingPrefs.trainingStatsJsonFlow(context)
        .collectAsStateWithLifecycle(lifecycle = lifecycle, initialValue = "{}")

    val statsMap = remember(statsJson) { parseTrainingStats(json = statsJson) }
    val weekDates = remember(statsJson) { lastNDates(count = 7) }
    val monthDates = remember(statsJson) { lastNDates(count = 30) }

    val weekStats = remember(statsMap, weekDates) { buildStatsList(statsMap, weekDates) }
    val monthStats = remember(statsMap, monthDates) { buildStatsList(statsMap, monthDates) }

    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = Strings.statistics,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            StatsSection(
                title = Strings.thisWeek,
                subtitle = Strings.last7Days,
                stats = weekStats,
                showLabels = true
            )
            StatsSection(
                title = Strings.thisMonth,
                subtitle = Strings.last30Days,
                stats = monthStats,
                showLabels = false
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatsSection(
    title: String,
    subtitle: String,
    stats: List<TrainingDayStats>,
    showLabels: Boolean
) {
    val pointsTotal = stats.sumOf { it.points }

    val bibleUnitsTotal = stats.sumOf { max(it.bibleArticles, it.bibleMeditationMin / 10) }
    val meditationMinTotal = stats.sumOf { it.bibleMeditationMin }

    val prayerTotal = stats.sumOf { it.prayer }
    val rosaryTotal = stats.sumOf { it.rosary }
    val encouragementTotal = stats.sumOf { it.encouragement }
    val avgPoints = if (stats.isEmpty()) 0 else (pointsTotal / stats.size)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(Strings.pointsLabel(pointsTotal), fontWeight = FontWeight.SemiBold)
            }

            PointsBarChart(stats = stats, showLabels = showLabels)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatChip(label = Strings.bibleValueLabel, value = bibleUnitsTotal.toString())
                StatChip(label = Strings.meditationMinLabel, value = meditationMinTotal.toString())
                StatChip(label = Strings.prayersLabel, value = prayerTotal.toString())
                StatChip(label = Strings.rosaryLabel, value = rosaryTotal.toString())
                StatChip(label = Strings.encouragementLabel, value = encouragementTotal.toString())
                StatChip(label = Strings.averageLabel, value = avgPoints.toString())
            }
        }
    }
}

@Composable
private fun PointsBarChart(stats: List<TrainingDayStats>, showLabels: Boolean) {
    val maxPoints = (stats.maxOfOrNull { it.points } ?: 1).toFloat().coerceAtLeast(1f)
    val maxHeight = 64.dp
    val barWidth = if (showLabels) 18.dp else 8.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        stats.forEach { day ->
            val height = (day.points / maxPoints).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.widthIn(min = barWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .height(maxHeight)
                        .width(barWidth)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(barWidth)
                            .height(maxHeight * height)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            if (showLabels) {
                Text(
                    text = shortDate(date = day.date),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}


@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseTrainingStats(json: String): Map<String, TrainingDayStats> {
    val result = LinkedHashMap<String, TrainingDayStats>()
    val obj = try {
        JSONObject(json)
    } catch (e: Exception) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        JSONObject()
    }
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val day = obj.optJSONObject(key) ?: continue

        val psalms = day.optInt("psalms", 0)

        // kompatibilno sa starim ključevima
        val bibleArticles = day.optInt("bibleArticles", day.optInt("bibleMin", 0))
        val bibleMeditationMin = day.optInt("bibleMeditationMin", day.optInt("bibleMeditation", 0))

        val prayer = day.optInt("prayer", 0)
        val rosary = day.optInt("rosary", 0)
        val encouragement = day.optInt("encouragement", 0)

        // ✅ NEW Bible scoring (isto kao u glavnom ekranu)
        val meditationUnits = (bibleMeditationMin.coerceAtLeast(0)) / 10
        val bibleUnits = max(bibleArticles.coerceAtLeast(0), meditationUnits)
        val hasAnyBible = bibleArticles > 0 || bibleMeditationMin >= 10

        val biblePoints = biblePointsFromUnits(units = bibleUnits, hasAnyBible = hasAnyBible)

        val calcPoints =
            biblePoints +
                    (if (prayer >= 1) 4 else 0) +
                    (if (rosary >= 1) 5 else 0) +
                    (if (encouragement >= 1) 2 else 0)

        val points = day.optInt("points", calcPoints)

        result[key] = TrainingDayStats(
            date = key,
            psalms = psalms,
            bibleArticles = bibleArticles,
            bibleMeditationMin = bibleMeditationMin,
            prayer = prayer,
            rosary = rosary,
            encouragement = encouragement,
            points = points
        )
    }
    return result
}

private fun lastNDates(count: Int): List<String> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    val out = ArrayList<String>(count)
    for (i in 0 until count) {
        out.add(0, fmt.format(cal.time))
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return out
}

private fun buildStatsList(
    stats: Map<String, TrainingDayStats>,
    dates: List<String>
): List<TrainingDayStats> {
    return dates.map { date ->
        stats[date] ?: TrainingDayStats(date = date, psalms = 0, bibleArticles = 0, bibleMeditationMin = 0, prayer = 0, rosary = 0, encouragement = 0, points = 0)
    }
}

private fun shortDate(date: String): String {
    val src = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dst = SimpleDateFormat("dd.MM", Locale.getDefault())
    val parsed = runCatching { src.parse(date) }.getOrNull() ?: Date()
    return dst.format(parsed)
}

/**
 * ✅ Bible scoring helpers (mora biti isti sistem kao u glavnom ekranu)
 */
private fun biblePointsFromUnits(units: Int, hasAnyBible: Boolean): Int {
    val u = units.coerceAtLeast(0)
    val base = u * 5
    val bonusOver2 = ((u - 2).coerceAtLeast(0)) * 2
    val bonusOver5 = ((u - 5).coerceAtLeast(0)) * 3
    val focusBonus = if (hasAnyBible) 5 else 0
    return base + bonusOver2 + bonusOver5 + focusBonus
}
