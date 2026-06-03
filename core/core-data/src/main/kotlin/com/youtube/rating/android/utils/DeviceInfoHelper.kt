package com.youtube.rating.android.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import java.io.File

/**
 * Helper for collecting device and app info
 */
object DeviceInfoHelper {
    
    data class MemoryInfo(
        val totalRAM: Long,
        val availableRAM: Long,
        val usedRAM: Long,
        val lowMemory: Boolean,
        val heapSize: Long,
        val heapUsed: Long,
        val nativeHeapSize: Long,
        val nativeHeapUsed: Long
    )
    
    data class BuildInfo(
        val appVersion: String,
        val versionCode: Int,
        val buildType: String,
        val debuggable: Boolean,
        val packageName: String,
        val minSdk: Int,
        val targetSdk: Int,
        val compileSdk: Int
    )
    
    data class ApkInfo(
        val apkSize: Long,
        val apkPath: String,
        val installTime: Long,
        val updateTime: Long
    )
    
    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        
        return MemoryInfo(
            totalRAM = memInfo.totalMem,
            availableRAM = memInfo.availMem,
            usedRAM = memInfo.totalMem - memInfo.availMem,
            lowMemory = memInfo.lowMemory,
            heapSize = runtime.maxMemory(),
            heapUsed = runtime.totalMemory() - runtime.freeMemory(),
            nativeHeapSize = Debug.getNativeHeapSize(),
            nativeHeapUsed = Debug.getNativeHeapAllocatedSize()
        )
    }

    fun isLowRamDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024 * 1024)
        return activityManager.isLowRamDevice || memInfo.lowMemory || totalRamGb <= 2.0
    }
    
    fun getBuildInfo(): BuildInfo {
        return BuildInfo(
            appVersion = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            buildType = BuildConfig.BUILD_TYPE,
            debuggable = BuildConfig.DEBUG,
            packageName = BuildConfig.APPLICATION_ID,
            minSdk = BuildConfig.MIN_SDK,
            targetSdk = BuildConfig.TARGET_SDK,
            compileSdk = BuildConfig.COMPILE_SDK
        )
    }
    
    fun getApkInfo(context: Context): ApkInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val apkFile = File(packageInfo.applicationInfo?.sourceDir ?: "")
        
        return ApkInfo(
            apkSize = apkFile.length(),
            apkPath = packageInfo.applicationInfo?.sourceDir ?: "N/A",
            installTime = packageInfo.firstInstallTime,
            updateTime = packageInfo.lastUpdateTime
        )
    }
    
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    fun getPerformanceSnapshot(): String {
        val runtime = Runtime.getRuntime()
        val heapUsed = runtime.totalMemory() - runtime.freeMemory()
        val heapMax = runtime.maxMemory()
        val heapPercent = (heapUsed * 100.0 / heapMax).toInt()
        
        return buildString {
            appendLine("📊 Performance Snapshot")
            appendLine("═══════════════════════")
            appendLine("Heap: ${formatBytes(bytes = heapUsed)} / ${formatBytes(bytes = heapMax)} ($heapPercent%)")
            appendLine("Native: ${formatBytes(bytes = Debug.getNativeHeapAllocatedSize())} / ${formatBytes(bytes = Debug.getNativeHeapSize())}")
            appendLine("Threads: ${Thread.activeCount()}")
        }
    }
}
