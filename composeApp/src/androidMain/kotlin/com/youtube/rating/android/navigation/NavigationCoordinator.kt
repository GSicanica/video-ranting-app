package com.youtube.rating.android.navigation

import androidx.navigation.NavController

/**
 * Central coordinator for navigation operations
 * Provides high-level navigation methods and delegates to NavController extensions
 */
class NavigationCoordinator(private val navController: NavController) {

    // Bible-related navigation
    fun navigateToLocalBible() = navController.navigateToLocalBible()

    fun navigateToBiblePlanner() = navController.navigateToBiblePlanner()

    fun navigateToBibleStats() = navController.navigateToBibleStats()

    fun navigateToBibleBook(book: String) = navController.navigateToBibleBook(book)

    fun navigateToBibleChapter(book: String, chapter: String) = navController.navigateToBibleChapter(book, chapter)

    fun navigateToBiblePsalm(psalm: Int) = navController.navigateToBiblePsalm(psalm)

    fun navigateToBibleReader(book: String, chapter: Int, lang: String, line: Int? = null) =
        navController.navigateToBibleReader(book, chapter, lang, line)

    fun navigateToGospelDay(date: String? = null) = navController.navigateToGospelDay(date)

    // Main screens navigation
    fun navigateToHome() = navController.navigateToHome()

    fun navigateToPrayer() = navController.navigateToPrayer()

    fun navigateToFavorites() = navController.navigateToFavorites()

    fun navigateToNotes() = navController.navigateToNotes()

    fun navigateToAnalytics() = navController.navigateToAnalytics()

    fun navigateToSettings() = navController.navigateToSettings()
    fun navigateToHabitTracker() = navController.navigateToHabitTracker()
    fun navigateToRunningFeature() = navController.navigateToRunningFeature()
    fun navigateToKuiverGraph() = navController.navigateToKuiverGraph()

    // Media screens navigation
    fun navigateToRatedVideos() = navController.navigateToRatedVideos()

    fun navigateToOfflineVideos() = navController.navigateToOfflineVideos()

    fun navigateToGallery() = navController.navigateToGallery()

    // Other screens
    fun navigateToRosary() = navController.navigateToRosary()

    fun navigateToSaints() = navController.navigateToSaints()

    fun navigateToFasting() = navController.navigateToFasting()

    fun navigateToRateVideo(videoId: String) = navController.navigateToRateVideo(videoId)

    // Common navigation actions
    fun navigateUp() = navController.navigateUp()

    fun navigateBack() = navController.navigateUp()

    fun popBackStack() = navController.popBackStack()

    fun popBackStack(route: String, inclusive: Boolean = false) {
        navController.popBackStack(route, inclusive)
    }

    // Utility functions - delegate to extensions
    fun isCurrentRoute(route: String) = navController.isCurrentRoute(route)

    fun isHomeRoute() = navController.isHomeRoute()
}
