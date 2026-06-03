package com.youtube.rating.habittracker.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.youtube.rating.android.ui.screens.HabitTrackerLegacyScreen

const val HABIT_TRACKER_ROUTE = "habit_tracker"
const val MONTHLY_HABIT_TRACKER_ROUTE = "habit_tracker/monthly"

fun NavController.navigateToHabitTrackerFeature() {
    navigate(MONTHLY_HABIT_TRACKER_ROUTE) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.habitTrackerScreen() {
    composable(HABIT_TRACKER_ROUTE) {
        HabitTrackerLegacyScreen()
    }
    composable(MONTHLY_HABIT_TRACKER_ROUTE) {
        HabitTrackerLegacyScreen()
    }
}
