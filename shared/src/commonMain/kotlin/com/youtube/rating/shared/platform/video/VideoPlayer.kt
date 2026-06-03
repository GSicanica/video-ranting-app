package com.youtube.rating.shared.platform.video

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Platform interface for video playback
 */
interface PlatformVideoPlayer {
    fun initialize(videoUrl: String): Boolean
    fun play()
    fun pause()
    fun stop()
    fun seek(seconds: Double)
    
    fun getDuration(): Double
    fun getCurrentTime(): Double
    fun setVolume(volume: Float)
    fun getVolume(): Float
    fun setRate(rate: Float)
    fun getRate(): Float
    
    fun getNetworkQuality(): String
    fun observePlaybackStats(): Flow<PlaybackStats>
}

/**
 * Shared video player implementation
 */
class VideoPlayer(private val platformPlayer: PlatformVideoPlayer) {
    
    private val _playbackStats = MutableStateFlow<PlaybackStats?>(null)
    val playbackStats = _playbackStats.asStateFlow()
    
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.IDLE)
    val playerState = _playerState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var isInitialized = false
    
    init {
        // Observe platform player stats
        scope.launch {
            platformPlayer.observePlaybackStats().collect { stats ->
                _playbackStats.value = stats
                updatePlayerState(stats)
            }
        }
    }
    
    fun initialize(videoUrl: String): Boolean {
        return platformPlayer.initialize(videoUrl).also { success ->
            isInitialized = success
            if (success) {
                _playerState.value = PlayerState.READY
            } else {
                _playerState.value = PlayerState.ERROR
            }
        }
    }
    
    fun play() {
        if (!isInitialized) return
        platformPlayer.play()
        _playerState.value = PlayerState.PLAYING
    }
    
    fun pause() {
        if (!isInitialized) return
        platformPlayer.pause()
        _playerState.value = PlayerState.PAUSED
    }
    
    fun stop() {
        platformPlayer.stop()
        isInitialized = false
        _playerState.value = PlayerState.IDLE
    }

    fun dispose() {
        scope.cancel()
    }
    
    fun seek(seconds: Double) {
        if (!isInitialized) return
        platformPlayer.seek(max(0.0, min(getDuration(), seconds)))
    }
    
    fun getDuration(): Double = platformPlayer.getDuration()
    fun getCurrentTime(): Double = platformPlayer.getCurrentTime()
    
    fun setVolume(volume: Float) {
        platformPlayer.setVolume(max(0f, min(1f, volume)))
    }
    
    fun getVolume(): Float = platformPlayer.getVolume()
    
    fun setRate(rate: Float) {
        platformPlayer.setRate(max(0.25f, min(2f, rate)))
    }
    
    fun getRate(): Float = platformPlayer.getRate()
    
    fun getNetworkQuality(): NetworkQuality {
        return when (platformPlayer.getNetworkQuality()) {
            "poor" -> NetworkQuality.POOR
            "fair" -> NetworkQuality.FAIR
            "good" -> NetworkQuality.GOOD
            "excellent" -> NetworkQuality.EXCELLENT
            else -> NetworkQuality.UNKNOWN
        }
    }
    
    fun getProgress(): Double {
        val current = getCurrentTime()
        val duration = getDuration()
        return if (duration > 0) current / duration else 0.0
    }
    
    fun getRemainingTime(): Double {
        return getDuration() - getCurrentTime()
    }
    
    fun isBuffered(percentage: Double): Boolean {
        val stats = playbackStats.value ?: return false
        return stats.bufferedDuration >= (stats.duration * percentage)
    }
    
    private fun updatePlayerState(stats: PlaybackStats) {
        if (stats.isPlaying && _playerState.value != PlayerState.PLAYING) {
            _playerState.value = PlayerState.PLAYING
        } else if (!stats.isPlaying && _playerState.value == PlayerState.PLAYING) {
            _playerState.value = PlayerState.PAUSED
        }
    }
}

/**
 * Playback statistics
 */
data class PlaybackStats(
    val duration: Double,
    val currentTime: Double,
    val bufferedDuration: Double,
    val isPlaying: Boolean,
)

/**
 * Player state enumeration
 */
enum class PlayerState {
    IDLE,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    SEEKING,
    ERROR,
}

/**
 * Network quality enumeration
 */
enum class NetworkQuality {
    POOR,           // < 500 kbps
    FAIR,           // 500 kbps - 1 mbps
    GOOD,           // 1 mbps - 5 mbps
    EXCELLENT,      // > 5 mbps
    UNKNOWN,
}
