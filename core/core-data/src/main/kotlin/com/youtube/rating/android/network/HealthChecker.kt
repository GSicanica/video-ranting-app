package com.youtube.rating.android.network

import com.youtube.rating.shared.models.HealthCheckResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Health check utility for monitoring backend connectivity
 */
class HealthChecker(private val api: RatingApi) {

    /**
     * Perform a health check
     */
    suspend fun checkHealth(): HealthCheckResponse {
        return try {
            api.healthCheck()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Return offline status if health check fails
            HealthCheckResponse(
                success = false,
                message = "Unable to connect to server: ${e.message}",
                data = null
            )
        }
    }

    /**
     * Stream health check results periodically
     */
    fun healthCheckFlow(intervalMs: Long = 30000L): Flow<HealthCheckResponse> = flow {
        while (true) {
            val result = checkHealth()
            emit(result)
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    /**
     * Check if the backend is healthy
     */
    suspend fun isHealthy(): Boolean {
        return try {
            val response = api.healthCheck()
            val data = response.data
            response.success && data != null && data.status == "healthy"
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            false
        }
    }
}
