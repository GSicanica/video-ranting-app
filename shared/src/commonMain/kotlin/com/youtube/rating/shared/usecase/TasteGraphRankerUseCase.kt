package com.youtube.rating.shared.usecase

import com.youtube.rating.shared.models.VideoSearchResult

/**
 * Client-side post-ranking to keep ordering stable and deterministic across clients.
 */
class TasteGraphRankerUseCase {
    operator fun invoke(videos: List<VideoSearchResult>, limit: Int = 50): List<VideoSearchResult> {
        if (videos.isEmpty()) return emptyList()

        return videos
            .distinctBy { it.videoId }
            .sortedWith(
                compareByDescending<VideoSearchResult> { score(it) }
                    .thenByDescending { it.totalRatings }
                    .thenByDescending { it.createdAt }
            )
            .take(limit.coerceAtLeast(1))
    }

    private fun score(video: VideoSearchResult): Double {
        val avg = (video.avgTotal / 3.0).coerceIn(0.0, 1.0)
        val social = (video.totalRatings.toDouble() / 100.0).coerceIn(0.0, 1.0)
        return (0.72 * avg) + (0.28 * social)
    }
}

