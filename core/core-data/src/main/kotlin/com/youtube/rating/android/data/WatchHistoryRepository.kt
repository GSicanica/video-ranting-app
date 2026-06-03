package com.youtube.rating.android.data

import com.youtube.rating.core.coroutines.ioDispatcher

import android.util.Log
import com.youtube.rating.android.network.NetworkResult
import com.youtube.rating.android.storage.WatchHistoryEntry
import com.youtube.rating.android.utils.CoroutineTracing
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.WatchHistoryClearRequest
import com.youtube.rating.shared.models.WatchHistoryRecordRequest
import com.youtube.rating.shared.models.WatchHistoryToggleRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Repository for watch history API operations.
 * Uses RatingApiClient (baseUrlProvider = Ktor) for consistent timeout/retry behavior.
 */
class WatchHistoryRepository(
    private val userTokenManager: UserTokenManager,
    private val apiClient: RatingApiClient = RatingApiClient()
) {

    companion object {
        private const val TAG = "WatchHistoryRepo"
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    private class HttpStatusException(val statusCode: Int, message: String) : IOException(message)

    private suspend fun getUserTokenOrNull(): String? {
        return userTokenManager.getUserTokenAsync()
            ?: userTokenManager.getCachedUserToken()
    }

    private fun shouldRetry(throwable: Throwable): Boolean {
        return when (throwable) {
            is SocketTimeoutException -> true
            is IOException -> true
            is HttpStatusException -> throwable.statusCode >= 500 || throwable.statusCode == 429
            else -> false
        }
    }

    private suspend fun <T> withRetry(block: suspend () -> T): Result<T> {
        var attempt = 0
        var delayMs = INITIAL_RETRY_DELAY_MS
        var lastError: Throwable? = null

        while (attempt < MAX_RETRIES) {
            try {
                return Result.success(block())
            } catch (t: Throwable) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(t)
                lastError = t
                attempt++
                if (attempt >= MAX_RETRIES || !shouldRetry(t)) break
                Log.d(TAG, "Retry $attempt/$MAX_RETRIES after ${delayMs}ms")
                delay(delayMs)
                delayMs *= 2
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Unknown network error"))
    }

    /**
     * Record video view on server with retry logic.
     */
    suspend fun recordView(entry: WatchHistoryEntry): NetworkResult<Unit> =
        CoroutineTracing.traceSuspend("WatchHistory.recordView") {
            withContext(ioDispatcher) {
                try {
                    val userToken = getUserTokenOrNull().orEmpty()
                    if (userToken.isBlank()) {
                        return@withContext NetworkResult.Error(
                            message = "No user token available",
                            throwable = null
                        )
                    }

                    val payload = WatchHistoryRecordRequest(
                        userToken = userToken,
                        videoId = entry.videoId,
                        title = entry.title,
                        thumbnail = entry.thumbnail,
                        channelName = entry.channelName,
                        category = entry.category ?: "",
                        source = entry.source,
                        watchDuration = (entry.watchDuration / 1000).toInt(),
                        totalDuration = (entry.totalDuration / 1000).toInt()
                    )

                    val res = withRetry {
                        apiClient.recordWatchHistory(payload)
                    }
                    if (res.isFailure) {
                        val e = res.exceptionOrNull()
                        return@withContext if (e != null && shouldRetry(e)) {
                            NetworkResult.Retryable(e.message ?: "Retryable error recording view", e)
                        } else {
                            NetworkResult.Error(e?.message ?: "Error recording view", e)
                        }
                    }
                    val response = res.getOrNull()
                    if (response == null || !response.success) {
                        return@withContext NetworkResult.Error(
                            message = response?.message ?: "Error recording view",
                            throwable = null
                        )
                    }

                    Log.d(TAG, "Successfully recorded view for: ${entry.videoId}")
                    NetworkResult.Success(Unit)
                } catch (e: Throwable) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Log.e(TAG, "Error recording view", e)
                    if (shouldRetry(e)) {
                        NetworkResult.Retryable(
                            message = e.message ?: "Retryable error recording view",
                            throwable = e
                        )
                    } else {
                        NetworkResult.Error(
                            message = e.message ?: "Error recording view",
                            throwable = e
                        )
                    }
                }
            }
        }

    /**
     * Get watch history from server.
     * Uses stream-based JSON parsing from response body reader.
     */
    suspend fun getHistory(page: Int = 1, perPage: Int = 20): NetworkResult<List<WatchHistoryEntry>> =
        CoroutineTracing.traceSuspend("WatchHistory.getHistory") {
            withContext(ioDispatcher) {
                try {
                    val userToken = getUserTokenOrNull().orEmpty()
                    if (userToken.isBlank()) {
                        return@withContext NetworkResult.Error(
                            message = "No user token available",
                            throwable = null
                        )
                    }

                    val rootRes = withRetry {
                        apiClient.getWatchHistory(
                            userToken = userToken,
                            page = page,
                            perPage = perPage
                        )
                    }
                    if (rootRes.isFailure) {
                        val e = rootRes.exceptionOrNull()
                        return@withContext if (e != null && shouldRetry(e)) {
                            NetworkResult.Retryable(e.message ?: "Retryable error loading history", e)
                        } else {
                            NetworkResult.Error(e?.message ?: "Error loading history", e)
                        }
                    }
                    val root = rootRes.getOrNull()
                        ?: return@withContext NetworkResult.Error("Error loading history", null)

                    if (root["success"]?.jsonPrimitive?.booleanOrNull != true) {
                        return@withContext NetworkResult.Error(
                            message = root["message"]?.jsonPrimitive?.content ?: "Unknown error",
                            throwable = null
                        )
                    }

                    val dataArray = root["data"]?.jsonArray.orEmpty()
                    val history = mutableListOf<WatchHistoryEntry>()
                    for (i in dataArray.indices) {
                        runCatching {
                            parseHistoryEntry(json = dataArray[i].jsonObject)
                        }.onSuccess { history.add(it) }
                            .onFailure { e -> Log.e(TAG, "Error parsing history entry at index $i", e) }
                    }

                    Log.d(TAG, "Retrieved ${history.size} history entries from server")
                    NetworkResult.Success(history)
                } catch (e: Throwable) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Log.e(TAG, "Error getting history from server", e)
                    if (shouldRetry(e)) {
                        NetworkResult.Retryable(
                            message = e.message ?: "Retryable error loading history",
                            throwable = e
                        )
                    } else {
                        NetworkResult.Error(
                            message = e.message ?: "Error loading history",
                            throwable = e
                        )
                    }
                }
            }
        }

    /**
     * Toggle history tracking on server.
     */
    suspend fun toggleHistory(enabled: Boolean): NetworkResult<Unit> =
        CoroutineTracing.traceSuspend("WatchHistory.toggleHistory") {
            withContext(ioDispatcher) {
                try {
                    val userToken = getUserTokenOrNull().orEmpty()
                    if (userToken.isBlank()) {
                        return@withContext NetworkResult.Error(
                            message = "No user token available",
                            throwable = null
                        )
                    }

                    val payload = WatchHistoryToggleRequest(
                        userToken = userToken,
                        enabled = enabled
                    )

                    val res = withRetry { apiClient.toggleWatchHistory(payload) }
                    if (res.isFailure) {
                        val e = res.exceptionOrNull()
                        return@withContext if (e != null && shouldRetry(e)) {
                            NetworkResult.Retryable(e.message ?: "Retryable error toggling history", e)
                        } else {
                            NetworkResult.Error(e?.message ?: "Error toggling history", e)
                        }
                    }
                    val response = res.getOrNull()
                    if (response == null || !response.success) {
                        return@withContext NetworkResult.Error(
                            message = response?.message ?: "Error toggling history",
                            throwable = null
                        )
                    }

                    Log.d(TAG, "Successfully toggled history setting")
                    NetworkResult.Success(Unit)
                } catch (e: Throwable) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Log.e(TAG, "Error toggling history setting", e)
                    if (shouldRetry(e)) {
                        NetworkResult.Retryable(
                            message = e.message ?: "Retryable error toggling history",
                            throwable = e
                        )
                    } else {
                        NetworkResult.Error(
                            message = e.message ?: "Error toggling history",
                            throwable = e
                        )
                    }
                }
            }
        }

    /**
     * Clear all watch history on server.
     */
    suspend fun clearHistory(): NetworkResult<Unit> =
        CoroutineTracing.traceSuspend("WatchHistory.clearHistory") {
            withContext(ioDispatcher) {
                try {
                    val userToken = getUserTokenOrNull().orEmpty()
                    if (userToken.isBlank()) {
                        return@withContext NetworkResult.Error(
                            message = "No user token available",
                            throwable = null
                        )
                    }

                    val payload = WatchHistoryClearRequest(userToken = userToken)

                    val res = withRetry { apiClient.clearWatchHistory(payload) }
                    if (res.isFailure) {
                        val e = res.exceptionOrNull()
                        return@withContext if (e != null && shouldRetry(e)) {
                            NetworkResult.Retryable(e.message ?: "Retryable error clearing history", e)
                        } else {
                            NetworkResult.Error(e?.message ?: "Error clearing history", e)
                        }
                    }
                    val response = res.getOrNull()
                    if (response == null || !response.success) {
                        return@withContext NetworkResult.Error(
                            message = response?.message ?: "Error clearing history",
                            throwable = null
                        )
                    }

                    Log.d(TAG, "Successfully cleared history on server")
                    NetworkResult.Success(Unit)
                } catch (e: Throwable) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    Log.e(TAG, "Error clearing history on server", e)
                    if (shouldRetry(e)) {
                        NetworkResult.Retryable(
                            message = e.message ?: "Retryable error clearing history",
                            throwable = e
                        )
                    } else {
                        NetworkResult.Error(
                            message = e.message ?: "Error clearing history",
                            throwable = e
                        )
                    }
                }
            }
        }

    /**
     * Parse history entry from JSON.
     */
    private fun parseHistoryEntry(json: kotlinx.serialization.json.JsonObject): WatchHistoryEntry {
        return WatchHistoryEntry(
            id = json["id"]?.jsonPrimitive?.intOrNull ?: 0,
            videoId = json["video_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            title = json["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            thumbnail = json["thumbnail"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            channelName = json["channel_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            category = json["category"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() },
            source = json["source"]?.jsonPrimitive?.contentOrNull ?: "home",
            watchDuration = (json["watch_duration"]?.jsonPrimitive?.longOrNull ?: 0L) * 1000,
            totalDuration = (json["total_duration"]?.jsonPrimitive?.longOrNull ?: 0L) * 1000,
            viewedAt = json["viewed_at"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        )
    }
}
