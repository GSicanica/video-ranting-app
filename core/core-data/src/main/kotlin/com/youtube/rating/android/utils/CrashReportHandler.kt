package com.youtube.rating.android.utils

import android.content.Context
import android.os.Build
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.sentry.SentryLogger
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.CrashReport
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.launch
import java.io.File
import org.json.JSONObject
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * Global Exception Handler that automatically sends crash reports to backend
 * Install this in Application.onCreate()
 * ✅ FIXED: Added cleanup method and thread-safe singleton pattern
 */
class CrashReportHandler(
    private val context: Context,
    private val apiClient: RatingApiClient,
    private val userTokenManager: UserTokenManager
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private val installLock = Any()
        
        @Volatile
        private var currentHandler: CrashReportHandler? = null

        @Volatile
        private var originalDefaultHandler: Thread.UncaughtExceptionHandler? = null
        
        fun install(context: Context, apiClient: RatingApiClient, userTokenManager: UserTokenManager) {
            synchronized(installLock) {
                Logger.info("CrashReportHandler", "install() called")

                // Already installed: avoid wrapping ourselves as the default handler.
                if (currentHandler != null) {
                    Logger.info("CrashReportHandler", "Already installed, skipping")
                    return
                }

                // Preserve the original default handler once.
                if (originalDefaultHandler == null) {
                    originalDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                }
                
                val handler = CrashReportHandler(context = context, apiClient = apiClient, userTokenManager = userTokenManager)
                currentHandler = handler
                Thread.setDefaultUncaughtExceptionHandler(handler)
                Logger.info("CrashReportHandler", "Set as default uncaught exception handler")
            }
        }
        
        /**
         * ✅ FIXED: Cleanup method to prevent memory leaks
         */
        fun cleanup() {
            synchronized(installLock) {
                val handler = currentHandler
                if (handler != null && Thread.getDefaultUncaughtExceptionHandler() === handler) {
                    Thread.setDefaultUncaughtExceptionHandler(originalDefaultHandler)
                }
                currentHandler = null
            }
        }

        /**
         * Best-effort send of any queued crash reports.
         * Call from Application.onCreate() after dependencies are ready.
         */
        fun sendPendingReports(
            context: Context,
            apiClient: RatingApiClient,
            userTokenManager: UserTokenManager
        ) {
            val appContext = context.applicationContext
            val dir = File(appContext.filesDir, "crash_reports")
            if (!dir.exists()) return
            AppScope.get().makeIOCall {
                val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".json") } ?: return@makeIOCall
                files.forEach { file ->
                    val report = runCatching { parseReport(json = file.readText(), userTokenManager = userTokenManager) }.getOrNull()
                    if (report == null) {
                        file.delete()
                        return@forEach
                    }
                    val success = runCatching {
                        apiClient.submitCrashReport(report).success
                    }.getOrDefault(false)
                    if (success) {
                        file.delete()
                    }
                }
            }
        }

        private fun reportToJson(report: CrashReport): JSONObject = JSONObject().apply {
            put("stackTrace", report.stackTrace)
            put("errorMessage", report.errorMessage)
            put("timestamp", report.timestamp)
            put("appVersion", report.appVersion)
            put("appVersionCode", report.appVersionCode)
            put("androidVersion", report.androidVersion)
            put("androidSdk", report.androidSdk)
            put("deviceModel", report.deviceModel)
            put("manufacturer", report.manufacturer)
            put("userToken", report.userToken ?: "")
            put("threadName", report.threadName)
            put("availableMemory", report.availableMemory)
            put("totalMemory", report.totalMemory)
            val custom = JSONObject()
            report.customData?.forEach { (k, v) -> custom.put(k, v) }
            put("customData", custom)
        }

        private fun parseReport(json: String, userTokenManager: UserTokenManager): CrashReport {
            val obj = JSONObject(json)
            val userToken = obj.optString("userToken").ifBlank { userTokenManager.getCachedUserToken() }
            val custom = obj.optJSONObject("customData")
            val customMap = mutableMapOf<String, String>()
            if (custom != null) {
                custom.keys().forEach { key -> customMap[key] = custom.optString(key) }
            }
            return CrashReport(
                stackTrace = obj.optString("stackTrace"),
                errorMessage = obj.optString("errorMessage"),
                timestamp = obj.optLong("timestamp"),
                appVersion = obj.optString("appVersion"),
                appVersionCode = obj.optInt("appVersionCode"),
                androidVersion = obj.optString("androidVersion"),
                androidSdk = obj.optInt("androidSdk"),
                deviceModel = obj.optString("deviceModel"),
                manufacturer = obj.optString("manufacturer"),
                userToken = userToken,
                threadName = obj.optString("threadName"),
                availableMemory = obj.optLong("availableMemory"),
                totalMemory = obj.optLong("totalMemory"),
                customData = customMap
            )
        }
    }
    
    /**
     * Cancel coroutine scope to prevent memory leaks
     */
    private fun cleanup() {
        // AppScope is shared; do not cancel here.
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Logger.error("CrashReportHandler", "Uncaught exception intercepted: ${throwable.message}", throwable)

        // Also report to Sentry (best-effort).
        // Avoid duplicate events when the default handler is Sentry's own uncaught exception handler.
        val defaultHandlerName = defaultHandler?.javaClass?.name?.lowercase().orEmpty()
        val defaultIsSentry = defaultHandlerName.contains("sentry")
        if (!defaultIsSentry) {
            val hasToken = try {
                val t = userTokenManager.getCachedUserToken()
                !t.isNullOrBlank()
            } catch (e: Exception) {
                false
            }
            SentryLogger.captureException(
                throwable,
                tags = mapOf("source" to "uncaught"),
                extras = mapOf("hasUserToken" to hasToken)
            )
        }
        
        try {
            // Collect crash information
            val stackTrace = getStackTraceString(throwable = throwable)
            val errorMessage = throwable.message ?: throwable.javaClass.simpleName
            
            Logger.info("CrashReportHandler", "Error message: $errorMessage")
            Logger.info("CrashReportHandler", "Thread: ${thread.name}")
            
            // Get memory info
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val totalMemory = runtime.maxMemory() / 1024 / 1024
            
            Logger.info("CrashReportHandler", "Memory used=${usedMemory}MB total=${totalMemory}MB")
            
            // Create crash report
            val crashReport = CrashReport(
                stackTrace = stackTrace,
                errorMessage = errorMessage,
                timestamp = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                androidVersion = Build.VERSION.RELEASE,
                androidSdk = Build.VERSION.SDK_INT,
                deviceModel = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                userToken = userTokenManager.getCachedUserToken(),
                threadName = thread.name,
                availableMemory = runtime.freeMemory() / 1024 / 1024,
                totalMemory = totalMemory,
                customData = mapOf(
                    "brand" to Build.BRAND,
                    "device" to Build.DEVICE,
                    "product" to Build.PRODUCT,
                    "buildType" to BuildConfig.BUILD_TYPE
                )
            )
            
            Logger.info("CrashReportHandler", "CrashReport created")
            
            // Persist crash report and send on next app start.
            try {
                persistCrashReport(context = context, report = crashReport)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error("CrashReportHandler", "Failed to persist crash report", e)
            }
            
            Logger.error("CrashReportHandler", "Uncaught exception", throwable)
            
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("CrashReportHandler", "Error in crash handler", e)
        } finally {
            // Call default handler to show crash dialog
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun getStackTraceString(throwable: Throwable): String {
        return throwable.stackTraceToString()
    }

    private fun persistCrashReport(context: Context, report: CrashReport) {
        val dir = File(context.filesDir, "crash_reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "crash_${report.timestamp}.json")
        file.writeText(reportToJson(report = report).toString())
    }
}
