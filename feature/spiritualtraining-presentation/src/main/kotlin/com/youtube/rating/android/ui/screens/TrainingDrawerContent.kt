package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.localization.Strings

@Composable
fun TrainingRightDrawerContent(
    pinned: Set<String>,
    readCount: Int,
    thinkingMin: Int,
    psalmsDone: Int,
    psalmsTotal: Int,
    sequentialSubtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Označeno",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (pinned.isEmpty()) {
            Text(
                text = "Označi kartice u Treningu da se pojave ovdje.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            return
        }

        if (TrainingPinnedItems.READING in pinned) {
            TrainingPinnedCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = Strings.reading,
                subtitle = Strings.todayCount(readCount),
            )
        }
        if (TrainingPinnedItems.MEDITATION in pinned) {
            TrainingPinnedCard(
                icon = Icons.Default.SelfImprovement,
                title = Strings.meditationTitle,
                subtitle = Strings.meditationSubtitle(thinkingMin),
            )
        }
        if (TrainingPinnedItems.PSALMS in pinned) {
            TrainingPinnedCard(
                icon = Icons.Default.FavoriteBorder,
                title = Strings.dailyPsalmsTitle,
                subtitle = Strings.dailyPsalmsSubtitle(psalmsDone, psalmsTotal),
            )
        }
        if (TrainingPinnedItems.SEQUENTIAL in pinned) {
            TrainingPinnedCard(
                icon = Icons.Default.AutoStories,
                title = Strings.sequentialReading,
                subtitle = sequentialSubtitle,
            )
        }
        if (TrainingPinnedItems.SEARCH in pinned) {
            TrainingPinnedCard(
                icon = Icons.Default.Search,
                title = "Pretraga Biblije",
                subtitle = "Brzi pristup pretrazi",
            )
        }
    }
}

@Composable
private fun TrainingPinnedCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

