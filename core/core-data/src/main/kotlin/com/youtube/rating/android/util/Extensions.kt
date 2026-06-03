package com.youtube.rating.android.util

import android.content.Context
import com.youtube.rating.android.util.StorageConstants.BYTES_PER_GB
import com.youtube.rating.android.util.StorageConstants.BYTES_PER_KB
import com.youtube.rating.android.util.StorageConstants.BYTES_PER_MB
import com.youtube.rating.android.util.VideoConstants.MS_PER_DAY
import com.youtube.rating.android.util.VideoConstants.MS_PER_HOUR
import com.youtube.rating.android.util.VideoConstants.MS_PER_MINUTE
import com.youtube.rating.android.util.VideoConstants.MS_PER_SECOND
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions for common operations
 * Makes code more readable and reduces duplication
 */

// ========== STRING EXTENSIONS ==========

/**
 * Check if string is a valid YouTube video ID
 */
fun String.isValidYouTubeVideoId(): Boolean {
    return this.length == VideoConstants.YOUTUBE_VIDEO_ID_LENGTH && this.all { it.isLetterOrDigit() || it == '_' || it == '-' }
}

/**
 * Extract YouTube video ID from URL
 */
fun String.extractYouTubeVideoId(): String? {
    return when {
        contains(VideoConstants.YOUTUBE_SHORT_URL) -> 
            substringAfter(VideoConstants.YOUTUBE_SHORT_URL).substringBefore("?")
        contains(VideoConstants.YOUTUBE_WATCH_URL) -> 
            substringAfter("v=").substringBefore("&")
        isValidYouTubeVideoId() -> this
        else -> null
    }
}

/**
 * Validate note title length
 */
fun String.isValidNoteTitleLength(): Boolean {
    return this.length in NoteConstants.MIN_TITLE_LENGTH..NoteConstants.MAX_TITLE_LENGTH
}

/**
 * Validate note content length
 */
fun String.isValidNoteContentLength(): Boolean {
    return this.length <= NoteConstants.MAX_CONTENT_LENGTH
}

/**
 * Truncate string to max length with ellipsis
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    return if (this.length <= maxLength) this 
    else "${this.take(maxLength - ellipsis.length)}$ellipsis"
}

// ========== INT/LONG EXTENSIONS ==========

/**
 * Check if rating is valid (1-3)
 */
fun Int.isValidRating(): Boolean {
    return this in RatingConstants.MIN_RATING..RatingConstants.MAX_RATING
}

/**
 * Format milliseconds to human-readable duration
 * e.g., "2:34" or "1:23:45"
 */
fun Long.formatDuration(): String {
    val seconds = (this / MS_PER_SECOND).toInt()
    val minutes = seconds / VideoConstants.SECONDS_PER_MINUTE
    val hours = minutes / VideoConstants.MINUTES_PER_HOUR
    
    return when {
        hours > 0 -> String.format(
            "%d:%02d:%02d",
            hours,
            minutes % VideoConstants.MINUTES_PER_HOUR,
            seconds % VideoConstants.SECONDS_PER_MINUTE
        )
        else -> String.format(
            "%d:%02d",
            minutes,
            seconds % VideoConstants.SECONDS_PER_MINUTE
        )
    }
}

/**
 * Format timestamp to date string
 */
fun Long.formatDate(pattern: String = "dd.MM.yyyy HH:mm"): String {
    val date = Date(this)
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(date)
}

/**
 * Format timestamp to relative time
 * e.g., "prije 2 sata", "prije 3 dana"
 */
fun Long.formatRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < MS_PER_MINUTE -> "upravo sada"
        diff < MS_PER_HOUR -> {
            val minutes = (diff / MS_PER_MINUTE).toInt()
            "prije $minutes ${minutesWord(minutes)}"
        }
        diff < MS_PER_DAY -> {
            val hours = (diff / MS_PER_HOUR).toInt()
            "prije $hours ${hoursWord(hours)}"
        }
        diff < 7 * MS_PER_DAY -> {
            val days = (diff / MS_PER_DAY).toInt()
            "prije $days ${daysWord(days)}"
        }
        else -> formatDate("dd.MM.yyyy")
    }
}

/**
 * Format file size to human-readable string
 * e.g., "2.5 MB", "1.2 GB"
 */
fun Long.formatFileSize(): String {
    val bytes = this.toDouble()
    val kb = bytes / BYTES_PER_KB
    val mb = bytes / BYTES_PER_MB
    val gb = bytes / BYTES_PER_GB
    
    return when {
        gb >= StorageConstants.GB_THRESHOLD -> String.format(FormatConstants.DECIMAL_FORMAT_2 + " GB", gb)
        mb >= StorageConstants.MB_THRESHOLD -> String.format(FormatConstants.DECIMAL_FORMAT_1 + " MB", mb)
        kb >= StorageConstants.KB_THRESHOLD -> String.format(FormatConstants.DECIMAL_FORMAT_1 + " KB", kb)
        else -> "$this B"
    }
}

// ========== DOUBLE/FLOAT EXTENSIONS ==========

/**
 * Format rating to display string (1 decimal place)
 */
fun Double.formatRating(): String {
    return if (this > RatingConstants.DEFAULT_RATING) {
        String.format(FormatConstants.DECIMAL_FORMAT_1, this)
    } else {
        RatingConstants.DEFAULT_RATING.toString()
    }
}

/**
 * Convert rating to percentage (0-100%)
 */
fun Double.toRatingPercentage(): Int {
    return ((this / RatingConstants.MAX_RATING) * PerformanceConstants.MEMORY_PERCENT_MULTIPLIER).toInt()
}

/**
 * Check if rating is above threshold
 */
fun Double.isAboveThreshold(threshold: Double = 3.0): Boolean {
    return this >= threshold
}

// ========== COLLECTION EXTENSIONS ==========

/**
 * Calculate average of list of numbers
 */
fun List<Double>.average(): Double {
    return if (isEmpty()) RatingConstants.DEFAULT_RATING 
    else sum() / size
}

/**
 * Calculate average rating from three categories
 */
fun calculateOverallRating(love: Double, faith: Double, hope: Double): Double {
    return (love + faith + hope) / RatingConstants.RATING_CATEGORIES_COUNT
}

// ========== HELPER FUNCTIONS ==========

private fun minutesWord(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "minutu"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "minute"
        else -> "minuta"
    }
}

private fun hoursWord(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "sat"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "sata"
        else -> "sati"
    }
}

private fun daysWord(count: Int): String {
    return when {
        count == 1 -> "dan"
        count in 2..4 -> "dana"
        else -> "dana"
    }
}

// ========== CONTEXT EXTENSIONS ==========

/**
 * Check if device is online
 * ✅ FIXED: Added proper null safety handling
 */
fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        ?: return false  // ✅ Default to offline if service unavailable
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/**
 * Get available storage space in bytes
 */
fun Context.getAvailableStorageSpace(): Long {
    val stat = android.os.StatFs(filesDir.path)
    return stat.availableBlocksLong * stat.blockSizeLong
}

/**
 * Get total storage space in bytes
 */
fun Context.getTotalStorageSpace(): Long {
    val stat = android.os.StatFs(filesDir.path)
    return stat.blockCountLong * stat.blockSizeLong
}

// ========== VALIDATION EXTENSIONS ==========

/**
 * Validate rating object
 */
fun Triple<Int, Int, Int>.isValidRatingTriple(): Boolean {
    return first.isValidRating() && second.isValidRating() && third.isValidRating()
}

/**
 * Check if timestamp is recent (within last hour)
 */
fun Long.isRecent(): Boolean {
    val now = System.currentTimeMillis()
    return (now - this) < MS_PER_HOUR
}

/**
 * Check if cache is expired
 */
fun Long.isCacheExpired(duration: Long = CacheConstants.VIDEO_INFO_CACHE_DURATION): Boolean {
    val now = System.currentTimeMillis()
    return (now - this) > duration
}
