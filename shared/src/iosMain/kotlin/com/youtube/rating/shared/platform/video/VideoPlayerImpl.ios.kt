package com.youtube.rating.shared.platform.video

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlatformVideoPlayerImpl : PlatformVideoPlayer {
    private val stats = MutableStateFlow(
        PlaybackStats(
            duration = 0.0,
            currentTime = 0.0,
            bufferedDuration = 0.0,
            isPlaying = false
        )
    )

    private var volume = 1f
    private var rate = 1f

    override fun initialize(videoUrl: String): Boolean = videoUrl.isNotBlank()

    override fun play() {
        stats.value = stats.value.copy(isPlaying = true)
    }

    override fun pause() {
        stats.value = stats.value.copy(isPlaying = false)
    }

    override fun stop() {
        stats.value = stats.value.copy(currentTime = 0.0, isPlaying = false)
    }

    override fun seek(seconds: Double) {
        stats.value = stats.value.copy(currentTime = seconds)
    }

    override fun getDuration(): Double = stats.value.duration

    override fun getCurrentTime(): Double = stats.value.currentTime

    override fun setVolume(volume: Float) {
        this.volume = volume
    }

    override fun getVolume(): Float = volume

    override fun setRate(rate: Float) {
        this.rate = rate
    }

    override fun getRate(): Float = rate

    override fun getNetworkQuality(): String = "unknown"

    override fun observePlaybackStats(): Flow<PlaybackStats> = stats.asStateFlow()
}
