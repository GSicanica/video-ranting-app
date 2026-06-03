package com.youtube.rating.habittracker.feature.habits.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.youtube.rating.habittracker.feature.habits.presentation.today.MonthlyTrackerScreenRoot

@Composable
fun HabitsNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Monthly,
        modifier = modifier,
    ) {
        composable<Route.Monthly> {
            MonthlyTrackerScreenRoot()
        }
    }
}
