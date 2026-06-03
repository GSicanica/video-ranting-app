package com.youtube.rating.android.utils

import android.os.Debug
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Memory profiler for tracking heap usage, GC events, and memory allocation
 * ✅ FIXED: Proper lifecycle management with cleanup method
 */
object MemoryProfiler {
    
    data class MemorySnapshot(
        val timestamp: Long,
        val heapUsed: Long,
        val heapFree: Long,
        val heapMax: Long,
        val nativeHeapUsed: Long,
        val nativeHeapMax: Long,
        val gcCount: Int
    )
    
    private val _snapshots = MutableStateFlow<List<MemorySnapshot>>(emptyList())
    val snapshots: StateFlow<List<MemorySnapshot>> = _snapshots.asStateFlow()
    
    // ✅ FIXED: Added @Volatile for thread visibility
    @Volatile
    private var isMonitoring = false
    
    @Volatile
    private var monitoringJob: Job? = null
    
    @Volatile
    private var gcCount = 0
    
    @Volatile
    private var lastSnapshot: MemorySnapshot? = null
    
    // ✅ FIXED: Reuse single CoroutineScope instead of creating new one each time
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    fun startMonitoring(intervalMs: Long = 1000) {
        if (isMonitoring) {
            Logger.debug("MemoryProfiler", "Already monitoring")
            return
        }
        
        Logger.info("MemoryProfiler", "Started (interval=${intervalMs}ms)")
        isMonitoring = true
        gcCount = 0
        
        // ✅ FIXED: Reuse scope instead of creating new one
        monitoringJob = scope.launch {
            while (isActive && isMonitoring) {
                takeSnapshot()
                delay(intervalMs)
            }
        }
    }
    
    fun stopMonitoring() {
        if (isMonitoring) {
            Logger.info("MemoryProfiler", "Stopped (snapshots=${_snapshots.value.size})")
            isMonitoring = false
            monitoringJob?.cancel()
            monitoringJob = null
        }
    }
    
    fun takeSnapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            heapUsed = runtime.totalMemory() - runtime.freeMemory(),
            heapFree = runtime.freeMemory(),
            heapMax = runtime.maxMemory(),
            nativeHeapUsed = Debug.getNativeHeapAllocatedSize(),
            nativeHeapMax = Debug.getNativeHeapSize(),
            gcCount = gcCount
        )
        
        lastSnapshot = snapshot
        
        // Keep last 1000 snapshots (max ~16 minutes at 1s interval)
        val currentSnapshots = _snapshots.value.toMutableList()
        currentSnapshots.add(snapshot)
        if (currentSnapshots.size > 1000) {
            currentSnapshots.removeAt(0)
        }
        _snapshots.value = currentSnapshots
        
        return snapshot
    }
    
    fun forceGc() {
        Logger.debug("MemoryProfiler", "Forcing GC")
        gcCount++
        Runtime.getRuntime().gc()
        System.gc()
        Logger.debug("MemoryProfiler", "GC completed")
    }
    
    fun getLastSnapshot(): MemorySnapshot? = lastSnapshot
    
    fun clearSnapshots() {
        _snapshots.value = emptyList()
        lastSnapshot = null
        gcCount = 0
        Logger.debug("MemoryProfiler", "Snapshots cleared")
    }
    
    fun isMonitoring() = isMonitoring
    
    /**
     * ✅ FIXED: Cleanup method to prevent memory leaks
     * Call this from Application.onTerminate()
     */
    fun cleanup() {
        stopMonitoring()
        scope.cancel()
        clearSnapshots()
        Logger.info("MemoryProfiler", "Cleaned up")
    }
    
    fun getFormattedStats(): String {
        val snapshot = lastSnapshot ?: return "No data"
        val heapPercent = (snapshot.heapUsed * 100.0 / snapshot.heapMax).toInt()
        val nativePercent = if (snapshot.nativeHeapMax > 0) {
            (snapshot.nativeHeapUsed * 100.0 / snapshot.nativeHeapMax).toInt()
        } else 0
        
        return buildString {
            appendLine("💾 Memory Stats")
            appendLine("═══════════════════════")
            appendLine("Heap Used: ${DeviceInfoHelper.formatBytes(snapshot.heapUsed)} / ${DeviceInfoHelper.formatBytes(snapshot.heapMax)} ($heapPercent%)")
            appendLine("Heap Free: ${DeviceInfoHelper.formatBytes(snapshot.heapFree)}")
            appendLine("Native: ${DeviceInfoHelper.formatBytes(snapshot.nativeHeapUsed)} / ${DeviceInfoHelper.formatBytes(snapshot.nativeHeapMax)} ($nativePercent%)")
            appendLine("GC Count: ${snapshot.gcCount}")
            appendLine("Snapshots: ${_snapshots.value.size}")
        }
    }
}
