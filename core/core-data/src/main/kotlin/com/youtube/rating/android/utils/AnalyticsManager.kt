package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.os.Build
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.AppUsageSession
import com.youtube.rating.shared.models.TrackUsageRequest
import com.youtube.rating.shared.utils.LogConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Analytics Manager for tracking app usage sessions and feature interactions
 * - Tracks app sessions (start/end)
 * - Tracks feature usage events
 * - Sends data to backend analytics API
 */
class AnalyticsManager(
    private val context: Context,
    private val apiClient: RatingApiClient,
    private val userTokenManager: UserTokenManager
) {

    companion object {
        private const val TAG = "AnalyticsManager"
        private const val SESSION_TIMEOUT_MINUTES = 30 // Consider session ended after 30 min inactivity
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var currentSessionId: String? = null
    private var sessionStartTime: Long = 0L
    private var lastActivityTime: Long = 0L

    private fun log(message: String) = LogConfig.log("$TAG: $message")
    private fun logError(message: String, throwable: Throwable? = null) = LogConfig.logError("$TAG: $message", throwable)

    /**
     * Called when app comes to foreground (onResume)
     */
    fun onAppForeground() {
        val now = System.currentTimeMillis()
        lastActivityTime = now

        // Check if we need to start a new session
        if (shouldStartNewSession(now = now)) {
            startNewSession()
        }
    }

    /**
     * Called when app goes to background (onPause)
     */
    fun onAppBackground() {
        val now = System.currentTimeMillis()
        lastActivityTime = now

        // End current session after a delay to handle quick app switches
        scope.launch {
            kotlinx.coroutines.delay(5000) // Wait 5 seconds
            if (System.currentTimeMillis() - lastActivityTime >= 5000) {
                endCurrentSession()
            }
        }
    }

    /**
     * Track feature usage
     */
    fun trackFeatureUsage(
        featureName: String,
        action: String,
        duration: Int = 0,
        metadata: Map<String, Any>? = null
    ) {
        scope.launch {
            try {
                val userToken = userTokenManager.getUserTokenAsync() ?: return@launch

                val request = TrackUsageRequest(
                    sessionId = currentSessionId ?: "",
                    action = "feature_used",
                    timestamp = System.currentTimeMillis(),
                    data = mapOf(
                        "userToken" to (userToken ?: ""),
                        "featureName" to featureName,
                        "action" to action,
                        "duration" to duration.toString()
                    ) + (metadata?.mapValues { it.value.toString() } ?: emptyMap())
                )

                val response = apiClient.trackAppUsage(request)
                if (response.success) {
                    log(message = "Tracked feature usage: $featureName - $action")
                } else {
                    logError("Failed to track feature usage: ${response.message}")
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                logError(message = "Error tracking feature usage", throwable = e)
            }
        }
    }

    /**
     * Track video rating
     */
    fun trackVideoRating(
        videoId: String,
        videoTitle: String,
        videoChannel: String,
        love: Int,
        faith: Int,
        hope: Int,
        category: String,
        language: String
    ) {
        trackFeatureUsage(
            featureName = "video_rating",
            action = "rate",
            metadata = mapOf(
                "video_id" to videoId,
                "video_title" to videoTitle,
                "video_channel" to videoChannel,
                "love" to love.toString(),
                "faith" to faith.toString(),
                "hope" to hope.toString(),
                "category" to (category ?: ""),
                "language" to language
            )
        )
    }

    /**
     * Track prayer request creation
     */
    fun trackPrayerRequest() {
        trackFeatureUsage(
            featureName = "prayer",
            action = "create_request"
        )
    }

    /**
     * Track encouragement submission
     */
    fun trackEncouragement() {
        trackFeatureUsage(
            featureName = "prayer",
            action = "add_encouragement"
        )
    }

    /**
     * Track bible reading
     */
    fun trackBibleReading(book: String, chapter: String) {
        trackFeatureUsage(
            featureName = "bible",
            action = "read",
            metadata = mapOf("book" to book, "chapter" to chapter)
        )
    }

    /**
     * Track gospel reading
     */
    fun trackGospelReading(date: String) {
        trackFeatureUsage(
            featureName = "gospel",
            action = "read",
            metadata = mapOf("date" to date)
        )
    }

    private fun shouldStartNewSession(now: Long): Boolean {
        if (currentSessionId == null) return true

        // Start new session if more than 30 minutes have passed since last activity
        val timeSinceLastActivity = now - lastActivityTime
        return timeSinceLastActivity > (SESSION_TIMEOUT_MINUTES * 60 * 1000L)
    }

    private fun startNewSession() {
        scope.launch {
            try {
                val userToken = userTokenManager.getUserTokenAsync() ?: return@launch

                currentSessionId = UUID.randomUUID().toString()
                sessionStartTime = System.currentTimeMillis()

                val deviceInfo = mapOf(
                    "model" to (Build.MODEL ?: "unknown"),
                    "manufacturer" to (Build.MANUFACTURER ?: "unknown"),
                    "os_version" to Build.VERSION.RELEASE,
                    "api_level" to Build.VERSION.SDK_INT.toString(),
                    "brand" to (Build.BRAND ?: "unknown")
                )

                val session = AppUsageSession(
                    sessionId = currentSessionId ?: return@launch,
                    startTime = sessionStartTime,
                    endTime = null,
                    duration = 0L,
                    deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.ID}".replace(" ", "_"),
                    platform = "android",
                    appVersion = com.youtube.rating.android.core.LegacyBuildConfig.VERSION_NAME,
                    userToken = userToken?.toString()
                )

                val response = apiClient.submitAppUsageSession(session)
                if (response.success) {
                    log(message = "Started new session: $currentSessionId")
                } else {
                    logError("Failed to start session: ${response.message}")
                    currentSessionId = null
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                logError(message = "Error starting session", throwable = e)
                currentSessionId = null
            }
        }
    }

    private fun endCurrentSession() {
        val sessionId = currentSessionId ?: return
        val startTime = sessionStartTime

        scope.launch {
            try {
                val userToken = userTokenManager.getUserTokenAsync() ?: return@launch
                val endTime = System.currentTimeMillis()
                val duration = ((endTime - startTime) / 1000).toInt() // Convert to seconds

                val deviceInfo = mapOf(
                    "model" to (Build.MODEL ?: "unknown"),
                    "manufacturer" to (Build.MANUFACTURER ?: "unknown"),
                    "os_version" to Build.VERSION.RELEASE,
                    "api_level" to Build.VERSION.SDK_INT.toString(),
                    "brand" to (Build.BRAND ?: "unknown")
                )

                val session = AppUsageSession(
                    sessionId = sessionId,
                    startTime = startTime,
                    endTime = endTime,
                    duration = duration.toLong(),
                    deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.ID}".replace(" ", "_"),
                    platform = "android",
                    appVersion = com.youtube.rating.android.core.LegacyBuildConfig.VERSION_NAME,
                    userToken = userToken?.toString()
                )

                val response = apiClient.submitAppUsageSession(session)
                if (response.success) {
                    log(message = "Ended session: $sessionId (duration: ${duration}s)")
                } else {
                    logError("Failed to end session: ${response.message}")
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                logError(message = "Error ending session", throwable = e)
            } finally {
                currentSessionId = null
                sessionStartTime = 0L
            }
        }
    }

    /**
     * Force end current session (for app termination)
     */
    fun forceEndSession() {
        endCurrentSession()
    }
}
