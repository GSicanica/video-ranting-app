package com.youtube.rating.android.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.youtube.rating.habittracker.feature.habits.presentation.navigation.HabitsNavHost
import com.youtube.rating.habittracker.ui.theme.HabitTrackerTheme

@Composable
fun HabitTrackerLegacyScreen() {
    HabitTrackerTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val navController = rememberNavController()
            HabitsNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
