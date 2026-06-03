package com.youtube.rating.watchhistory.data

import com.youtube.rating.core.coroutines.ioDispatcher

import android.util.Log
import com.youtube.rating.core.data.network.NetworkResult
import com.youtube.rating.watchhistory.domain.WatchHistoryEntry
import com.youtube.rating.watchhistory.domain.WatchHistoryRemoteRepository
import com.youtube.rating.watchhistory.domain.WatchHistoryRemoteResult
import com.youtube.rating.watchhistory.domain.WatchHistoryTokenProvider
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.WatchHistoryClearRequest
import com.youtube.rating.shared.models.WatchHistoryRecordRequest
import com.youtube.rating.shared.models.WatchHistoryToggleRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.IOException
import java.net.SocketTimeoutException

class WatchHistoryRepository(
    private val tokenProvider: WatchHistoryTokenProvider,
    private val apiClient: RatingApiClient = RatingApiClient()
) : WatchHistoryRemoteRepository {

    companion object {
        private const val TAG = "WatchHistoryRepo"
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    private class HttpStatusException(val statusCode: Int, message: String) : IOException(message)

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

    suspend fun recordView(entry: WatchHistoryEntry): NetworkResult<Unit> = withContext(ioDispatcher) {
        try {
            val userToken = tokenProvider.getUserToken().orEmpty()
            if (userToken.isBlank()) {
                return@withContext NetworkResult.Error(message = "No user token available")
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

            val res = withRetry { apiClient.recordWatchHistory(payload) }
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
                return@withContext NetworkResult.Error(response?.message ?: "Error recording view")
            }

            NetworkResult.Success(Unit)
        } catch (e: Throwable) {
            if (shouldRetry(e)) {
                NetworkResult.Retryable(e.message ?: "Retryable error recording view", e)
            } else {
                NetworkResult.Error(e.message ?: "Error recording view", e)
            }
        }
    }

    suspend fun getHistory(page: Int = 1, perPage: Int = 20): NetworkResult<List<WatchHistoryEntry>> =
        withContext(ioDispatcher) {
            try {
                val userToken = tokenProvider.getUserToken().orEmpty()
                if (userToken.isBlank()) {
                    return@withContext NetworkResult.Error(message = "No user token available")
                }

                val rootRes = withRetry {
                    apiClient.getWatchHistory(userToken = userToken, page = page, perPage = perPage)
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
                    ?: return@withContext NetworkResult.Error("Error loading history")

                if (root["success"]?.jsonPrimitive?.booleanOrNull != true) {
                    return@withContext NetworkResult.Error(
                        message = root["message"]?.jsonPrimitive?.content ?: "Unknown error"
                    )
                }

                val history = root["data"]?.jsonArray.orEmpty().mapNotNull { element ->
                    runCatching { parseHistoryEntry(element.jsonObject) }
                        .onFailure { Log.e(TAG, "Error parsing history entry", it) }
                        .getOrNull()
                }

                NetworkResult.Success(history)
            } catch (e: Throwable) {
                if (shouldRetry(e)) {
                    NetworkResult.Retryable(e.message ?: "Retryable error loading history", e)
                } else {
                    NetworkResult.Error(e.message ?: "Error loading history", e)
                }
            }
        }

    override suspend fun loadHistory(): WatchHistoryRemoteResult<List<WatchHistoryEntry>> {
        return when (val result = getHistory()) {
            is NetworkResult.Success -> WatchHistoryRemoteResult.Success(result.data)
            is NetworkResult.Retryable -> WatchHistoryRemoteResult.Retryable(result.message)
            is NetworkResult.Error -> WatchHistoryRemoteResult.Error(result.message)
        }
    }

    suspend fun toggleHistory(enabled: Boolean): NetworkResult<Unit> = withContext(ioDispatcher) {
        try {
            val userToken = tokenProvider.getUserToken().orEmpty()
            if (userToken.isBlank()) {
                return@withContext NetworkResult.Error(message = "No user token available")
            }

            val res = withRetry {
                apiClient.toggleWatchHistory(WatchHistoryToggleRequest(userToken = userToken, enabled = enabled))
            }
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
                return@withContext NetworkResult.Error(response?.message ?: "Error toggling history")
            }

            NetworkResult.Success(Unit)
        } catch (e: Throwable) {
            if (shouldRetry(e)) {
                NetworkResult.Retryable(e.message ?: "Retryable error toggling history", e)
            } else {
                NetworkResult.Error(e.message ?: "Error toggling history", e)
            }
        }
    }

    suspend fun clearHistory(): NetworkResult<Unit> = withContext(ioDispatcher) {
        try {
            val userToken = tokenProvider.getUserToken().orEmpty()
            if (userToken.isBlank()) {
                return@withContext NetworkResult.Error(message = "No user token available")
            }

            val res = withRetry { apiClient.clearWatchHistory(WatchHistoryClearRequest(userToken = userToken)) }
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
                return@withContext NetworkResult.Error(response?.message ?: "Error clearing history")
            }

            NetworkResult.Success(Unit)
        } catch (e: Throwable) {
            if (shouldRetry(e)) {
                NetworkResult.Retryable(e.message ?: "Retryable error clearing history", e)
            } else {
                NetworkResult.Error(e.message ?: "Error clearing history", e)
            }
        }
    }

    override suspend fun clearRemoteHistory(): WatchHistoryRemoteResult<Unit> {
        return when (val result = clearHistory()) {
            is NetworkResult.Success -> WatchHistoryRemoteResult.Success(Unit)
            is NetworkResult.Retryable -> WatchHistoryRemoteResult.Retryable(result.message)
            is NetworkResult.Error -> WatchHistoryRemoteResult.Error(result.message)
        }
    }

    private fun parseHistoryEntry(json: JsonObject): WatchHistoryEntry {
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
