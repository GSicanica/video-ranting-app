package com.youtube.rating.android.navigation

import android.net.Uri
import com.youtube.rating.habittracker.presentation.navigation.HABIT_TRACKER_ROUTE
import com.youtube.rating.habittracker.presentation.navigation.MONTHLY_HABIT_TRACKER_ROUTE
import com.youtube.rating.running.presentation.navigation.RUNNING_ROUTE

/**
 * Navigation routes for the app
 * Uses sealed class for type safety and named navigation
 */
sealed class Screen(val route: String) {

    // Navigation categories for better organization
    enum class Category {
        HOME,
        BIBLE,
        MAIN,
        MEDIA,
        SPECIAL
    }

    // Home screen
    object Home : Screen(route = "home") {
        override val category = Category.HOME
    }

    // Bible-related screens
    object Bible : Screen(route = "bible") {
        override val category = Category.BIBLE
    }

    object LocalBible : Screen(route = "bible/local") {
        override val category = Category.BIBLE
    }

    object BiblePlanner : Screen(route = "bible/planner") {
        override val category = Category.BIBLE
    }

    object Training : Screen(route = "training") {
        override val category = Category.BIBLE
    }

    object BibleStats : Screen(route = "bible/stats") {
        override val category = Category.BIBLE
    }

    object LocalBibleBook : Screen(route = "bible/local/{book}") {
        override val category = Category.BIBLE
        fun createRoute(book: String) = "bible/local/${Uri.encode(book)}"
    }

    object BiblePsalm : Screen(route = "bible/psalm/{psalm}") {
        override val category = Category.BIBLE
        fun createRoute(psalm: Int) = "bible/psalm/$psalm"
    }

    object Reader : Screen("reader/{type}?book={book}&chapter={chapter}&lang={lang}&line={line}&date={date}") {
        override val category = Category.BIBLE
        const val TYPE_BIBLE = "bible"
        const val TYPE_GOSPEL = "gospel"
        const val TYPE_LOCAL = "local"

        fun createBibleRoute(book: String, chapter: Int, lang: String, line: Int? = null): String {
            val params = mutableListOf(
                "book=${Uri.encode(book)}",
                "chapter=$chapter",
                "lang=${Uri.encode(lang)}"
            )
            if (line != null) params += "line=$line"
            return "reader/$TYPE_BIBLE?${params.joinToString("&")}"
        }

        fun createLocalRoute(book: String, chapter: String, line: Int? = null): String {
            val params = mutableListOf(
                "book=${Uri.encode(book)}",
                "chapter=${Uri.encode(chapter)}"
            )
            if (line != null) params += "line=$line"
            return "reader/$TYPE_LOCAL?${params.joinToString("&")}"
        }

        fun createGospelRoute(date: String? = null): String {
            val base = "reader/$TYPE_GOSPEL"
            return if (date.isNullOrBlank()) base else "$base?date=${Uri.encode(date)}"
        }
    }

    // Main screens
    object Prayer : Screen(route = "prayer") {
        override val category = Category.MAIN
    }

    object Favorites : Screen(route = "favorites") {
        override val category = Category.MAIN
    }

    object Notes : Screen(route = "notes") {
        override val category = Category.MAIN
    }

    object Calls : Screen(route = "calls") {
        override val category = Category.MAIN
    }

    object Analytics : Screen(route = "analytics") {
        override val category = Category.MAIN
    }

    object Settings : Screen(route = "settings") {
        override val category = Category.MAIN
    }

    object HabitTracker : Screen(route = HABIT_TRACKER_ROUTE) {
        override val category = Category.MAIN
    }

    object MonthlyHabitTracker : Screen(route = MONTHLY_HABIT_TRACKER_ROUTE) {
        override val category = Category.MAIN
    }

    object Running : Screen(route = RUNNING_ROUTE) {
        override val category = Category.MAIN
    }

    object KuiverGraph : Screen(route = "feature/kuiver") {
        override val category = Category.MAIN
    }

    // Media screens
    object Rosary : Screen(route = "rosary") {
        override val category = Category.SPECIAL
    }

    object Saints : Screen(route = "saints") {
        override val category = Category.SPECIAL
    }

    object RatedVideos : Screen(route = "rated") {
        override val category = Category.MEDIA
    }

    object WatchHistory : Screen(route = "watch_history") {
        override val category = Category.MEDIA
    }

    object Fasting : Screen(route = "fasting") {
        override val category = Category.SPECIAL
    }

    object OfflineVideos : Screen(route = "offline") {
        override val category = Category.MEDIA
    }

    object Gallery : Screen(route = "gallery") {
        override val category = Category.MEDIA
    }

    // Deep link navigation for rating specific video
    object RateVideo : Screen("rate/{videoId}?start={start}") {
        override val category = Category.SPECIAL
        fun createRoute(videoId: String, startSeconds: Int? = null): String {
            val base = "rate/$videoId"
            val start = startSeconds?.takeIf { it > 0 } ?: return base
            return "$base?start=$start"
        }
    }

    // Abstract property for categorization
    abstract val category: Category

    companion object {
        // Named constants for common routes
        const val HOME_ROUTE = "home"
        const val LOCAL_BIBLE_ROUTE = "bible/local"
        const val PRAYER_ROUTE = "prayer"
        const val FAVORITES_ROUTE = "favorites"
        const val SETTINGS_ROUTE = "settings"
        const val RATED_VIDEOS_ROUTE = "rated"
        const val GALLERY_ROUTE = "gallery"
    }
}
