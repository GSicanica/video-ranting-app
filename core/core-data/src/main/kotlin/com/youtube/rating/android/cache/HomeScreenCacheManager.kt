package com.youtube.rating.android.cache

import com.youtube.rating.core.coroutines.ioDispatcher

import com.youtube.rating.shared.data.HomeScreenCacheModel
import com.youtube.rating.shared.data.HomeScreenCacheRepository
import com.youtube.rating.shared.models.PaginatedSearchResponse
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Hybrid cache manager that combines in-memory LRU cache with persistent Realm storage
 * for improved home screen caching performance and offline capabilities.
 */
class HomeScreenCacheManager(
    private val memoryCache: SearchCache<PaginatedSearchResponse> = SearchCache(
        maxSize = 50,
        ttlMs = 15 * 60 * 1000L // 15 minutes for memory cache
    ),
    private val persistentRepository: HomeScreenCacheRepository
) {

    companion object {
        private const val TAG = "HomeScreenCacheManager"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private suspend inline fun <T> io(crossinline block: suspend () -> T): T =
        withContext(ioDispatcher) { block() }

    /**
     * Get cached response, checking memory cache first, then persistent storage
     */
    suspend fun getCachedResponse(cacheKey: SearchCacheKey): PaginatedSearchResponse? = io {
        runCatching {
            memoryCache.get(cacheKey)?.also {
                if (LogConfig.ENABLE_LOGS) Logger.debug(TAG, "MEMORY CACHE HIT: $cacheKey")
            } ?: getPersistentCachedResponse(cacheKey = cacheKey)
        }.onFailure { e ->
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            if (LogConfig.ENABLE_LOGS) Logger.warning(TAG, "CACHE ERROR: ${e.message}")
        }.getOrNull()
    }

    private suspend fun getPersistentCachedResponse(cacheKey: SearchCacheKey): PaginatedSearchResponse? {
        val persistentKey = buildPersistentCacheKey(cacheKey = cacheKey)
        val persistentModel = persistentRepository.getCachedResponse(persistentKey) ?: run {
            if (LogConfig.ENABLE_LOGS) Logger.debug(TAG, "CACHE MISS: $cacheKey")
            return null
        }

        return runCatching {
            json.decodeFromString<PaginatedSearchResponse>(persistentModel.responseData)
        }.onSuccess { response ->
            memoryCache.put(cacheKey, response)
            if (LogConfig.ENABLE_LOGS) Logger.debug(TAG, "PERSISTENT CACHE HIT: $cacheKey")
        }.onFailure { e ->
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            markPersistentEntryCorrupted(model = persistentModel)
            if (LogConfig.ENABLE_LOGS) Logger.warning(TAG, "CORRUPTED PERSISTENT CACHE: $cacheKey")
        }.getOrNull()
    }

    private suspend fun markPersistentEntryCorrupted(model: HomeScreenCacheModel) {
        runCatching {
            persistentRepository.saveCachedResponse(
                model.copy(
                    responseData = "",
                    expiresAt = 0L,
                    timestamp = System.currentTimeMillis(),
                    lastAccessed = System.currentTimeMillis(),
                    accessCount = 0
                )
            )
        }
    }

    /**
     * Save response to both memory and persistent caches
     */
    suspend fun saveResponse(cacheKey: SearchCacheKey, response: PaginatedSearchResponse) {
        io {
            runCatching {
                memoryCache.put(cacheKey, response)

                val persistentKey = buildPersistentCacheKey(cacheKey = cacheKey)
                val jsonData = json.encodeToString(PaginatedSearchResponse.serializer(), response)

                persistentRepository.saveCachedResponse(
                    HomeScreenCacheModel(
                        cacheKey = persistentKey,
                        searchQuery = cacheKey.searchQuery,
                        category = cacheKey.category,
                        minLove = cacheKey.minLove,
                        maxLove = cacheKey.maxLove,
                        minFaith = cacheKey.minFaith,
                        maxFaith = cacheKey.maxFaith,
                        minHope = cacheKey.minHope,
                        maxHope = cacheKey.maxHope,
                        languages = cacheKey.languages,
                        sortBy = cacheKey.sortBy,
                        page = cacheKey.page,
                        perPage = cacheKey.perPage,
                        responseData = jsonData,
                        timestamp = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (30 * 60 * 1000L), // 30 minutes
                        accessCount = 0,
                        lastAccessed = System.currentTimeMillis()
                    )
                )

                if (LogConfig.ENABLE_LOGS) Logger.debug(TAG, "SAVED TO CACHE: $cacheKey")
            }.onFailure { e ->
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                if (LogConfig.ENABLE_LOGS) Logger.warning(TAG, "SAVE CACHE ERROR: ${e.message}")
            }
        }
    }

    /**
     * Clear all caches (both memory and persistent)
     */
    suspend fun clearAll() {
        io {
            runCatching {
                memoryCache.clear()
                persistentRepository.clearAll()
                if (LogConfig.ENABLE_LOGS) Logger.info(TAG, "CLEARED ALL CACHES")
            }.onFailure { e ->
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                if (LogConfig.ENABLE_LOGS) Logger.warning(TAG, "CLEAR CACHE ERROR: ${e.message}")
            }
        }
    }

    /**
     * Clean up expired entries from persistent storage
     */
    suspend fun cleanupExpiredEntries(): Int = io {
        runCatching {
            when (val result = persistentRepository.deleteExpiredEntries()) {
                is com.youtube.rating.shared.data.DataResult.Success -> result.data
                else -> 0
            }
        }.onFailure { e ->
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            if (LogConfig.ENABLE_LOGS) Logger.warning(TAG, "CLEANUP ERROR: ${e.message}")
        }.getOrDefault(0)
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats = io {
        runCatching {
            val persistentStats = persistentRepository.getCacheStats()
            val memoryStats = memoryCache.getStats()
            CacheStats(
                totalEntries = persistentStats.totalEntries + memoryStats.totalEntries,
                validEntries = persistentStats.validEntries + memoryStats.validEntries,
                expiredEntries = persistentStats.expiredEntries + memoryStats.expiredEntries,
                maxSize = persistentStats.maxSize + memoryStats.maxSize,
                ttlMs = maxOf(persistentStats.ttlMs, memoryStats.ttlMs)
            )
        }.onFailure { e ->
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        }.getOrDefault(CacheStats(totalEntries = 0, validEntries = 0, expiredEntries = 0, maxSize = 0, ttlMs = 0))
    }

    /**
     * Preload common cache entries for better UX
     */
    suspend fun preloadCommonEntries(languageCodes: List<String>) {
        io {
            runCatching {
                // Preload default "most_rated" sort
                val defaultKey = SearchCacheKey(
                    searchQuery = "",
                    category = null,
                    minLove = 1, maxLove = 3,
                    minFaith = 1, maxFaith = 3,
                    minHope = 1, maxHope = 3,
                    languages = languageCodes,
                    page = 1,
                    perPage = 20,
                    sortBy = "most_rated"
                )

                // Only preload if not already cached
                if (getCachedResponse(cacheKey = defaultKey) == null) {
                    // Note: Actual preloading would require API call, this just warms the cache structure
                    if (LogConfig.ENABLE_LOGS) Logger.debug(TAG, "PRELOAD SETUP COMPLETE")
                }
            }.onFailure { e ->
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                if (LogConfig.ENABLE_LOGS) Logger.warning(TAG, "PRELOAD ERROR: ${e.message}")
            }
        }
    }

    /**
     * Build a consistent cache key for persistent storage
     */
    private fun buildPersistentCacheKey(cacheKey: SearchCacheKey): String {
        return buildString {
            append("home|q=").append(cacheKey.searchQuery)
            append("|cat=").append(cacheKey.category ?: "")
            append("|love=").append(cacheKey.minLove).append("-").append(cacheKey.maxLove)
            append("|faith=").append(cacheKey.minFaith).append("-").append(cacheKey.maxFaith)
            append("|hope=").append(cacheKey.minHope).append("-").append(cacheKey.maxHope)
            append("|lang=").append(cacheKey.languages.sorted().joinToString(","))
            append("|sort=").append(cacheKey.sortBy ?: "")
            append("|page=").append(cacheKey.page)
            append("|per=").append(cacheKey.perPage)
        }
    }
}
