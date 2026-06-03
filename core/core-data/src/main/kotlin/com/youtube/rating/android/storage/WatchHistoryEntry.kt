package com.youtube.rating.android.storage

import org.json.JSONObject

/**
 * Data class representing a single watch history entry
 */
data class WatchHistoryEntry(
    val id: Int? = null,
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val category: String? = null,
    val source: String = "home",  // home, history, favorites, search, other
    val watchDuration: Long = 0L,  // milliseconds
    val totalDuration: Long = 0L,   // milliseconds
    val viewedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to JSON for API requests
     */
    fun toJson(): JSONObject = JSONObject().apply {
        id?.let { put("id", it) }
        put("videoId", videoId)
        put("title", title)
        put("thumbnail", thumbnail)
        put("channelName", channelName)
        put("category", category ?: "")
        put("source", source)
        put("watchDuration", watchDuration)
        put("totalDuration", totalDuration)
        put("viewedAt", viewedAt)
    }

    companion object {
        /**
         * Parse from JSON response
         */
        fun fromJson(json: JSONObject): WatchHistoryEntry = WatchHistoryEntry(
            id = if (json.has("id")) json.getInt("id") else null,
            videoId = json.getString("videoId"),
            title = json.getString("title"),
            thumbnail = json.getString("thumbnail"),
            channelName = json.getString("channelName"),
            category = json.optString("category").takeIf { it.isNotEmpty() },
            source = json.optString("source", "home"),
            watchDuration = json.optLong("watchDuration", 0L),
            totalDuration = json.optLong("totalDuration", 0L),
            viewedAt = json.getLong("viewedAt")
        )
    }
}
