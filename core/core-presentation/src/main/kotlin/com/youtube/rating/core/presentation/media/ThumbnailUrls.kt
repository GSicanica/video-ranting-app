package com.youtube.rating.core.presentation.media

object ThumbnailUrls {
    fun youtubeMaxRes(videoId: String): String =
        "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"

    fun youtubeCandidates(videoId: String): List<String> =
        listOf(
            "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/sddefault.jpg",
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/default.jpg"
        )
}
