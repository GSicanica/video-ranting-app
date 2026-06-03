package com.youtube.rating.android.domain.usecase

import com.youtube.rating.shared.models.VideoSearchResult

class ShuffleVideosUseCase {
    fun execute(list: List<VideoSearchResult>): List<VideoSearchResult> {
        if (list.size <= 1) return list
        if (list.size == 2) return listOf(list[1], list[0])

        val ids = list.map { it.videoId }
        var shuffled = list.shuffled()
        repeat(3) {
            if (shuffled.map { it.videoId } != ids) return shuffled
            shuffled = list.shuffled()
        }
        return shuffled
    }
}

