package com.youtube.rating.android

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
internal fun RatingNavigationRail(
    isInPipMode: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    onHomeReClick: () -> Unit,
) {
    if (isInPipMode) return

    val selectedHome = currentRoute == Screen.Home.route || currentRoute?.startsWith("rate/") == true
    val selectedPrayer = currentRoute == Screen.Prayer.route
    val selectedTraining = currentRoute == Screen.Training.route ||
        currentRoute == Screen.Bible.route ||
        (currentRoute?.startsWith("bible/") == true) ||
        (currentRoute?.startsWith("reader/") == true)
    val selectedFavorites = currentRoute == Screen.Favorites.route

    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
        indicatorColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationRail(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        NavigationRailItem(
            colors = itemColors,
            icon = { Icon(Icons.Default.Home, modifier = Modifier.size(20.dp), contentDescription = Strings.home) },
            label = { RailLabel(text = Strings.home, selected = selectedHome) },
            selected = selectedHome,
            onClick = {
                if (selectedHome) onHomeReClick()
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        )

        NavigationRailItem(
            colors = itemColors,
            icon = { Icon(Icons.Default.FavoriteBorder, modifier = Modifier.size(20.dp), contentDescription = Strings.prayer) },
            label = { RailLabel(text = Strings.prayer, selected = selectedPrayer) },
            selected = selectedPrayer,
            onClick = { navController.navigate(Screen.Prayer.route) { launchSingleTop = true } }
        )

        NavigationRailItem(
            colors = itemColors,
            icon = { Icon(Icons.Default.FitnessCenter, modifier = Modifier.size(20.dp), contentDescription = "Trening") },
            label = { RailLabel(text = "Trening", selected = selectedTraining) },
            selected = selectedTraining,
            onClick = { navController.navigate(Screen.Bible.route) { launchSingleTop = true } }
        )

        NavigationRailItem(
            colors = itemColors,
            icon = { Icon(Icons.Default.Bookmark, modifier = Modifier.size(20.dp), contentDescription = Strings.favorites) },
            label = { RailLabel(text = Strings.favorites, selected = selectedFavorites) },
            selected = selectedFavorites,
            onClick = { navController.navigate(Screen.Favorites.route) { launchSingleTop = true } }
        )
    }
}

@Composable
private fun RailLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
    )
}
