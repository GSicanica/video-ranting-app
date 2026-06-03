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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.youtube.rating.ioscomposeapp.screens.HomeScreen
import com.youtube.rating.ioscomposeapp.screens.NotesScreen
import com.youtube.rating.ioscomposeapp.screens.RateScreen
import com.youtube.rating.ioscomposeapp.screens.RatedScreen
import com.youtube.rating.ioscomposeapp.screens.SearchScreen
import com.youtube.rating.ioscomposeapp.screens.SettingsScreen
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

            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "YouTubeRating", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "(iOS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    when (selectedTab) {
                        IosTab.Home -> HomeScreen(api = api)
                        IosTab.Search -> SearchScreen(api = api)
                        IosTab.Rate -> RateScreen(api = api)
                        IosTab.Rated -> RatedScreen(api = api)
                        IosTab.Notes -> NotesScreen()
                        IosTab.Settings -> SettingsScreen(api = api)
                    }
                }

                HorizontalDivider()
                BottomNav(
                    selected = selectedTab,
                    onSelected = { selectedTab = it }
                )
            }
        }
    }
}

private enum class IosTab(val label: String) {
    Home("Home"),
    Search("Search"),
    Rate("Rate"),
    Rated("Rated"),
    Notes("Notes"),
    Settings("Settings")
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

@Composable
private fun SimpleScreen(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}
