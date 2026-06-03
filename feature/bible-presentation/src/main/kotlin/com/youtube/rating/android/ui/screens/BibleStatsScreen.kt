package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.data.prefs.BibleStatsPrefs
import com.youtube.rating.core.designsystem.components.RatingTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleStatsScreen(onBack: () -> Unit) {

    val context = LocalContext.current

    val totalRead by BibleStatsPrefs.bibleTotalReadFlow(context)
        .collectAsStateWithLifecycle( 0)

    val totalMeditation by BibleStatsPrefs.bibleTotalMeditationFlow(context)
        .collectAsStateWithLifecycle( 0)

    val bestStreak by BibleStatsPrefs.bibleBestStreakFlow(context)
        .collectAsStateWithLifecycle( 0)

    val activeDays by BibleStatsPrefs.bibleDaysActiveFlow(context)
        .collectAsStateWithLifecycle( 0)

    Scaffold(
        topBar = {
            RatingTopAppBar(
                title = Strings.bibleStatsTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            StatCard(title = "📘 ${Strings.bibleTotalReadings}", value = "$totalRead poglavlja")
            StatCard(title = "🧠 ${Strings.bibleMeditation}", value = "$totalMeditation min")
            StatCard(title = "🔥 ${Strings.bibleLongestStreak}", value = "$bestStreak dana")
            StatCard(title = "📅 ${Strings.bibleActiveDays}", value = "$activeDays")

        }
    }
}

@Composable
private fun StatCard(title: String, value: String) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.padding(16.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
