package com.youtube.rating.android.utils

import android.content.Context
import android.os.Build
import android.os.Looper
import com.youtube.rating.shared.utils.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Export debug logs and system info to file
 */
object LogExporter {
    
    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US) }

    private fun isMainThread(): Boolean = Looper.getMainLooper().thread == Thread.currentThread()

    private fun formatTimestamp(date: Date): String =
        dateFormat.get()?.format(date) ?: SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(date)
    
    data class ExportResult(
        val success: Boolean,
        val filePath: String?,
        val error: String?
    )
    
    /**
     * Export full debug report to external files directory
     */
    fun exportDebugReport(context: Context): ExportResult {
        if (isMainThread()) {
            return ExportResult(
                success = false,
                filePath = null,
                error = "exportDebugReport() must be called off the main thread (could ANR)"
            )
        }
        return try {
            val timestamp = formatTimestamp(date = Date())
            val fileName = "debug_report_$timestamp.txt"
            val externalDir = context.getExternalFilesDir(null)
                ?: return ExportResult(success = false, filePath = null, error = "External storage unavailable")
            val file = File(externalDir, fileName)
            
            val report = buildDebugReport(context = context)
            file.writeText(report)
            
            Logger.info("LogExporter", "Debug report exported to: ${file.absolutePath}")
            ExportResult(success = true, filePath = file.absolutePath, error = null)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("LogExporter", "Failed to export debug report", e)
            ExportResult(success = false, filePath = null, error = e.message)
        }
    }
    
    /**
     * Export logcat logs (requires READ_LOGS permission, may not work on newer Android)
     */
    fun exportLogcat(context: Context): ExportResult {
        if (isMainThread()) {
            return ExportResult(
                success = false,
                filePath = null,
                error = "exportLogcat() must be called off the main thread (could ANR)"
            )
        }
        return try {
            val timestamp = formatTimestamp(date = Date())
            val fileName = "logcat_$timestamp.txt"
            val externalDir = context.getExternalFilesDir(null)
                ?: return ExportResult(success = false, filePath = null, error = "External storage unavailable")
            val file = File(externalDir, fileName)
            
            // Execute logcat command
            val process = Runtime.getRuntime().exec("logcat -d -v time *:V")
            val logcatContent = process.inputStream.bufferedReader().use { it.readText() }
            
            file.writeText(logcatContent)
            
            Logger.info("LogExporter", "Logcat exported to: ${file.absolutePath}")
            ExportResult(success = true, filePath = file.absolutePath, error = null)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("LogExporter", "Failed to export logcat", e)
            ExportResult(success = false, filePath = null, error = e.message)
        }
    }
    
    /**
     * Export filtered app-specific logs
     */
    fun exportAppLogs(context: Context): ExportResult {
        if (isMainThread()) {
            return ExportResult(
                success = false,
                filePath = null,
                error = "exportAppLogs() must be called off the main thread (could ANR)"
            )
        }
        return try {
            val timestamp = formatTimestamp(date = Date())
            val fileName = "app_logs_$timestamp.txt"
            val externalDir = context.getExternalFilesDir(null)
                ?: return ExportResult(success = false, filePath = null, error = "External storage unavailable")
            val file = File(externalDir, fileName)
            
            // Filter only app logs with "youtubeRating" tag
            val process = Runtime.getRuntime().exec("logcat -d -v time -s youtubeRating:V AndroidRuntime:E")
            val logContent = process.inputStream.bufferedReader().use { it.readText() }
            
            file.writeText(logContent)
            
            Logger.info("LogExporter", "App logs exported to: ${file.absolutePath}")
            ExportResult(success = true, filePath = file.absolutePath, error = null)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("LogExporter", "Failed to export app logs", e)
            ExportResult(success = false, filePath = null, error = e.message)
        }
    }
    
    /**
     * Build comprehensive debug report
     */
    private fun buildDebugReport(context: Context): String {
        val memoryInfo = DeviceInfoHelper.getMemoryInfo(context)
        val buildInfo = DeviceInfoHelper.getBuildInfo()
        val apkInfo = DeviceInfoHelper.getApkInfo(context)
        
        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("       DEBUG REPORT")
            appendLine("═══════════════════════════════════════")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine()
            
            appendLine("📱 DEVICE INFO")
            appendLine("─────────────────────────────────────")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Build ID: ${Build.ID}")
            appendLine()
            
            appendLine("💾 MEMORY INFO")
            appendLine("─────────────────────────────────────")
            appendLine("Total RAM: ${DeviceInfoHelper.formatBytes(memoryInfo.totalRAM)}")
            appendLine("Available RAM: ${DeviceInfoHelper.formatBytes(memoryInfo.availableRAM)}")
            appendLine("Used RAM: ${DeviceInfoHelper.formatBytes(memoryInfo.usedRAM)}")
            appendLine("Low Memory: ${memoryInfo.lowMemory}")
            appendLine("Heap Size: ${DeviceInfoHelper.formatBytes(memoryInfo.heapSize)}")
            appendLine("Heap Used: ${DeviceInfoHelper.formatBytes(memoryInfo.heapUsed)}")
            appendLine("Native Heap: ${DeviceInfoHelper.formatBytes(memoryInfo.nativeHeapSize)}")
            appendLine("Native Used: ${DeviceInfoHelper.formatBytes(memoryInfo.nativeHeapUsed)}")
            appendLine()
            
            appendLine("🔨 BUILD INFO")
            appendLine("─────────────────────────────────────")
            appendLine("App Version: ${buildInfo.appVersion}")
            appendLine("Version Code: ${buildInfo.versionCode}")
            appendLine("Package: ${buildInfo.packageName}")
            appendLine("Build Type: ${buildInfo.buildType}")
            appendLine("Debuggable: ${buildInfo.debuggable}")
            appendLine("Min SDK: ${buildInfo.minSdk}")
            appendLine("Target SDK: ${buildInfo.targetSdk}")
            appendLine()
            
            appendLine("📦 APK INFO")
            appendLine("─────────────────────────────────────")
            appendLine("APK Size: ${DeviceInfoHelper.formatBytes(apkInfo.apkSize)}")
            appendLine("APK Path: ${apkInfo.apkPath}")
            appendLine("Install Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(apkInfo.installTime))}")
            appendLine("Update Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(apkInfo.updateTime))}")
            appendLine()
            
            appendLine("⚙️ RUNTIME INFO")
            appendLine("─────────────────────────────────────")
            appendLine("Active Threads: ${Thread.activeCount()}")
            appendLine("Processors: ${Runtime.getRuntime().availableProcessors()}")
            appendLine()
            
            appendLine("═══════════════════════════════════════")
        }
    }
    
    /**
     * Get exported files directory path
     */
    fun getExportDirectory(context: Context): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: "N/A"
    }
    
    /**
     * List all exported files
     */
    fun listExportedFiles(context: Context): List<File> {
        val externalDir = context.getExternalFilesDir(null) ?: return emptyList()
        return externalDir.listFiles()?.filter { 
            it.name.startsWith("debug_report_") || 
            it.name.startsWith("logcat_") || 
            it.name.startsWith("app_logs_")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
