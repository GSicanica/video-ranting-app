package com.youtube.rating.running.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.youtube.rating.android.feature.running.ui.RunningScreen

const val RUNNING_ROUTE = "running"

fun NavController.navigateToRunningFeature() {
    navigate(RUNNING_ROUTE) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.runningScreen(onBack: () -> Unit) {
    composable(RUNNING_ROUTE) {
        RunningScreen(onBack = onBack)
    }
}
