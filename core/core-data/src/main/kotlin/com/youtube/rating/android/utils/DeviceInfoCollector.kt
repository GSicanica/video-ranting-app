package com.youtube.rating.android.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.view.WindowManager
import com.youtube.rating.android.data.DeviceInfo
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for collecting device information for bug reports
 */
object DeviceInfoCollector {
    
    fun collectDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            screenResolution = getScreenResolution(context),
            language = Locale.getDefault().language,
            timezone = TimeZone.getDefault().id,
            availableMemoryMB = getAvailableMemoryMB(context),
            totalMemoryMB = getTotalMemoryMB(context),
            batteryLevel = getBatteryLevel(context),
            isCharging = isCharging(context)
        )
    }
    
    private fun getScreenResolution(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val bounds = windowManager.currentWindowMetrics.bounds
                "${bounds.width()}x${bounds.height()}"
            } else {
                val dm = context.resources.displayMetrics
                "${dm.widthPixels}x${dm.heightPixels}"
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            val dm = context.resources.displayMetrics
            "${dm.widthPixels}x${dm.heightPixels}"
        }
    }
    
    private fun getAvailableMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }
    
    private fun getTotalMemoryMB(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }
    
    private fun getBatteryLevel(context: Context): Int? {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                null
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }
    
    private fun isCharging(context: Context): Boolean? {
        return try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }
}
