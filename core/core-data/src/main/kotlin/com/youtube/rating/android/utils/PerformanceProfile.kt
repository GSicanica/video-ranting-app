package com.youtube.rating.android.utils

import android.content.Context

/**
 * Centralized performance profile to adapt behavior per device class.
 * Keep this lightweight and deterministic (no I/O, no suspend).
 */
object PerformanceProfile {

    enum class DeviceClass { LOW, MID, HIGH }

    data class Profile(
        val deviceClass: DeviceClass,
        val prefetchEnabled: Boolean,
        val prefetchBatchSize: Int,
        val gridCacheAheadDp: Int,
        val gridCacheBehindDp: Int,
        val listCacheAheadDp: Int,
        val listCacheBehindDp: Int,
        val imageMemoryCachePercent: Double,
        val imageDiskCacheBytes: Long,
        val maxImageRequests: Int,
        val maxImageRequestsPerHost: Int
    )

    @Volatile
    private var cached: Profile? = null

    fun get(context: Context): Profile {
        val existing = cached
        if (existing != null) return existing
        synchronized(this) {
            val again = cached
            if (again != null) return again
            val created = buildProfile(context = context.applicationContext)
            cached = created
            return created
        }
    }

    private fun buildProfile(context: Context): Profile {
        val mem = DeviceInfoHelper.getMemoryInfo(context)
        val totalRamGb = mem.totalRAM / (1024.0 * 1024 * 1024)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val isLowRam = DeviceInfoHelper.isLowRamDevice(context)

        val deviceClass = when {
            isLowRam || totalRamGb <= 2.0 || cores <= 4 -> DeviceClass.LOW
            totalRamGb <= 4.0 || cores <= 6 -> DeviceClass.MID
            else -> DeviceClass.HIGH
        }

        return when (deviceClass) {
            DeviceClass.LOW -> Profile(
                deviceClass = deviceClass,
                prefetchEnabled = false,
                prefetchBatchSize = 2,
                gridCacheAheadDp = 160,
                gridCacheBehindDp = 80,
                listCacheAheadDp = 240,
                listCacheBehindDp = 120,
                imageMemoryCachePercent = 0.22,
                imageDiskCacheBytes = 80 * 1024 * 1024L,
                maxImageRequests = 8,
                maxImageRequestsPerHost = 4
            )
            DeviceClass.MID -> Profile(
                deviceClass = deviceClass,
                prefetchEnabled = true,
                prefetchBatchSize = 4,
                gridCacheAheadDp = 220,
                gridCacheBehindDp = 110,
                listCacheAheadDp = 360,
                listCacheBehindDp = 180,
                imageMemoryCachePercent = 0.30,
                imageDiskCacheBytes = 150 * 1024 * 1024L,
                maxImageRequests = 12,
                maxImageRequestsPerHost = 6
            )
            DeviceClass.HIGH -> Profile(
                deviceClass = deviceClass,
                prefetchEnabled = true,
                prefetchBatchSize = 6,
                gridCacheAheadDp = 300,
                gridCacheBehindDp = 150,
                listCacheAheadDp = 520,
                listCacheBehindDp = 260,
                imageMemoryCachePercent = 0.40,
                imageDiskCacheBytes = 220 * 1024 * 1024L,
                maxImageRequests = 18,
                maxImageRequestsPerHost = 8
            )
        }
    }
}
