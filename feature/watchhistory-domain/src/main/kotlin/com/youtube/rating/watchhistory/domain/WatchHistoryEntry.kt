package com.youtube.rating.watchhistory.domain

data class WatchHistoryEntry(
    val id: Int? = null,
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val category: String? = null,
    val source: String = "home",
    val watchDuration: Long = 0L,
    val totalDuration: Long = 0L,
    val viewedAt: Long = System.currentTimeMillis()
)
