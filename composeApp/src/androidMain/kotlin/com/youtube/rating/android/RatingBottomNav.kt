package com.youtube.rating.android

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.navigation.Screen

@Composable
internal fun RatingBottomNavigationBar(
    isInPipMode: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    callsTabEnabled: Boolean,
    onHomeReClick: () -> Unit
) {
    if (isInPipMode) return

    val selectedHome = currentRoute == Screen.Home.route || currentRoute?.startsWith("rate/") == true
    val selectedPrayer = currentRoute == Screen.Prayer.route
    val selectedTraining = currentRoute == Screen.Training.route ||
        currentRoute == Screen.Bible.route ||
        (currentRoute?.startsWith("bible/") == true) ||
        (currentRoute?.startsWith("reader/") == true)
    val selectedCalls = callsTabEnabled && currentRoute == Screen.Calls.route
    val selectedFavorites = currentRoute == Screen.Favorites.route

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
        indicatorColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .height(56.dp),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 3.dp
    ) {
        NavigationBarItem(
            colors = navItemColors,
            icon = { Icon(Icons.Default.Home, modifier = Modifier.size(20.dp), contentDescription = Strings.home) },
            label = { NavLabel(text = Strings.home, selected = selectedHome) },
            selected = selectedHome,
            onClick = {
                if (selectedHome) onHomeReClick()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        )
        NavigationBarItem(
            colors = navItemColors,
            icon = { Icon(Icons.Default.FavoriteBorder, modifier = Modifier.size(20.dp), contentDescription = Strings.prayer) },
            label = { NavLabel(text = Strings.prayer, selected = selectedPrayer) },
            selected = selectedPrayer,
            onClick = {
                navController.navigate(Screen.Prayer.route) { launchSingleTop = true }
            }
        )
        NavigationBarItem(
            colors = navItemColors,
            icon = { Icon(Icons.Default.FitnessCenter, modifier = Modifier.size(20.dp), contentDescription = "Trening") },
            label = { NavLabel(text = "Trening", selected = selectedTraining) },
            selected = selectedTraining,
            onClick = {
                navController.navigate(Screen.Bible.route) { launchSingleTop = true }
            }
        )
        if (callsTabEnabled) {
            NavigationBarItem(
                colors = navItemColors,
                icon = { Icon(Icons.Default.VideoCall, modifier = Modifier.size(20.dp), contentDescription = "Pozivi") },
                label = { NavLabel(text = "Pozivi", selected = selectedCalls) },
                selected = selectedCalls,
                onClick = {
                    navController.navigate(Screen.Calls.route) { launchSingleTop = true }
                }
            )
        }
        NavigationBarItem(
            colors = navItemColors,
            icon = { Icon(Icons.Default.Bookmark, modifier = Modifier.size(20.dp), contentDescription = Strings.favorites) },
            label = { NavLabel(text = Strings.favorites, selected = selectedFavorites) },
            selected = selectedFavorites,
            onClick = {
                navController.navigate(Screen.Favorites.route) { launchSingleTop = true }
            }
        )
    }
}

@Composable
private fun NavLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
    )
}
