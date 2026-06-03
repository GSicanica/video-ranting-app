package com.youtube.rating.habittracker.feature.habits.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Monthly : Route
}
