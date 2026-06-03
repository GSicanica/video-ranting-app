package com.youtube.rating.android.youtube

import com.youtube.rating.core.coroutines.ioDispatcher

import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.YouTubeVideoInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

class YouTubeInfoService(
    private val apiClient: RatingApiClient,
    private val cache: YouTubeInfoCache
) {

    /**
     * In-flight dedupe:
     * Ako je request za isti videoId već u tijeku, svi ostali awaitaju isti rezultat,
     * umjesto da šalju novi network call.
     */
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<Result<YouTubeVideoInfo>>>()

    suspend fun getVideoInfo(
        videoId: String,
        forceRefresh: Boolean = false
    ): Result<YouTubeVideoInfo> {
        if (videoId.isBlank()) {
            return Result.failure(IllegalArgumentException("Prazan videoId"))
        }

        // 1) Cache fast-path (ako nije forceRefresh)
        if (!forceRefresh) {
            cache.get(videoId)?.let { return Result.success(it) }
        }

        // 2) Dedupe: ako već postoji request za ovaj id -> čekaj njega
        return coroutineScope {
            val existing = inFlightMutex.withLock { inFlight[videoId] }
            if (existing != null) {
                return@coroutineScope existing.await()
            }

            // 3) Kreiraj novi request i registriraj ga kao "in-flight"
            val deferred = async(ioDispatcher) {
                runCatchingWithRetry {
                    apiClient.getYouTubeVideoInfo(videoId)
                }.onSuccess { info ->
                    // Spremi u cache (pretpostavka: put(info) koristi info.videoId interno)
                    cache.put(info)

                    // Ako želiš 100% sigurnost da ide pod key = request videoId,
                    // a tvoj cache to podržava, zamijeni s: cache.put(videoId, info)
                }
            }

            inFlightMutex.withLock { inFlight[videoId] = deferred }

            try {
                deferred.await()
            } finally {
                // 4) Obavezno očisti mapu i kad request faila
                inFlightMutex.withLock { inFlight.remove(videoId) }
            }
        }
    }

    suspend fun prefetch(videoIds: List<String>) {
        if (videoIds.isEmpty()) return

        coroutineScope {
            // distinct da ne šalješ duple id-eve u istom batchu
            val ids = videoIds.distinct().filter { it.isNotBlank() }
            if (ids.isEmpty()) return@coroutineScope

            // Limit parallelism to avoid spiky network usage
            val chunkSize = 3
            ids.chunked(chunkSize).forEach { chunk ->
                chunk.map { id ->
                    async(ioDispatcher) {
                        if (cache.get(id) == null) {
                            getVideoInfo(id, forceRefresh = false)
                        }
                    }
                }.awaitAll()
            }
        }
    }

    fun clearCache() {
        cache.clear()
    }

    fun invalidate(videoId: String) {
        if (videoId.isNotBlank()) {
            cache.invalidate(videoId)
        }
    }

    private suspend fun <T> runCatchingWithRetry(
        maxRetries: Int = 2,
        initialDelayMs: Long = 350,
        block: suspend () -> T
    ): Result<T> {
        var attempt = 0
        var delayMs = initialDelayMs
        var lastError: Throwable? = null

        while (attempt <= maxRetries) {
            try {
                return Result.success(block())
            } catch (e: Throwable) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                if (e is CancellationException) {
                    // Propagate cancellation without rethrowing from here.
                    coroutineContext.cancel(e)
                    return Result.failure(e)
                }
                lastError = e

                if (!isRetryable(error = e) || attempt == maxRetries) break

                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(2000)
                attempt += 1
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Nepoznata greška"))
    }

    private fun isRetryable(error: Throwable): Boolean {
        if (error is UnknownHostException || error is SocketTimeoutException) return true
        val msg = error.message.orEmpty()
        return msg.contains("timeout", ignoreCase = true) ||
                msg.contains("429") ||
                msg.contains("too many", ignoreCase = true)
    }
}
