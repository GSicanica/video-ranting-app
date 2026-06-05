package com.youtube.rating.shared.api

import com.youtube.rating.shared.data.AddEncouragementBody
import com.youtube.rating.shared.data.CreatePrayerRequestBody
import com.youtube.rating.shared.data.EncouragementDto
import com.youtube.rating.shared.data.PrayerPageDto
import com.youtube.rating.shared.data.PrayerRequestDto
import com.youtube.rating.shared.data.SetPrayerPrayedBody
import com.youtube.rating.shared.models.AnonymousRegisterResponse
import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.BlockedUsersResponse
import com.youtube.rating.shared.models.BibleSearchResponse
import com.youtube.rating.shared.models.BulkRatingRequest
import com.youtube.rating.shared.models.BulkRatingResponse
import com.youtube.rating.shared.models.DailyUsageStats
import com.youtube.rating.shared.models.EnhancedRatingResponse
import com.youtube.rating.shared.models.EnhancedVideosListResponse
import com.youtube.rating.shared.models.FavoriteVideoDto
import com.youtube.rating.shared.models.GalleryListResponse
import com.youtube.rating.shared.models.GospelDayResponse
import com.youtube.rating.shared.models.SaintOfDayResponse
import com.youtube.rating.shared.models.HealthCheckResponse
import com.youtube.rating.shared.models.HomeCategoriesResponse
import com.youtube.rating.shared.models.NovenaTodayResponse
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.models.TopVideosResponse
import com.youtube.rating.shared.models.PaginationData
import com.youtube.rating.shared.models.PopularTermsResponse
import com.youtube.rating.shared.models.RandomPassageResponse
import com.youtube.rating.shared.models.RatedVideosResponse
import com.youtube.rating.shared.models.RatingRequest
import com.youtube.rating.shared.models.RatingResponse
import com.youtube.rating.shared.models.RatingResultData
import com.youtube.rating.shared.models.SearchResponse
import com.youtube.rating.shared.models.SearchVideosResponse
import com.youtube.rating.shared.models.StandardizedApiResponse
import com.youtube.rating.shared.models.SyncFavoritesRequest
import com.youtube.rating.shared.models.SyncFavoritesResponse
import com.youtube.rating.shared.models.PsalmHighlightsSyncRequest
import com.youtube.rating.shared.models.PsalmHighlightsSyncResponse
import com.youtube.rating.shared.models.WatchHistoryClearRequest
import com.youtube.rating.shared.models.WatchHistoryRecordRequest
import com.youtube.rating.shared.models.WatchHistoryToggleRequest
import com.youtube.rating.shared.models.BugReportRequest
import com.youtube.rating.shared.models.UsageAnalyticsResponse
import com.youtube.rating.shared.models.UsageSummary
import com.youtube.rating.shared.models.VideoItem
import com.youtube.rating.shared.models.VideoSearchResult
import com.youtube.rating.shared.models.VideoStats
import com.youtube.rating.shared.models.VideosData
import com.youtube.rating.shared.models.VideosListResponse
import com.youtube.rating.shared.models.YouTubeVideoInfo
import com.youtube.rating.shared.network.NetworkPolicy
import com.youtube.rating.shared.utils.LogConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.min

/**
 * Platform-specific HttpClient creation
 */
internal expect fun createPlatformHttpClient(
    jsonConfig: Json,
    enableLogging: Boolean,
    requestTimeoutMillis: Long,
    connectTimeoutMillis: Long,
    socketTimeoutMillis: Long,
    context: Any? = null
): HttpClient

class RatingApiClient(
    private val baseUrlProvider: () -> String = { com.youtube.rating.shared.BASE_URL },
    private val enableDebugLogging: Boolean = LogConfig.ENABLE_LOGS,
    private val context: Any? = null
) {

    constructor(
        baseUrl: String,
        enableDebugLogging: Boolean = LogConfig.ENABLE_LOGS,
        context: Any? = null
    ) : this(
        baseUrlProvider = { baseUrl },
        enableDebugLogging = enableDebugLogging,
        context = context
    )

    private val baseUrl: String
        get() = baseUrlProvider().trimEnd('/')

    companion object {
        private const val CSRF_TOKEN_HEADER = "X-CSRF-Token"
        private const val CSRF_TTL_MS = 60 * 60 * 1000L // 1 hour

        // Cache TTLs (conservative)
        private const val TTL_VIDEO_STATS_MS = 30_000L          // 30s
        private const val TTL_GOSPEL_MS = 15 * 60_000L          // 15m
        private const val TTL_BIBLE_PASSAGE_MS = 30 * 60_000L   // 30m
        private const val TTL_BIBLE_SEARCH_MS = 5 * 60_000L     // 5m
        private const val TTL_NOVENA_MS = 60 * 60_000L          // 1h
        private const val TTL_VIDEOS_LIST_MS = 60_000L          // 60s
        private const val TTL_BLOCKED_USERS_MS = 60_000L        // 60s
        private const val TTL_PRAYER_ENCOURAGEMENTS_MS = 20_000L// 20s
        private const val TTL_PRAYER_REQUESTS_MS = 20_000L      // 20s
        private const val TTL_HEALTH_MS = 10_000L               // 10s
        private const val TTL_GALLERY_MS = 60_000L              // 60s
        private const val TTL_RATED_VIDEOS_MS = 15_000L         // 15s
        private const val TTL_YOUTUBE_VIDEO_INFO_MS = 24 * 60 * 60 * 1000L // 24h
        private const val TTL_TOP_VIDEOS_MS = 5 * 60_000L         // 5m
        private const val TTL_HOME_CATEGORIES_MS = 10 * 60_000L   // 10m
    }

    private fun log(message: String) = LogConfig.log(message)
    private fun logError(message: String, throwable: Throwable? = null) =
        LogConfig.logError(message, throwable)

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonConfig = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val client by lazy {
        createPlatformHttpClient(
            jsonConfig = jsonConfig,
            enableLogging = enableDebugLogging,
            requestTimeoutMillis = NetworkPolicy.REQUEST_TIMEOUT_MS,
            connectTimeoutMillis = NetworkPolicy.CONNECT_TIMEOUT_MS,
            socketTimeoutMillis = NetworkPolicy.SOCKET_TIMEOUT_MS,
            context = context
        )
    }

    // Helper function to add development headers
    private fun HttpRequestBuilder.addDevelopmentHeaders() {
        // Add X-Dev-Mode header only when debug logging is enabled
        if (enableDebugLogging) {
            headers { append("X-Dev-Mode", "true") }
        }
    }

    // ===================== MEMORY CACHE (KMM SAFE) =====================

    /**
     * In-memory cache:
     * - TTL
     * - LRU eviction (touch by remove+put)
     * - per-key lock (stampede protection)
     *
     * Nested to avoid visibility exposure issues in KMM.
     */
    private class MemoryResponseCache(
        private val maxEntries: Int = 300
    ) {
        private data class Entry(val value: Any, val expiryMs: Long)

        private val mutex = Mutex()

        // KMM common: no accessOrder constructor, so we simulate LRU by touch().
        private val map: LinkedHashMap<String, Entry> = LinkedHashMap()

        // Prevent multiple identical requests simultaneously
        private val keyLocks = mutableMapOf<String, Mutex>()

        private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

        private fun touchUnsafe(key: String) {
            val e = map.remove(key) ?: return
            map[key] = e // move to end
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> getIfFreshUnsafe(key: String): T? {
            val entry = map[key] ?: return null
            if (nowMs() >= entry.expiryMs) {
                map.remove(key)
                return null
            }
            touchUnsafe(key = key)
            return entry.value as? T
        }

        private fun evictIfNeededUnsafe() {
            while (map.size > maxEntries) {
                val eldest = map.keys.firstOrNull() ?: return
                map.remove(eldest)
                keyLocks.remove(eldest)
            }
        }

        suspend fun invalidate(key: String) = mutex.withLock { map.remove(key) }

        suspend fun invalidatePrefix(prefix: String) = mutex.withLock {
            val it = map.keys.iterator()
            while (it.hasNext()) {
                val k = it.next()
                if (k.startsWith(prefix)) it.remove()
            }
        }

        suspend fun clear() = mutex.withLock { map.clear() }

        suspend inline fun <reified T : Any> getOrPut(
            key: String,
            ttlMs: Long,
            crossinline loader: suspend () -> T
        ): T {
            // fast path
            val cached: T? = mutex.withLock { getIfFreshUnsafe(key) }
            if (cached != null) return cached

            // per-key lock
            val lock = mutex.withLock { keyLocks.getOrPut(key) { Mutex() } }
            return try {
                lock.withLock {
                    // re-check after lock
                    val cached2: T? = mutex.withLock { getIfFreshUnsafe(key) }
                    if (cached2 != null) return@withLock cached2

                    val loaded = loader()
                    mutex.withLock {
                        map[key] = Entry(value = loaded, expiryMs = nowMs() + ttlMs)
                        touchUnsafe(key = key)
                        evictIfNeededUnsafe()
                    }
                    loaded
                }
            } finally {
                // If the load failed (no cache entry), don't retain the lock.
                mutex.withLock {
                    if (!map.containsKey(key)) {
                        keyLocks.remove(key)
                    }
                }
            }
        }
    }

    private val cache = MemoryResponseCache(maxEntries = 300)

    private class CircuitBreaker(
        private val failureThreshold: Int,
        private val openMillis: Long
    ) {
        private var failures = 0
        private var openUntilMs: Long = 0L

        fun check(nowMs: Long) {
            if (openUntilMs > nowMs) {
                throw Exception("Circuit breaker open. Try again later.")
            }
        }

        fun onSuccess() {
            failures = 0
            openUntilMs = 0L
        }

        fun onFailure(nowMs: Long) {
            failures += 1
            if (failures >= failureThreshold) {
                openUntilMs = nowMs + openMillis
            }
        }
    }

    private val circuitBreaker = CircuitBreaker(
        failureThreshold = 5,
        openMillis = 30_000L
    )

    private fun cacheKey(path: String, params: Map<String, Any?> = emptyMap()): String {
        val normalized = params
            .filterValues { it != null }
            .toList()
            .sortedBy { it.first }
            .joinToString("&") { (k, v) -> "$k=$v" }
        return if (normalized.isBlank()) path else "$path?$normalized"
    }

    private suspend inline fun <reified T : Any> cachedGet(
        path: String,
        ttlMs: Long,
        params: Map<String, Any?> = emptyMap(),
        crossinline loader: suspend () -> T
    ): T {
        val key = cacheKey(path, params)
        return cache.getOrPut(key, ttlMs) { loader() }
    }

    // ===================== CSRF TOKEN (THREAD SAFE) =====================

    private val csrfMutex = Mutex()
    private var csrfToken: String? = null
    private var csrfTokenExpiry: Long = 0L

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private suspend fun fetchCsrfTokenUnsafe(): String {
        val response = client.get("$baseUrl/api/csrf-token.php")
        val tokenResponse: StandardizedApiResponse<String> = response.body()

        if (response.status.isSuccess() && tokenResponse.success && tokenResponse.data != null) {
            csrfToken = tokenResponse.data
            csrfTokenExpiry = nowMs() + CSRF_TTL_MS
            return tokenResponse.data
        }
        throw Exception("Failed to obtain CSRF token")
    }

    private suspend fun getCsrfToken(forceRefresh: Boolean = false): String {
        return csrfMutex.withLock {
            val valid = !forceRefresh && csrfToken != null && nowMs() < csrfTokenExpiry
            if (valid) return@withLock csrfToken ?: return@withLock fetchCsrfTokenUnsafe()

            try {
                fetchCsrfTokenUnsafe()
            } catch (e: Exception) {
                logError(message = "Failed to fetch CSRF token", throwable = e)
                throw e
            }
        }
    }

    // ===================== RETRY LOGIC =====================

    private fun Throwable.httpStatusOrNull(): Int? =
        (this as? ResponseException)?.response?.status?.value

    private fun Throwable.retryAfterSecondsOrNull(): Long? =
        (this as? ResponseException)?.response?.headers?.get(HttpHeaders.RetryAfter)?.toLongOrNull()

    private fun shouldRetry(e: Throwable): Boolean {
        if (e is CancellationException) return false
        val status = e.httpStatusOrNull()
        if (status == null) return true
        return status == 429 || status in 500..599
    }

    private suspend inline fun <reified T> executeWithRetry(
        allowRetry: Boolean = true,
        crossinline request: suspend () -> T
    ): T {
        if (RateLimitManager.isRateLimitExceeded()) {
            val resetInSeconds = RateLimitManager.getTimeUntilReset().coerceAtLeast(0)
            if (resetInSeconds > 0) {
                log(message = "Rate limit exceeded. Waiting ${resetInSeconds}s before request.")
                delay(resetInSeconds * 1000L)
            }
        }

        var last: Throwable? = null
        var delayMs = NetworkPolicy.INITIAL_BACKOFF_MS

        repeat(NetworkPolicy.MAX_RETRIES) { attempt ->
            try {
                circuitBreaker.check(nowMs())
                val result = request()
                circuitBreaker.onSuccess()
                return result
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                last = e
                circuitBreaker.onFailure(nowMs())

                val canRetry = allowRetry && attempt < NetworkPolicy.MAX_RETRIES - 1 && shouldRetry(e)
                if (!canRetry) throw e

                val retryAfter = e.retryAfterSecondsOrNull()?.coerceAtLeast(0)
                val waitMs =
                    if (retryAfter != null && retryAfter > 0) retryAfter * 1000L else delayMs

                log("Retry #${attempt + 1} after ${waitMs}ms (status=${e.httpStatusOrNull()})")
                delay(waitMs)

                delayMs = NetworkPolicy.nextBackoffMs(delayMs)
            }
        }

        throw (last as? Exception) ?: Exception("Request failed after ${NetworkPolicy.MAX_RETRIES} attempts")
    }

    // ===================== POST WITH CSRF =====================

    private suspend inline fun <reified T> postWithCsrf(
        url: String,
        body: Any,
        allowRetry: Boolean = true
    ): T {
        return executeWithRetry(allowRetry) {
            val token = getCsrfToken(forceRefresh = false)
            try {
                client.post(url) {
                    contentType(ContentType.Application.Json)
                    headers { append(CSRF_TOKEN_HEADER, token) }
                    addDevelopmentHeaders()
                    setBody(body)
                }.body<T>()
            } catch (e: Throwable) {
                if (e is CancellationException) throw e

                val status = e.httpStatusOrNull()
                val isCsrfLike =
                    status == 403 || status == 419 ||
                            e.message?.contains("CSRF", ignoreCase = true) == true ||
                            e.message?.contains("token", ignoreCase = true) == true

                if (!isCsrfLike) throw e

                log("CSRF token rejected (status=$status). Refreshing token and retrying once.")
                val fresh = getCsrfToken(forceRefresh = true)

                client.post(url) {
                    contentType(ContentType.Application.Json)
                    headers { append(CSRF_TOKEN_HEADER, fresh) }
                    addDevelopmentHeaders()
                    setBody(body)
                }.body<T>()
            }
        }
    }

    // ===================== ERROR MESSAGES =====================

    private fun userFriendlyError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            e is CancellationException -> throw e
            e is HttpRequestTimeoutException || msg.contains("timeout", ignoreCase = true) ->
                "Vrijeme čekanja isteklo"

            msg.contains("UnknownHost", ignoreCase = true) ->
                "Nema internet konekcije"

            msg.contains("Connect", ignoreCase = true) ->
                "Greška povezivanja sa serverom"

            msg.contains("CSRF", ignoreCase = true) ->
                "Sigurnosna greška. Pokušajte ponovo."

            else ->
                "Greška mreže: ${e.message ?: "Nepoznata greška"}"
        }
    }

    // ===================== CACHE INVALIDATION =====================

    /**
     * Read and validate the response body, returning null with an error message on failure.
     */
    private suspend fun readResponseBody(
        response: io.ktor.client.statement.HttpResponse,
        context: String
    ): String? {
        val body = try {
            response.body<String>()
        } catch (e: Throwable) {
            logError("Failed to read response body ($context)", e)
            return null
        }
        if (body.isBlank()) {
            logError("Empty response from server ($context, status: ${response.status.value})")
            return null
        }
        return body
    }

    private suspend fun invalidateVideoRelatedCaches(videoId: String? = null) {
        cache.invalidatePrefix("/api/videos/read.php")
        cache.invalidatePrefix("/api/videos/read-simple.php")
        cache.invalidatePrefix("/api/videos/search.php")
        cache.invalidatePrefix("/api/videos/search-simple.php")
        cache.invalidatePrefix("/api/videos/stats.php")
        cache.invalidatePrefix("/api/ratings/get-by-device.php")

        if (videoId != null) {
            cache.invalidate(cacheKey("/api/videos/stats.php", mapOf("id" to videoId)))
        }
    }

    // ===================== PUBLIC API (SIGNATURES UNCHANGED) =====================

    suspend fun submitRating(rating: RatingRequest): RatingResponse {
        return try {
            log(message = "Submitting rating")

            suspend fun doRequest(token: String) =
                client.post("$baseUrl/api/ratings/create.php") {
                    contentType(ContentType.Application.Json)
                    headers { append(CSRF_TOKEN_HEADER, token) }
                    addDevelopmentHeaders()
                    setBody(rating)
                }

            val initialToken = getCsrfToken(forceRefresh = false)
            val response = try {
                doRequest(token = initialToken)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e

                val status = e.httpStatusOrNull()
                val isCsrfLike =
                    status == 403 || status == 419 ||
                        e.message?.contains("CSRF", ignoreCase = true) == true ||
                        e.message?.contains("token", ignoreCase = true) == true

                if (!isCsrfLike) throw e

                log("CSRF token rejected (status=$status). Refreshing token and retrying once.")
                val fresh = getCsrfToken(forceRefresh = true)
                doRequest(token = fresh)
            }

            val responseJson = try {
                response.body<String>()
            } catch (e: Throwable) {
                logError(message = "Failed to read response body", throwable = e)
                return RatingResponse(
                    success = false,
                    message = "Failed to read server response: ${e.message}"
                )
            }
            log("Server response status: ${response.status.value}, body: '${responseJson.take(200)}'")

            // Check for server errors indicated by successful status but empty body
            if (response.status.value == 200 && responseJson.isBlank()) {
                val errorMsg =
                    "Server returned empty response for successful request (status: ${response.status.value})"
                logError(errorMsg)
                return RatingResponse(
                    success = false,
                    message = "Server processed the request but didn't return a response. This might be a temporary server issue. Please try again."
                )
            }

            if (responseJson.isBlank()) {
                val errorMsg =
                    "Empty response from server (status: ${response.status.value}, headers: ${response.headers})"
                logError(errorMsg)
                return RatingResponse(
                    success = false,
                    message = "Server returned empty response. Please check your connection and try again."
                )
            }

            // Parse JSON response manually
            val parsedResponse =
                jsonConfig.decodeFromString<StandardizedApiResponse<JsonElement>>(responseJson)

            // Invalidate caches
            invalidateVideoRelatedCaches(videoId = rating.videoId)

            val data = parsedResponse.data
            val ratingId = if (data is kotlinx.serialization.json.JsonObject) {
                data["rating_id"]?.jsonPrimitive?.content
            } else null

            RatingResponse(
                success = parsedResponse.success,
                message = parsedResponse.message ?: "Rating submitted successfully",
                ratingId = ratingId
            )
        } catch (e: Throwable) {
            logError(message = "Error submitting rating", throwable = e)
            RatingResponse(success = false, message = userFriendlyError(e = e))
        }
    }

    suspend fun updateRating(rating: RatingRequest): RatingResponse {
        return try {
            log(message = "Updating rating")

            val token = getCsrfToken(forceRefresh = false)
            val response = client.post("$baseUrl/api/ratings/update.php") {
                contentType(ContentType.Application.Json)
                headers { append(CSRF_TOKEN_HEADER, token) }
                addDevelopmentHeaders()
                setBody(rating)
            }

            val responseJson = readResponseBody(response, "updateRating")
                ?: return RatingResponse(
                    success = false,
                    message = "Server returned empty or unreadable response. Please try again."
                )

            val parsedResponse =
                jsonConfig.decodeFromString<StandardizedApiResponse<JsonElement>>(responseJson)

            invalidateVideoRelatedCaches(videoId = rating.videoId)

            val data = parsedResponse.data
            val ratingId = if (data is kotlinx.serialization.json.JsonObject) {
                data["rating_id"]?.jsonPrimitive?.content
            } else null

            RatingResponse(
                success = parsedResponse.success,
                message = parsedResponse.message ?: "Rating updated",
                ratingId = ratingId
            )
        } catch (e: Throwable) {
            logError(message = "Error updating rating", throwable = e)
            RatingResponse(success = false, message = userFriendlyError(e = e))
        }
    }

    suspend fun deleteRating(videoId: String, userToken: String?): ApiResponse {
        return try {
            log(message = "Deleting rating")

            val token = getCsrfToken(forceRefresh = false)
            val response = client.post("$baseUrl/api/ratings/delete.php") {
                contentType(ContentType.Application.Json)
                headers { append(CSRF_TOKEN_HEADER, token) }
                addDevelopmentHeaders()
                setBody(mapOf("videoId" to videoId, "userToken" to userToken))
            }

            val responseJson = readResponseBody(response, "deleteRating")
                ?: return ApiResponse(
                    success = false,
                    message = "Server returned empty or unreadable response. Please try again."
                )

            val parsedResponse = jsonConfig.decodeFromString<ApiResponse>(responseJson)

            invalidateVideoRelatedCaches(videoId = videoId)
            parsedResponse
        } catch (e: Throwable) {
            logError(message = "Error deleting rating", throwable = e)
            ApiResponse(success = false, message = userFriendlyError(e = e))
        }
    }

    suspend fun submitBulkRatings(
        ratings: List<RatingRequest>,
        userToken: String?
    ): BulkRatingResponse {
        fun errorResponse(msg: String) = BulkRatingResponse(
            success = false, message = msg,
            totalProcessed = ratings.size, successCount = 0,
            errorCount = ratings.size, results = emptyList()
        )
        return try {
            log(message = "Submitting bulk ratings: ${ratings.size} ratings")

            val bulkRequest = BulkRatingRequest(userToken = userToken, ratings = ratings)

            val token = getCsrfToken(forceRefresh = false)
            val response = client.post("$baseUrl/api/ratings/bulk-create.php") {
                contentType(ContentType.Application.Json)
                headers { append(CSRF_TOKEN_HEADER, token) }
                addDevelopmentHeaders()
                setBody(bulkRequest)
            }

            val responseJson = readResponseBody(response, "submitBulkRatings")
                ?: return errorResponse("Server returned empty or unreadable response. Please try again.")

            val parsedResponse = jsonConfig.decodeFromString<BulkRatingResponse>(responseJson)

            invalidateVideoRelatedCaches()
            parsedResponse
        } catch (e: Throwable) {
            logError(message = "Error submitting bulk ratings", throwable = e)
            errorResponse(userFriendlyError(e))
        }
    }

    suspend fun getVideoStats(videoId: String): VideoStats {
        return cachedGet(
            path = "/api/videos/stats.php",
            ttlMs = TTL_VIDEO_STATS_MS,
            params = mapOf("id" to videoId)
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/videos/stats.php") { parameter("id", videoId) }.body()
            }
        }
    }

    suspend fun getGospelOfDay(date: String? = null): GospelDayResponse {
        val requestDate = date
        return cachedGet(
            path = "/api/gospel/today.php",
            ttlMs = TTL_GOSPEL_MS,
            params = mapOf("date" to requestDate)
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/gospel/today.php") {
                    if (!requestDate.isNullOrBlank()) parameter("date", requestDate)
                }.body()
            }
        }
    }

    suspend fun getSaintOfDay(date: String? = null): SaintOfDayResponse {
        val todayKey = date ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        return executeWithRetry {
            client.get("$baseUrl/api/saint/today.php") {
                parameter("date", todayKey)
            }.body()
        }
    }

    suspend fun getRandomPassage(): RandomPassageResponse = executeWithRetry {
        client.get("$baseUrl/api/bible/random.php") {
            parameter("ts", Clock.System.now().toEpochMilliseconds())
        }.body()
    }

    suspend fun getBiblePassage(book: String, chapter: String): RandomPassageResponse {
        return cachedGet(
            path = "/api/bible/get.php",
            ttlMs = TTL_BIBLE_PASSAGE_MS,
            params = mapOf("book" to book, "chapter" to chapter)
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/bible/get.php") {
                    parameter("book", book)
                    parameter("chapter", chapter)
                }.body()
            }
        }
    }

    suspend fun searchBible(query: String, limit: Int = 40): BibleSearchResponse {
        val safeQuery = query.trim()
        if (safeQuery.isEmpty()) {
            return BibleSearchResponse(success = false, query = query, message = "Empty query")
        }
        val safeLimit = limit.coerceIn(1, 100)
        return cachedGet(
            path = "/api/bible/search.php",
            ttlMs = TTL_BIBLE_SEARCH_MS,
            params = mapOf(
                "q" to safeQuery,
                "limit" to safeLimit.toString()
            )
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/bible/search.php") {
                    parameter("q", safeQuery)
                    parameter("limit", safeLimit)
                }.body()
            }
        }
    }

    suspend fun getNovenasToday(): NovenaTodayResponse {
        return cachedGet(
            path = "/api/novena/today.php",
            ttlMs = TTL_NOVENA_MS
        ) {
            executeWithRetry { client.get("$baseUrl/api/novena/today.php").body() }
        }
    }

    suspend fun getAllVideos(): List<VideoStats> = executeWithRetry {
        try {
            val response: VideosListResponse = client.get("$baseUrl/api/videos/read-simple.php") {
                parameter("limit", 100)
            }.body()

            response.data.videos.map { video ->
                    VideoStats(
                        videoId = video.id,
                        videoTitle = video.title,
                        videoThumbnail = video.thumbnail,
                        channelName = video.channel_name,
                        category = video.category,
                        averageLove = 0.0,
                        averageFaith = 0.0,
                        averageHope = 0.0,
                        totalRatings = 0
                    )
                }
        } catch (e: Exception) {
            logError("Enhanced API failed, trying legacy format", e)
            client.get("$baseUrl/api/videos/read.php").body()
        }
    }

    suspend fun getVideosList(
        page: Int = 1,
        limit: Int = 20,
        category: String? = null,
        language: String? = null
    ): VideosListResponse {
        return cachedGet(
            path = "/api/videos/read.php",
            ttlMs = TTL_VIDEOS_LIST_MS,
            params = mapOf(
                "page" to page,
                "limit" to limit,
                "category" to category,
                "language" to language
            )
        ) {
            executeWithRetry {
                val response: EnhancedVideosListResponse =
                    client.get("$baseUrl/api/videos/read.php") {
                        parameter("page", page)
                        parameter("limit", limit)
                        category?.let { parameter("category", it) }
                        language?.let { parameter("language", it) }
                    }.body()

                VideosListResponse(
                    success = response.success,
                    data = VideosData(
                        videos = response.data.videos.map { video ->
                            VideoItem(
                                id = video.youtube_id,
                                title = video.title,
                                thumbnail = "",
                                channel_name = video.channel_name,
                                language = video.language,
                                categories = video.categories,
                                created_at = video.created_at
                            )
                        },
                        pagination = PaginationData(
                            current_page = response.data.pagination.current_page,
                            per_page = response.data.pagination.per_page,
                            total = response.data.pagination.total_items,
                            total_pages = response.data.pagination.total_pages
                        )
                    ),
                    timestamp = response.meta?.timestamp ?: Clock.System.now().toEpochMilliseconds()
                )
            }
        }
    }

    suspend fun searchVideosNew(
        query: String,
        page: Int = 1,
        limit: Int = 20,
        category: String? = null,
        language: String? = null
    ): SearchVideosResponse = executeWithRetry {
        client.get("$baseUrl/api/videos/search-simple.php") {
            parameter("q", query)
            parameter("page", page)
            parameter("limit", limit)
            category?.let { parameter("category", it) }
            language?.let { parameter("language", it) }
        }.body()
    }

    suspend fun getYouTubeVideoInfo(videoId: String): YouTubeVideoInfo {
        return cachedGet(
            path = "/api/youtube/info.php",
            ttlMs = TTL_YOUTUBE_VIDEO_INFO_MS,
            params = mapOf("id" to videoId)
        ) {
            executeWithRetry {
                client.get("${com.youtube.rating.shared.YOUTUBE_DATA_BASE_URL}/api/youtube/info.php") {
                    parameter("id", videoId)
                }.body()
            }
        }
    }

    suspend fun searchVideos(
        searchQuery: String? = null,
        minLove: Int = 0,
        minFaith: Int = 0,
        minHope: Int = 0,
        category: String? = null,
        sortBy: String = "latest",
        languages: List<String> = emptyList()
    ): List<VideoSearchResult> = executeWithRetry {
        val response: SearchResponse = client.get("$baseUrl/api/videos/search.php") {
            searchQuery?.let { parameter("search", it) }
            if (minLove > 0) parameter("minLove", minLove)
            if (minFaith > 0) parameter("minFaith", minFaith)
            if (minHope > 0) parameter("minHope", minHope)
            category?.let { parameter("category", it) }
            parameter("sort", sortBy)
            if (languages.isNotEmpty()) parameter("languages", languages.joinToString(","))
        }.body()
        response.videos
    }

    suspend fun searchVideosV2(
        searchQuery: String? = null,
        minLove: Int = 1,
        maxLove: Int = 3,
        minFaith: Int = 1,
        maxFaith: Int = 3,
        minHope: Int = 1,
        maxHope: Int = 3,
        category: String? = null,
        sortBy: String = "latest",
        languages: List<String> = emptyList(),
        page: Int = 1,
        perPage: Int = 20,
        forceRefresh: Boolean = false
    ): PaginatedSearchResponse = executeWithRetry {
        client.get("$baseUrl/api/v2/videos/search.php") {
            searchQuery?.let { parameter("search", it) }
            parameter("minLove", minLove)
            parameter("maxLove", maxLove)
            parameter("minFaith", minFaith)
            parameter("maxFaith", maxFaith)
            parameter("minHope", minHope)
            parameter("maxHope", maxHope)
            category?.let { parameter("category", it) }
            parameter("sort", sortBy)
            parameter("page", page)
            // Server expects `limit` (max 100). Keep `perPage` for backwards compatibility.
            parameter("limit", perPage)
            parameter("perPage", perPage)
            if (forceRefresh) parameter("nocache", "1")
            if (languages.isNotEmpty()) parameter("languages", languages.joinToString(","))
        }.body()
    }

    suspend fun getTopVideos(
        type: String = "rated",
        category: String? = null,
        language: String? = null,
        limit: Int = 10,
        range: String? = null
    ): TopVideosResponse {
        return cachedGet(
            path = "/api/v2/videos/top.php",
            ttlMs = TTL_TOP_VIDEOS_MS,
            params = mapOf(
                "type" to type,
                "category" to category,
                "language" to language,
                "limit" to limit,
                "range" to range
            )
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/v2/videos/top.php") {
                    parameter("type", type)
                    category?.let { parameter("category", it) }
                    language?.let { parameter("language", it) }
                    parameter("limit", limit)
                    range?.let { parameter("range", it) }
                }.body()
            }
        }
    }

    suspend fun getHomeCategories(forceRefresh: Boolean = false): HomeCategoriesResponse {
        val path = "/api/home/categories.php"
        if (forceRefresh) {
            cache.invalidate(cacheKey(path))
        }
        return cachedGet(
            path = path,
            ttlMs = TTL_HOME_CATEGORIES_MS
        ) {
            executeWithRetry {
                client.get("$baseUrl$path").body()
            }
        }
    }

    suspend fun getRatedVideos(
        userToken: String,
        page: Int = 1,
        perPage: Int = 20,
        category: String? = null,
        sortBy: String = "latest"
    ): RatedVideosResponse {
        return cachedGet(
            path = "/api/ratings/get-by-device.php",
            ttlMs = TTL_RATED_VIDEOS_MS,
            params = mapOf(
                "user_token" to userToken,
                "page" to page,
                "per_page" to perPage,
                "category" to category,
                "sort_by" to sortBy
            )
        ) {
            executeWithRetry {
                val response = client.get("$baseUrl/api/ratings/get-by-device.php") {
                    parameter("user_token", userToken)
                    parameter("page", page)
                    parameter("per_page", perPage)
                    category?.let { if (it != "all") parameter("category", it) }
                    parameter("sort_by", sortBy)
                }

                if (response.status.value in 200..299) {
                    response.body()
                } else {
                    val err = try {
                        response.body<String>()
                    } catch (_: Exception) {
                        ""
                    }
                    logError("getRatedVideos server error ${response.status.value}: $err")
                    RatedVideosResponse(success = false, data = emptyList(), pagination = null)
                }
            }
        }
    }

    suspend fun blockUser(blockerUserToken: String, blockedUserToken: String): ApiResponse =
        executeWithRetry(allowRetry = false) {
            val response = client.post("$baseUrl/api/users/block.php") {
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "blockerUserToken" to blockerUserToken,
                        "blockedUserToken" to blockedUserToken
                    )
                )
            }

            val responseJson = try {
                response.body<String>()
            } catch (e: Throwable) {
                logError(message = "Failed to read response body", throwable = e)
                return@executeWithRetry ApiResponse(
                    success = false,
                    message = "Failed to read server response: ${e.message}"
                )
            }

            if (responseJson.isBlank()) {
                val errorMsg = "Empty response from server (status: ${response.status.value})"
                logError(errorMsg)
                return@executeWithRetry ApiResponse(
                    success = false,
                    message = "Server returned empty response. Please check your connection and try again."
                )
            }

            val parsedResponse = jsonConfig.decodeFromString<ApiResponse>(responseJson)
            cache.invalidatePrefix("/api/users/blocked-list.php")
            parsedResponse
        }

    suspend fun unblockUser(blockerUserToken: String, blockedUserToken: String): ApiResponse =
        executeWithRetry(allowRetry = false) {
            val response = client.post("$baseUrl/api/users/unblock.php") {
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "blockerUserToken" to blockerUserToken,
                        "blockedUserToken" to blockedUserToken
                    )
                )
            }

            val responseJson = try {
                response.body<String>()
            } catch (e: Throwable) {
                logError(message = "Failed to read response body", throwable = e)
                return@executeWithRetry ApiResponse(
                    success = false,
                    message = "Failed to read server response: ${e.message}"
                )
            }

            if (responseJson.isBlank()) {
                val errorMsg = "Empty response from server (status: ${response.status.value})"
                logError(errorMsg)
                return@executeWithRetry ApiResponse(
                    success = false,
                    message = "Server returned empty response. Please check your connection and try again."
                )
            }

            val parsedResponse = jsonConfig.decodeFromString<ApiResponse>(responseJson)
            cache.invalidatePrefix("/api/users/blocked-list.php")
            parsedResponse
        }

    suspend fun getBlockedUsers(userToken: String): BlockedUsersResponse {
        return cachedGet(
            path = "/api/users/blocked-list.php",
            ttlMs = TTL_BLOCKED_USERS_MS,
            params = mapOf("userToken" to userToken)
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/users/blocked-list.php") {
                    parameter("userToken", userToken)
                }.body()
            }
        }
    }

    suspend fun deleteVideo(videoId: String): ApiResponse {
        return try {
            log(message = "Deleting video")
            val response = client.post("$baseUrl/api/videos/delete.php") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("videoId" to videoId))
            }

            if (response.status.value in 200..299) {
                val result: ApiResponse = try {
                    response.body()
                } catch (parseError: Exception) {
                    logError(message = "Failed to parse delete response", throwable = parseError)
                    ApiResponse(
                        success = false,
                        message = "Greška parsiranja odgovora: ${parseError.message}"
                    )
                }

                invalidateVideoRelatedCaches(videoId = videoId)
                result
            } else {
                logError("Delete failed with status: ${response.status.value}")
                ApiResponse(
                    success = false,
                    message = when (response.status.value) {
                        404 -> "Video nije pronađen"
                        403 -> "Nemate dozvolu za brisanje"
                        500 -> "Greška servera"
                        else -> "Greška ${response.status.value}"
                    }
                )
            }
        } catch (e: Exception) {
            logError(message = "Error deleting video", throwable = e)
            ApiResponse(success = false, message = "Greška pri brisanju: ${e.message}")
        }
    }

    suspend fun syncFavorites(
        userToken: String,
        favorites: List<FavoriteVideoDto>
    ): SyncFavoritesResponse =
        executeWithRetry(allowRetry = false) {
            val token = getCsrfToken(forceRefresh = false)
            val res: SyncFavoritesResponse = client.post("$baseUrl/api/favorites/sync.php") {
                contentType(ContentType.Application.Json)
                headers { append(CSRF_TOKEN_HEADER, token) }
                addDevelopmentHeaders()
                setBody(SyncFavoritesRequest(user_token = userToken, favorites = favorites))
            }.body()
            invalidateVideoRelatedCaches()
            res
        }

    suspend fun syncPsalmHighlights(
        userToken: String,
        highlights: List<String>,
        displayName: String? = null,
        gender: String? = null,
        notMarried: Boolean? = null
    ): PsalmHighlightsSyncResponse =
        executeWithRetry(allowRetry = false) {
            val token = getCsrfToken(forceRefresh = false)
            client.post("$baseUrl/api/psalms/sync-highlights.php") {
                contentType(ContentType.Application.Json)
                headers { append(CSRF_TOKEN_HEADER, token) }
                addDevelopmentHeaders()
                setBody(
                    PsalmHighlightsSyncRequest(
                        userToken = userToken,
                        highlights = highlights,
                        displayName = displayName,
                        gender = gender,
                        notMarried = notMarried
                    )
                )
            }.body()
        }

    suspend fun reportVideo(
        videoId: String,
        userToken: String,
        reason: String = "inappropriate"
    ): ApiResponse =
        executeWithRetry(allowRetry = false) {
            val res: ApiResponse = client.post("$baseUrl/api/reports/report-video.php") {
                setBody(
                    mapOf(
                        "videoId" to videoId,
                        "userToken" to userToken,
                        "deviceId" to userToken,
                        "reason" to reason
                    )
                )
            }.body()
            invalidateVideoRelatedCaches(videoId = videoId)
            res
        }

    suspend fun adminDeleteVideo(videoId: String, adminToken: String): ApiResponse =
        executeWithRetry(allowRetry = false) {
            val res: ApiResponse = client.post("$baseUrl/api/admin/admin-delete-video.php") {
                setBody(mapOf("videoId" to videoId, "adminToken" to adminToken))
            }.body()
            invalidateVideoRelatedCaches(videoId = videoId)
            res
        }

    suspend fun adminToggleVideo(
        videoId: String,
        isDisabled: Boolean,
        adminToken: String
    ): ApiResponse =
        executeWithRetry(allowRetry = false) {
            val res: ApiResponse = client.post("$baseUrl/api/admin/admin-toggle-video.php") {
                setBody(
                    mapOf(
                        "videoId" to videoId,
                        "isDisabled" to isDisabled,
                        "adminToken" to adminToken
                    )
                )
            }.body()
            invalidateVideoRelatedCaches(videoId = videoId)
            res
        }

    suspend fun submitCrashReport(crashReport: com.youtube.rating.shared.models.CrashReport): com.youtube.rating.shared.models.CrashReportResponse {
        return try {
            val crashBase = com.youtube.rating.shared.CRASH_BASE_URL.ifBlank { baseUrl }
            client.post("$crashBase/api/crash-reports/submit.php") {
                contentType(ContentType.Application.Json)
                setBody(crashReport)
            }.body()
        } catch (e: Exception) {
            com.youtube.rating.shared.models.CrashReportResponse(
                success = false,
                message = "Failed to submit crash report: ${e.message}"
            )
        }
    }

    suspend fun getPrayerRequests(
        userToken: String,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean = false
    ): PrayerPageDto {
        val loader: suspend () -> PrayerPageDto = {
            executeWithRetry {
                client.get("$baseUrl/api/prayer/requests.php") {
                    parameter("userToken", userToken)
                    parameter("page", page)
                    parameter("pageSize", pageSize)
                    headers.append(HttpHeaders.AcceptCharset, "utf-8")
                }.body()
            }
        }
        val loaderNoCache: suspend () -> PrayerPageDto = {
            executeWithRetry {
                client.get("$baseUrl/api/prayer/requests.php") {
                    parameter("userToken", userToken)
                    parameter("page", page)
                    parameter("pageSize", pageSize)
                    parameter("_ts", Clock.System.now().toEpochMilliseconds())
                    headers.append(HttpHeaders.AcceptCharset, "utf-8")
                    headers.append(HttpHeaders.CacheControl, "no-cache")
                    headers.append("Pragma", "no-cache")
                }.body()
            }
        }

        return if (forceRefresh) {
            loaderNoCache()
        } else {
            cachedGet(
                path = "/api/prayer/requests.php",
                ttlMs = TTL_PRAYER_REQUESTS_MS,
                params = mapOf("userToken" to userToken, "page" to page, "pageSize" to pageSize),
                loader = loader
            )
        }
    }

    suspend fun createPrayerRequest(body: CreatePrayerRequestBody): PrayerRequestDto =
        executeWithRetry(allowRetry = false) {
            val token = getCsrfToken(forceRefresh = false)
            val res: PrayerRequestDto = client.post("$baseUrl/api/prayer/create-request.php") {
                headers {
                    append(HttpHeaders.AcceptCharset, "utf-8")
                    append(CSRF_TOKEN_HEADER, token)
                }
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                addDevelopmentHeaders()
                setBody(body)
            }.body()
            cache.invalidatePrefix("/api/prayer/requests.php")
            res
        }

    suspend fun setPrayerPrayed(requestId: String, body: SetPrayerPrayedBody): PrayerRequestDto =
        executeWithRetry(allowRetry = false) {
            val res: PrayerRequestDto = client.post("$baseUrl/api/prayer/set-prayed.php") {
                headers { append(HttpHeaders.AcceptCharset, "utf-8") }
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(
                    com.youtube.rating.shared.data.SetPrayerPrayedRequest(
                        requestId = requestId,
                        userToken = body.userToken,
                        prayed = body.prayed
                    )
                )
            }.body()
            cache.invalidatePrefix("/api/prayer/requests.php")
            res
        }

    suspend fun getPrayerEncouragements(
        requestId: String,
        userToken: String
    ): List<EncouragementDto> {
        return cachedGet(
            path = "/api/prayer/encouragements.php",
            ttlMs = TTL_PRAYER_ENCOURAGEMENTS_MS,
            params = mapOf("requestId" to requestId, "userToken" to userToken)
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/prayer/encouragements.php") {
                    parameter("requestId", requestId)
                    parameter("userToken", userToken)
                    headers { append(HttpHeaders.AcceptCharset, "utf-8") }
                }.body()
            }
        }
    }

    suspend fun addPrayerEncouragement(
        requestId: String,
        body: AddEncouragementBody
    ): EncouragementDto =
        executeWithRetry(allowRetry = false) {
            val res: EncouragementDto = client.post("$baseUrl/api/prayer/add-encouragement.php") {
                headers { append(HttpHeaders.AcceptCharset, "utf-8") }
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(
                    mapOf(
                        "requestId" to requestId,
                        "userToken" to body.userToken,
                        "message" to body.message,
                        "authorName" to body.authorName
                    )
                )
            }.body()
            cache.invalidatePrefix("/api/prayer/encouragements.php")
            res
        }

    suspend fun deletePrayerRequest(requestId: String, adminToken: String): ApiResponse =
        executeWithRetry(allowRetry = false) {
            val res: ApiResponse = client.post("$baseUrl/api/prayer/delete-prayer.php") {
                headers { append(HttpHeaders.AcceptCharset, "utf-8") }
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(mapOf("requestId" to requestId, "adminToken" to adminToken))
            }.body()
            cache.invalidatePrefix("/api/prayer/requests.php")
            res
        }

    suspend fun anonymousRegister(): AnonymousRegisterResponse = executeWithRetry {
        client.post("$baseUrl/api/users/anon-register.php").body()
    }

    suspend fun getGalleryList(forceRefresh: Boolean = false): GalleryListResponse {
        if (forceRefresh) {
            cache.invalidate("/api/gallery/list.php")
        }

        return cachedGet(
            path = "/api/gallery/list.php",
            ttlMs = TTL_GALLERY_MS
        ) {
            executeWithRetry {
                client.get("$baseUrl/api/gallery/list.php").body()
            }
        }
    }

    suspend fun healthCheck(): HealthCheckResponse {
        return cachedGet(
            path = "/api/health.php",
            ttlMs = TTL_HEALTH_MS
        ) {
            executeWithRetry {
                val response = client.get("$baseUrl/api/health.php")
                val text = response.bodyAsText()

                val element: JsonElement = try {
                    jsonConfig.parseToJsonElement(text)
                } catch (_: Exception) {
                    return@executeWithRetry HealthCheckResponse(
                        success = false,
                        message = "Invalid health response (not JSON)",
                        data = null,
                        meta = null
                    )
                }

                // Support both legacy wrapped responses and the current backend format.
                if (element is JsonObject && element.containsKey("success")) {
                    return@executeWithRetry jsonConfig.decodeFromJsonElement(HealthCheckResponse.serializer(), element)
                }

                val raw = try {
                    jsonConfig.decodeFromJsonElement(
                        com.youtube.rating.shared.models.HealthCheckRawResponse.serializer(),
                        element
                    )
                } catch (_: Exception) {
                    return@executeWithRetry HealthCheckResponse(
                        success = false,
                        message = "Invalid health response schema",
                        data = null,
                        meta = null
                    )
                }

                val isHealthy = raw.status == "healthy"
                val timestampMs = try {
                    Instant.parse(raw.timestamp).toEpochMilliseconds()
                } catch (_: Exception) {
                    Clock.System.now().toEpochMilliseconds()
                }

                val mappedChecks = raw.checks.mapValues { (_, item) ->
                    com.youtube.rating.shared.models.HealthCheckItem(
                        status = item.status,
                        message = item.message,
                        response_time = item.durationMs?.let { "${it}ms" },
                        error_count = null
                    )
                }

                HealthCheckResponse(
                    success = isHealthy,
                    message = if (isHealthy) "OK" else "Backend unhealthy",
                    data = com.youtube.rating.shared.models.HealthData(
                        status = raw.status,
                        version = raw.version ?: "unknown",
                        timestamp = timestampMs,
                        checks = mappedChecks,
                        performance = null
                    ),
                    meta = null
                )
            }
        }
    }

    /**
     * Get usage analytics for user
     */
    suspend fun getUsageAnalytics(
        userToken: String,
        period: String = "month", // "week", "month", "year"
        startDate: String? = null,
        endDate: String? = null
    ): UsageAnalyticsResponse = executeWithRetry {
        client.get("$baseUrl/api/analytics/usage-stats.php") {
            parameter("userToken", userToken)
            parameter("period", period)
            startDate?.let { parameter("startDate", it) }
            endDate?.let { parameter("endDate", it) }
        }.body()
    }

    /**
     * Get daily usage statistics
     */
    suspend fun getDailyUsageStats(
        userToken: String,
        date: String // YYYY-MM-DD format
    ): DailyUsageStats = executeWithRetry {
        client.get("$baseUrl/api/analytics/daily-stats.php") {
            parameter("userToken", userToken)
            parameter("date", date)
        }.body()
    }

    /**
     * Get usage summary for a period
     */
    suspend fun getUsageSummary(
        userToken: String,
        period: String = "month",
        startDate: String? = null,
        endDate: String? = null
    ): UsageSummary = executeWithRetry {
        client.get("$baseUrl/api/analytics/usage-summary.php") {
            parameter("userToken", userToken)
            parameter("period", period)
            startDate?.let { parameter("startDate", it) }
            endDate?.let { parameter("endDate", it) }
        }.body()
    }

    /**
     * Get popular search terms for suggestions
     */
    suspend fun getPopularSearchTerms(limit: Int = 9, language: String = "hr"): List<String> {
        return try {
            val response = client.get("$baseUrl/api/v2/search/popular.php") {
                parameter("limit", limit)
                parameter("language", language)
            }

            if (!response.status.isSuccess()) return emptyList()

            val body: PopularTermsResponse = response.body()
            if (body.success) body.terms.map { it.term } else emptyList()

        } catch (e: kotlinx.coroutines.CancellationException) {
            // NORMALNO u Compose-u (napusti kompoziciju, navigacija, recomposition)
            emptyList()
        } catch (e: Exception) {
            logError(message = "Error fetching popular search terms", throwable = e)
            emptyList()
        }
    }

    suspend fun recordWatchHistory(payload: WatchHistoryRecordRequest): ApiResponse =
        executeWithRetry(allowRetry = false) {
            client.post("$baseUrl/api/watch-history/create.php") {
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(payload)
            }.body()
        }

    suspend fun getWatchHistory(
        userToken: String,
        page: Int = 1,
        perPage: Int = 20
    ): JsonObject = executeWithRetry {
        val text = client.get("$baseUrl/api/watch-history/get-by-device.php") {
            parameter("page", page)
            parameter("per_page", perPage)
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer $userToken")
            }
        }.bodyAsText()
        jsonConfig.parseToJsonElement(text).jsonObject
    }

    suspend fun toggleWatchHistory(payload: WatchHistoryToggleRequest): ApiResponse =
        executeWithRetry(allowRetry = false) {
            client.post("$baseUrl/api/watch-history/toggle-settings.php") {
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(payload)
            }.body()
        }

    suspend fun clearWatchHistory(payload: WatchHistoryClearRequest): ApiResponse =
        executeWithRetry(allowRetry = false) {
            client.post("$baseUrl/api/watch-history/clear.php") {
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(payload)
            }.body()
        }

    suspend fun submitBugReport(payload: BugReportRequest): JsonObject = executeWithRetry(allowRetry = false) {
        val text = client.post("$baseUrl/api/reports/report-bug.php") {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            headers { append(HttpHeaders.Accept, "application/json") }
            setBody(payload)
        }.bodyAsText()
        runCatching { jsonConfig.parseToJsonElement(text).jsonObject }.getOrElse {
            buildJsonObject {
                put("success", false)
                put("error", "Invalid JSON response")
                put("raw", text.take(300))
            }
        }
    }

    fun close() {
        client.close()
    }
}
