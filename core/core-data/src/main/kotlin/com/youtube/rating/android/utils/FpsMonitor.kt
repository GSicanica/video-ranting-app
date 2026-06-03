package com.youtube.rating.android.utils

import android.view.Choreographer
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time FPS (Frames Per Second) monitor using Choreographer
 */
object FpsMonitor {
    
    data class FpsStats(
        val currentFps: Int = 0,
        val avgFps: Int = 0,
        val droppedFrames: Int = 0,
        val avgFrameTime: Float = 0f,
        val maxFrameTime: Float = 0f
    )
    
    private val _fpsStats = MutableStateFlow(FpsStats())
    val fpsStats: StateFlow<FpsStats> = _fpsStats.asStateFlow()
    
    @Volatile
    private var isMonitoring = false
    @Volatile
    private var lastFrameTimeNanos = 0L
    private val frameTimes = mutableListOf<Float>()
    @Volatile
    private var droppedFramesCount = 0
    
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMonitoring) return
            
            if (lastFrameTimeNanos > 0) {
                val frameTimeMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                frameTimes.add(frameTimeMs)
                
                // Frame drop detection: 60fps = 16.67ms, 30fps = 33.33ms
                if (frameTimeMs > 16.67f * 2) {
                    droppedFramesCount++
                }
                
                // Keep last 120 frames (2 seconds at 60fps)
                if (frameTimes.size > 120) {
                    frameTimes.removeAt(0)
                }
                
                // Calculate stats
                val avgFrameTime = frameTimes.average().toFloat()
                val maxFrameTime = frameTimes.maxOrNull() ?: 0f
                val currentFps = if (frameTimeMs > 0) (1000f / frameTimeMs).toInt() else 0
                val avgFps = if (avgFrameTime > 0) (1000f / avgFrameTime).toInt() else 0
                
                _fpsStats.value = FpsStats(
                    currentFps = currentFps,
                    avgFps = avgFps,
                    droppedFrames = droppedFramesCount,
                    avgFrameTime = avgFrameTime,
                    maxFrameTime = maxFrameTime
                )
            }
            
            lastFrameTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    
    fun startMonitoring() {
        if (!isMonitoring) {
            if (BuildConfig.DEBUG) {
                Logger.debug("FpsMonitor", "Started")
            }
            isMonitoring = true
            lastFrameTimeNanos = 0L
            frameTimes.clear()
            droppedFramesCount = 0
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }
    
    fun stopMonitoring() {
        if (isMonitoring) {
            if (BuildConfig.DEBUG) {
                Logger.debug("FpsMonitor", "Stopped")
            }
            isMonitoring = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            _fpsStats.value = FpsStats()
        }
    }
    
    fun reset() {
        frameTimes.clear()
        droppedFramesCount = 0
        lastFrameTimeNanos = 0L
        _fpsStats.value = FpsStats()
        if (BuildConfig.DEBUG) {
            Logger.debug("FpsMonitor", "Stats reset")
        }
    }
    
    fun isMonitoring() = isMonitoring
    
    fun getFormattedStats(): String {
        val stats = _fpsStats.value
        return buildString {
            appendLine("📊 FPS Stats")
            appendLine("═══════════════════════")
            appendLine("Current: ${stats.currentFps} fps")
            appendLine("Average: ${stats.avgFps} fps")
            appendLine("Dropped Frames: ${stats.droppedFrames}")
            appendLine("Avg Frame Time: ${String.format("%.2f", stats.avgFrameTime)} ms")
            appendLine("Max Frame Time: ${String.format("%.2f", stats.maxFrameTime)} ms")
        }
    }
}
