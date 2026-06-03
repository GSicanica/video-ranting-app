package com.youtube.rating.android.api

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.util.Log
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.data.BugReportResponse
import com.youtube.rating.android.network.NetworkResult
import com.youtube.rating.android.utils.DeviceInfoCollector
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.BugReportRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.context.GlobalContext

/**
 * Bug Report API Client
 * Sends bug reports to backend through RatingApiClient (baseUrlProvider = Ktor).
 */
class BugReportApiClient(
    private val apiClient: RatingApiClient = RatingApiClient()
) {
    
    companion object {
        private const val TAG = "BugReportAPI"
    }

    private suspend fun getUserTokenOrNull(): String? {
        val koin = GlobalContext.getOrNull() ?: return null
        val userTokenManager: UserTokenManager = koin.get()

        return userTokenManager.getUserTokenAsync()
            ?: userTokenManager.getCachedUserToken()
    }
    
    /**
     * Submit bug report to backend
     */
    suspend fun submitBugReport(
        context: Context,
        title: String,
        description: String,
        userEmail: String? = null,
        priority: String = "medium"
    ): NetworkResult<BugReportResponse> = withContext(ioDispatcher) {
        try {
            // Collect device info
            val deviceInfo = DeviceInfoCollector.collectDeviceInfo(context)
            
            // Get user token instead of device ID
            val userToken = getUserTokenOrNull()
            
            // Build JSON payload
            val deviceInfoMap = mapOf(
                "manufacturer" to deviceInfo.manufacturer,
                "model" to deviceInfo.model,
                "android_version" to deviceInfo.androidVersion,
                "sdk_version" to deviceInfo.sdkVersion.toString(),
                "screen_resolution" to deviceInfo.screenResolution,
                "language" to deviceInfo.language,
                "timezone" to deviceInfo.timezone,
                "available_memory_mb" to deviceInfo.availableMemoryMB.toString(),
                "total_memory_mb" to deviceInfo.totalMemoryMB.toString(),
                "battery_level" to deviceInfo.batteryLevel.toString(),
                "is_charging" to deviceInfo.isCharging.toString()
            )

            val payload = BugReportRequest(
                title = title,
                description = description,
                device_info = deviceInfoMap,
                app_version = BuildConfig.VERSION_NAME,
                user_token = userToken,
                user_email = userEmail,
                priority = priority
            )
            
            Log.i(TAG, "Submitting bug report: $title")
            val jsonResponse = apiClient.submitBugReport(payload)
            val success = jsonResponse["success"]?.jsonPrimitive?.booleanOrNull == true
            if (success) {
                val bugId = jsonResponse["bug_id"]?.jsonPrimitive?.intOrNull ?: 0
                val message = jsonResponse["message"]?.jsonPrimitive?.contentOrNull ?: "OK"
                Log.i(TAG, "Bug report submitted successfully: #$bugId")
                NetworkResult.Success(BugReportResponse(success = true, message = message, bugId = bugId))
            } else {
                val error = (jsonResponse["error"]?.jsonPrimitive?.contentOrNull ?: "").ifBlank {
                    "Bug report failed"
                }
                Log.e(TAG, "Bug report failed: $error")
                NetworkResult.Error(error)
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Log.e(TAG, "Bug report error: ${e.message}", e)
            NetworkResult.Retryable(
                message = e.message ?: "Bug report error",
                throwable = e
            )
        }
    }
}
