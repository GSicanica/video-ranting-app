package com.youtube.rating.android.navigation

import androidx.navigation.NavController
import com.youtube.rating.habittracker.presentation.navigation.navigateToHabitTrackerFeature
import com.youtube.rating.running.presentation.navigation.navigateToRunningFeature as navigateToRunningFeatureEntry

// Extension functions for NavController - common navigation operations
fun NavController.navigateToHome() {
    navigate(Screen.Home.route) {
        popUpTo(Screen.Home.route) { inclusive = true }
    }
}

fun NavController.navigateToLocalBible() {
    navigate(Screen.LocalBible.route)
}

fun NavController.navigateToBiblePlanner() {
    navigate(Screen.BiblePlanner.route)
}

fun NavController.navigateToBibleStats() {
    navigate(Screen.BibleStats.route)
}

fun NavController.navigateToBibleBook(book: String) {
    navigate(Screen.LocalBibleBook.createRoute(book))
}

fun NavController.navigateToBibleChapter(book: String, chapter: String) {
    navigate(Screen.Reader.createLocalRoute(book, chapter))
}

fun NavController.navigateToBibleReader(
    book: String,
    chapter: Int,
    lang: String,
    line: Int? = null
) {
    navigate(Screen.Reader.createBibleRoute(book, chapter, lang, line))
}

fun NavController.navigateToGospelDay(date: String? = null) {
    navigate(Screen.Reader.createGospelRoute(date))
}

fun NavController.navigateToBiblePsalm(psalm: Int) {
    navigate(Screen.BiblePsalm.createRoute(psalm))
}

fun NavController.navigateToPrayer() {
    navigate(Screen.Prayer.route)
}

fun NavController.navigateToFavorites() {
    navigate(Screen.Favorites.route)
}

fun NavController.navigateToNotes() {
    navigate(Screen.Notes.route)
}

fun NavController.navigateToAnalytics() {
    navigate(Screen.Analytics.route)
}

fun NavController.navigateToSettings() {
    navigate(Screen.Settings.route)
}

fun NavController.navigateToHabitTracker() {
    navigateToHabitTrackerFeature()
}

fun NavController.navigateToRunningFeature() {
    navigateToRunningFeatureEntry()
}

fun NavController.navigateToKuiverGraph() {
    navigate(Screen.KuiverGraph.route)
}

fun NavController.navigateToRatedVideos() {
    navigate(Screen.RatedVideos.route)
}

fun NavController.navigateToOfflineVideos() {
    navigate(Screen.OfflineVideos.route)
}

fun NavController.navigateToGallery() {
    navigate(Screen.Gallery.route)
}

fun NavController.navigateToRosary() {
    navigate(Screen.Rosary.route)
}

fun NavController.navigateToSaints() {
    navigate(Screen.Saints.route)
}

fun NavController.navigateToFasting() {
    navigate(Screen.Fasting.route) {
        launchSingleTop = true
    }
}

fun NavController.navigateToRateVideo(videoId: String, startSeconds: Int? = null) {
    navigate(Screen.RateVideo.createRoute(videoId, startSeconds)) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Utility extensions
fun NavController.navigateUpSafely(): Boolean {
    return if (currentBackStackEntry?.destination?.route != Screen.Home.route) {
        navigateUp()
        true
    } else {
        false
    }
}

fun NavController.isCurrentRoute(route: String): Boolean {
    return currentBackStackEntry?.destination?.route == route
}

fun NavController.isHomeRoute(): Boolean {
    val currentRoute = currentBackStackEntry?.destination?.route
    return currentRoute == Screen.Home.route ||
           currentRoute == Screen.RateVideo.route ||
           currentRoute?.startsWith("rate/") == true
}
