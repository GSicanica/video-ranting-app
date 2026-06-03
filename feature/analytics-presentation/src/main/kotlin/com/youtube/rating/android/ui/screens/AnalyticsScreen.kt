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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youtube.rating.core.presentation.state.ResultState
import com.youtube.rating.android.localization.Strings
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.core.designsystem.components.RatingTopAppBar
import com.youtube.rating.core.designsystem.components.RatingCalloutCard
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.android.viewmodel.AnalyticsViewModel
import com.youtube.rating.shared.models.UsageAnalyticsResponse
import com.youtube.rating.shared.models.UsageSummary
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit = {},
    viewModel: AnalyticsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RatingScaffold(
        topBar = {
            RatingTopAppBar(
                title = Strings.analyticsTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))


            Text(
                text = Strings.analyticsInfoText,
                textAlign = TextAlign.Left,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("week", "month", "year").forEach { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { viewModel.setSelectedPeriod(period) },
                        label = {
                            Text(
                                when (period) {
                                    "week" -> Strings.periodWeek
                                    "month" -> Strings.periodMonth
                                    "year" -> Strings.periodYear
                                    else -> period
                                }
                            )
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            when (val result = uiState.result) {
                is ResultState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ResultState.Error -> {
                    RatingCalloutCard(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        title = Strings.error,
                        body = result.message,
                        toneSurfaceVariant = false,
                    )
                }
                is ResultState.Success -> {
                    result.data?.usageSummary?.let { summary ->
                        UsageSummaryCard(summary = summary)
                    }

                    result.data?.usageAnalytics?.let { analytics ->
                        UsageAnalyticsCard(analytics = analytics)
                    }

                    if (result.data?.usageSummary == null && result.data?.usageAnalytics == null) {
                        RatingEmptyState(
                            title = Strings.noUsageData,
                            body = Strings.dataWillAppear,
                            icon = {
                                Icon(
                                    Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UsageSummaryCard(summary: UsageSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    Strings.usageSummary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.AccessTime,
                    value = formatDuration(summary.totalDuration),
                    label = Strings.totalTime
                )
                StatItem(
                    icon = Icons.Default.DateRange,
                    value = summary.totalSessions.toString(),
                    label = Strings.sessions
                )
                StatItem(
                    icon = Icons.Default.BarChart,
                    value = "${summary.averageDailySessions}",
                    label = Strings.averageDaily
                )
            }

            Text(
                text = Strings.averageDailySessions(summary.averageDailySessions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            summary.retentionRate?.let { retentionRate ->
                Text(
                    Strings.retentionRateText(retentionRate.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UsageAnalyticsCard(analytics: UsageAnalyticsResponse) {
    val data = analytics.data ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    Strings.detailedAnalytics,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
            }

            // Daily stats
            if (data.dailyStats.isNotEmpty()) {
                val latestStats = data.dailyStats.last()
                Text(
                    Strings.dailyStats,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = latestStats.totalSessions.toString(),
                        label = Strings.sessions
                    )
                    StatItem(
                        value = formatDuration(latestStats.totalDuration),
                        label = Strings.time
                    )
                    StatItem(
                        value = formatDuration(latestStats.averageSessionDuration),
                        label = Strings.average
                    )
                }
            }

            // Tab usage statistics
            if (data.tabUsageStats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    Strings.timeByTabs,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                data.tabUsageStats.sortedByDescending { it.totalTime }.take(5).forEach { tab ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                formatTabName(tabName = tab.tabName),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                Strings.visitsAndAverage(
                                    tab.visitCount,
                                    formatDuration(tab.averageTime * 1000)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            formatDuration(tab.totalTime * 1000),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatTabName(tabName: String): String {
    return Strings.tabName(tabName)
}
