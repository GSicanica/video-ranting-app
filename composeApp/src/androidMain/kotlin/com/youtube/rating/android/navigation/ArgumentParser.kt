package com.youtube.rating.android.navigation

import android.net.Uri
import androidx.navigation.NavBackStackEntry

/**
 * Utility functions for parsing navigation arguments
 * Centralizes argument parsing logic to avoid duplication and improve testability
 */
object ArgumentParser {

    // Bible-related argument parsing
    fun parseBookArgument(backStackEntry: NavBackStackEntry): String {
        return Uri.decode(backStackEntry.arguments?.getString("book") ?: "")
    }

    fun parseChapterArgument(backStackEntry: NavBackStackEntry): String {
        return Uri.decode(backStackEntry.arguments?.getString("chapter") ?: "")
    }

    fun parsePsalmArgument(backStackEntry: NavBackStackEntry): Int {
        return backStackEntry.arguments?.getInt("psalm") ?: 0
    }

    // Video-related argument parsing
    fun parseVideoIdArgument(backStackEntry: NavBackStackEntry): String {
        return backStackEntry.arguments?.getString("videoId") ?: ""
    }

    fun parseVideoStartArgument(backStackEntry: NavBackStackEntry): Int {
        return backStackEntry.arguments?.getString("start")?.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    fun parseVideoTitleArgument(backStackEntry: NavBackStackEntry): String {
        return backStackEntry.arguments?.getString("videoTitle") ?: ""
    }

    // Combined parsing for complex routes
    data class BibleChapterArgs(val book: String, val chapter: String)

    fun parseBibleChapterArgs(backStackEntry: NavBackStackEntry): BibleChapterArgs {
        return BibleChapterArgs(
            book = parseBookArgument(backStackEntry),
            chapter = parseChapterArgument(backStackEntry)
        )
    }

    // Validation helpers
    fun validateBookArgument(book: String): Boolean {
        return book.isNotBlank()
    }

    fun validateChapterArgument(chapter: String): Boolean {
        return chapter.isNotBlank()
    }

    fun validateVideoIdArgument(videoId: String): Boolean {
        return videoId.isNotBlank()
    }

    fun validatePsalmArgument(psalm: Int): Boolean {
        return psalm > 0
    }
}
