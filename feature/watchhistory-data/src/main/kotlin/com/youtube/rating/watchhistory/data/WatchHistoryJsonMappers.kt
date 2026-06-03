package com.youtube.rating.watchhistory.data

import com.youtube.rating.watchhistory.domain.WatchHistoryEntry
import org.json.JSONObject

internal fun WatchHistoryEntry.toJson(): JSONObject = JSONObject().apply {
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

internal fun JSONObject.toWatchHistoryEntry(): WatchHistoryEntry = WatchHistoryEntry(
    id = if (has("id")) getInt("id") else null,
    videoId = getString("videoId"),
    title = getString("title"),
    thumbnail = getString("thumbnail"),
    channelName = getString("channelName"),
    category = optString("category").takeIf { it.isNotEmpty() },
    source = optString("source", "home"),
    watchDuration = optLong("watchDuration", 0L),
    totalDuration = optLong("totalDuration", 0L),
    viewedAt = getLong("viewedAt")
)
