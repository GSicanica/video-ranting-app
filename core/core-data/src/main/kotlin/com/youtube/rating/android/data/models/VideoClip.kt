package com.youtube.rating.android.data.models

import kotlinx.serialization.Serializable

@Serializable
data class VideoClip(
    val id: String,
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val startSeconds: Int,
    val endSeconds: Int,
    val createdAt: Long
) {
    val durationSeconds: Int
        get() = (endSeconds - startSeconds).coerceAtLeast(0)
}
