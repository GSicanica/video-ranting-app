package com.youtube.rating.android.cache

import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.GospelDayResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared singleton cache for gospel data.
 * Prevents duplicate API calls when both GospelOfDayViewModel and
 * BiblePlannerViewModel request the same gospel for the same date.
 *
 * Registered as Koin singleton.
 */
class GospelCache(private val apiClient: RatingApiClient) {

    private var cachedDate: String? = null
    private var cachedResponse: GospelDayResponse? = null
    private var cacheTimestamp: Long = 0L

    companion object {
        private const val TTL_MS = 15 * 60 * 1000L // 15 minutes (matches server Cache-Control)
    }

    private val mutex = Mutex()

    /**
     * Returns cached gospel if available for the given date,
     * otherwise fetches from server and caches.
     */
    suspend fun getGospel(date: String? = null, force: Boolean = false): GospelDayResponse {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && cachedDate == date && cachedResponse != null && (now - cacheTimestamp) < TTL_MS) {
                return cachedResponse ?: GospelDayResponse(success = false, message = "Cached response is null")
            }
        }

        val resp = apiClient.getGospelOfDay(date)
        if (resp.success) {
            mutex.withLock {
                cachedDate = date
                cachedResponse = resp
                cacheTimestamp = System.currentTimeMillis()
            }
        }
        return resp
    }

    fun invalidate() {
        cachedResponse = null
        cachedDate = null
        cacheTimestamp = 0L
    }
}
