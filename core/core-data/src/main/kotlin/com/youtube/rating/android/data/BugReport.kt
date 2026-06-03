package com.youtube.rating.android.data

import org.json.JSONObject

/**
 * Bug report data models
 */
data class BugReport(
    val title: String,
    val description: String,
    val deviceInfo: DeviceInfo,
    val appVersion: String,
    val userToken: String? = null,
    val userEmail: String? = null,
    val logs: String? = null,
    val screenshotUrl: String? = null,
    val priority: BugPriority = BugPriority.MEDIUM
)

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val screenResolution: String,
    val language: String,
    val timezone: String,
    val availableMemoryMB: Long,
    val totalMemoryMB: Long,
    val batteryLevel: Int?,
    val isCharging: Boolean?
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("manufacturer", manufacturer)
            put("model", model)
            put("android_version", androidVersion)
            put("sdk_version", sdkVersion)
            put("screen_resolution", screenResolution)
            put("language", language)
            put("timezone", timezone)
            put("available_memory_mb", availableMemoryMB)
            put("total_memory_mb", totalMemoryMB)
            batteryLevel?.let { put("battery_level", it) }
            isCharging?.let { put("is_charging", it) }
        }
    }
}

enum class BugPriority(val value: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical")
}

data class BugReportResponse(
    val success: Boolean,
    val message: String,
    val bugId: Int?
)
