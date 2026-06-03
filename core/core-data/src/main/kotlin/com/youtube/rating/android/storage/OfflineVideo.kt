package com.youtube.rating.android.storage

import org.json.JSONObject

/**
 * Represents a video that can be played offline
 */
data class OfflineVideo(
    val id: String,                    // Unique identifier
    val youtubeId: String?,            // YouTube video ID if downloaded from YouTube
    val title: String,
    val channelName: String,
    val localPath: String,             // Local file path
    val thumbnailPath: String?,        // Local thumbnail path
    val thumbnailUrl: String?,         // Remote thumbnail URL for YouTube videos
    val duration: Long,                // Duration in milliseconds
    val fileSize: Long,                // File size in bytes
    val isFromDevice: Boolean,         // True if added from device, false if downloaded
    val category: String?,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("youtubeId", youtubeId ?: "")
        put("title", title)
        put("channelName", channelName)
        put("localPath", localPath)
        put("thumbnailPath", thumbnailPath ?: "")
        put("thumbnailUrl", thumbnailUrl ?: "")
        put("duration", duration)
        put("fileSize", fileSize)
        put("isFromDevice", isFromDevice)
        put("category", category ?: "")
        put("addedAt", addedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): OfflineVideo = OfflineVideo(
            id = json.getString("id"),
            youtubeId = json.optString("youtubeId").ifBlank { null },
            title = json.getString("title"),
            channelName = json.optString("channelName", "Unknown"),
            localPath = json.getString("localPath"),
            thumbnailPath = json.optString("thumbnailPath").ifBlank { null },
            thumbnailUrl = json.optString("thumbnailUrl").ifBlank { null },
            duration = json.optLong("duration", 0L),
            fileSize = json.optLong("fileSize", 0L),
            isFromDevice = json.optBoolean("isFromDevice", true),
            category = json.optString("category").ifBlank { null },
            addedAt = json.optLong("addedAt", System.currentTimeMillis())
        )
    }

    fun getThumbnail(): String? = thumbnailPath ?: thumbnailUrl

    fun getFormattedDuration(): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun getFormattedFileSize(): String {
        val kb = fileSize / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}
