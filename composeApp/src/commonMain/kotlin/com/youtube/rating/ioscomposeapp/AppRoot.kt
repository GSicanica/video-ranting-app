package com.youtube.rating.ioscomposeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.youtube.rating.ioscomposeapp.screens.FavoritesScreen
import com.youtube.rating.ioscomposeapp.screens.HomeScreen
import com.youtube.rating.ioscomposeapp.screens.NotesScreen
import com.youtube.rating.ioscomposeapp.screens.PrayerScreen
import com.youtube.rating.ioscomposeapp.screens.RateScreen
import com.youtube.rating.ioscomposeapp.screens.RatedScreen
import com.youtube.rating.ioscomposeapp.screens.SearchScreen
import com.youtube.rating.ioscomposeapp.screens.SettingsScreen
import com.youtube.rating.ioscomposeapp.screens.TrainingScreen
import com.youtube.rating.shared.api.RatingApiClient

@Composable
fun AppRoot() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            val api = remember { RatingApiClient() }
            var selectedTab by remember { mutableStateOf(IosTab.Home) }
            var panel by remember { mutableStateOf(IosPanel.Main) }
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "YouTubeRating", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "(iOS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = {
                            panel = if (panel == IosPanel.More) IosPanel.Main else IosPanel.More
                        }) {
                            Text(if (panel == IosPanel.More) "Back" else "More")
                        }
                    }

                    when (panel) {
                        IosPanel.Main -> {
                            when (selectedTab) {
                                IosTab.Home -> HomeScreen(api = api)
                                IosTab.Prayer -> PrayerScreen(api = api)
                                IosTab.Training -> TrainingScreen(api = api)
                                IosTab.Favorites -> FavoritesScreen(api = api)
                            }
                        }
                        IosPanel.More -> {
                            MorePanel(
                                onOpenSearch = { panel = IosPanel.Search },
                                onOpenRate = { panel = IosPanel.Rate },
                                onOpenRated = { panel = IosPanel.Rated },
                                onOpenNotes = { panel = IosPanel.Notes },
                                onOpenSettings = { panel = IosPanel.Settings }
                            )
                        }
                        IosPanel.Search -> SearchScreen(api = api)
                        IosPanel.Rate -> RateScreen(api = api)
                        IosPanel.Rated -> RatedScreen(api = api)
                        IosPanel.Notes -> NotesScreen()
                        IosPanel.Settings -> SettingsScreen(api = api)
                    }
                }

                HorizontalDivider()
                BottomNav(
                    selected = selectedTab,
                    onSelected = {
                        selectedTab = it
                        panel = IosPanel.Main
                    }
                )
            }
        }
    }
}

private enum class IosPanel {
    Main,
    More,
    Search,
    Rate,
    Rated,
    Notes,
    Settings
}

@Composable
private fun MorePanel(
    onOpenSearch: () -> Unit,
    onOpenRate: () -> Unit,
    onOpenRated: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("More", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenSearch) { Text("Search") }
            TextButton(onClick = onOpenRate) { Text("Rate") }
            TextButton(onClick = onOpenRated) { Text("Rated") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenNotes) { Text("Notes") }
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}

private enum class IosTab(val label: String) {
    Home("Home"),
    Prayer("Prayer"),
    Training("Training"),
    Favorites("Favorites")
}

@Composable
private fun BottomNav(
    selected: IosTab,
    onSelected: (IosTab) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        IosTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                label = { Text(tab.label) },
                icon = {},
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
