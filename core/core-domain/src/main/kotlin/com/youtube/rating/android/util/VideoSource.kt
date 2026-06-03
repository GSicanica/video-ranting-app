package com.youtube.rating.android.util

/**
 * Unified source for pasted video links.
 */
sealed class VideoSource {
    data class YouTube(val videoId: String) : VideoSource()
    data class Facebook(
        val resolvedUrl: String,
        val videoId: String? = null,
        val originalUrl: String
    ) : VideoSource()

    data class Unknown(val reason: String? = null) : VideoSource()
}
