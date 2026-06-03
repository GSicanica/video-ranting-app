package com.youtube.rating.android.utils

/**
 * Result of backup operation
 */
sealed class BackupResult {
    data class Success(val filePath: String, val fileSize: Long) : BackupResult()
    data class Failed(val error: String) : BackupResult()
}

/**
 * Result of restore operation
 */
sealed class RestoreResult {
    data class Success(val stats: RestoreStats) : RestoreResult()
    data class Failed(val error: String) : RestoreResult()
}

/**
 * Statistics about restored data
 */
data class RestoreStats(
    var notesRestored: Int = 0,
    var favoritesRestored: Int = 0,
    var offlineVideosRestored: Int = 0,
    var rosarySessionsRestored: Int = 0,
    var tasksRestored: Int = 0,
    var fastingRestored: Int = 0,
    var watchHistoryRestored: Int = 0
) {
    val totalRestored: Int
        get() = notesRestored + favoritesRestored + offlineVideosRestored + rosarySessionsRestored + tasksRestored + fastingRestored + watchHistoryRestored

    override fun toString(): String {
        return "Notes: $notesRestored, Favorites: $favoritesRestored, Offline Videos: $offlineVideosRestored, Rosary Sessions: $rosarySessionsRestored, Tasks: $tasksRestored, Fasting: $fastingRestored, Watch History: $watchHistoryRestored (Total: $totalRestored)"
    }
}

/**
 * Information about an auto-backup
 */
data class AutoBackupInfo(
    val filePath: String,
    val fileName: String,
    val timestamp: Long,
    val size: Long
) {
    fun getFormattedDate(): String {
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }

    fun getFormattedSize(): String {
        if (size < 1024) return "$size B"

        val units = listOf("KB", "MB", "GB", "TB")
        var value = size.toDouble() / 1024.0
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return "%.1f %s".format(java.util.Locale.getDefault(), value, units[unitIndex])
    }

    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days dan${if (days > 1) "a" else ""} prije"
            hours > 0 -> "$hours sat${if (hours > 1) "i" else ""} prije"
            minutes > 0 -> "$minutes min prije"
            else -> "Upravo"
        }
    }
}
