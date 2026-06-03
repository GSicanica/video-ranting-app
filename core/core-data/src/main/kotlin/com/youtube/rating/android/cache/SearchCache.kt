package com.youtube.rating.android.cache

import java.util.LinkedHashMap

/**
 * Cache key for search API requests
 * Used to uniquely identify API calls for caching purposes
 */
data class SearchCacheKey(
    val searchQuery: String,
    val category: String?,
    val minLove: Int,
    val maxLove: Int,
    val minFaith: Int,
    val maxFaith: Int,
    val minHope: Int,
    val maxHope: Int,
    val languages: List<String>,
    val sortBy: String?,
    val page: Int,
    val perPage: Int
) {
    override fun hashCode(): Int {
        var result = searchQuery.hashCode()
        result = 31 * result + (category?.hashCode() ?: 0)
        result = 31 * result + minLove
        result = 31 * result + maxLove
        result = 31 * result + minFaith
        result = 31 * result + maxFaith
        result = 31 * result + minHope
        result = 31 * result + maxHope
        result = 31 * result + languages.hashCode()
        result = 31 * result + (sortBy?.hashCode() ?: 0)
        result = 31 * result + page
        result = 31 * result + perPage
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchCacheKey) return false

        if (searchQuery != other.searchQuery) return false
        if (category != other.category) return false
        if (minLove != other.minLove) return false
        if (maxLove != other.maxLove) return false
        if (minFaith != other.minFaith) return false
        if (maxFaith != other.maxFaith) return false
        if (minHope != other.minHope) return false
        if (maxHope != other.maxHope) return false
        if (languages != other.languages) return false
        if (page != other.page) return false
        if (perPage != other.perPage) return false
        if (sortBy != other.sortBy) return false

        return true
    }
}

/**
 * LRU Cache for API responses
 * 
 * Features:
 * - Automatic eviction of oldest entries when capacity reached
 * - TTL (Time To Live) for entries
 * - Thread-safe operations
 * - Memory-efficient
 * 
 * @param maxSize Maximum number of cached entries
 * @param ttlMs Time to live in milliseconds (default: 5 minutes)
 */
class SearchCache<T>(
    private val maxSize: Int = 20,
    private val ttlMs: Long = 5 * 60 * 1000L // 5 minutes
) {
    private val cache = object : LinkedHashMap<SearchCacheKey, CacheEntry<T>>(
        maxSize,
        0.75f,
        true // accessOrder = true (LRU)
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SearchCacheKey, CacheEntry<T>>?): Boolean {
            return size > maxSize
        }
    }

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long
    )

    /**
     * Get cached data if available and not expired
     * 
     * @param key The cache key
     * @return Cached data or null if not found or expired
     */
    @Synchronized
    fun get(key: SearchCacheKey): T? {
        val entry = cache[key] ?: return null
        
        // Check if expired
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            cache.remove(key)
            return null
        }
        
        return entry.data
    }

    /**
     * Put data into cache
     * 
     * @param key The cache key
     * @param data The data to cache
     */
    @Synchronized
    fun put(key: SearchCacheKey, data: T) {
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    /**
     * Check if key exists in cache and is not expired
     */
    @Synchronized
    fun contains(key: SearchCacheKey): Boolean {
        return get(key) != null
    }

    /**
     * Clear all cached entries
     */
    @Synchronized
    fun clear() {
        cache.clear()
    }

    /**
     * Remove specific entry from cache
     */
    @Synchronized
    fun remove(key: SearchCacheKey) {
        cache.remove(key)
    }

    /**
     * Get cache statistics
     */
    @Synchronized
    fun getStats(): CacheStats {
        val now = System.currentTimeMillis()
        val validEntries = cache.values.count { now - it.timestamp <= ttlMs }
        
        return CacheStats(
            totalEntries = cache.size,
            validEntries = validEntries,
            expiredEntries = cache.size - validEntries,
            maxSize = maxSize,
            ttlMs = ttlMs
        )
    }

    /**
     * Remove expired entries (manual cleanup)
     */
    @Synchronized
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val iterator = cache.entries.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.timestamp > ttlMs) {
                iterator.remove()
            }
        }
    }
}

/**
 * Cache statistics for monitoring
 */
data class CacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int,
    val maxSize: Int,
    val ttlMs: Long
) {
    val utilizationPercent: Double
        get() = (totalEntries * 100.0) / maxSize

    override fun toString(): String = """
        Cache Stats:
        - Total Entries: $totalEntries
        - Valid Entries: $validEntries
        - Expired Entries: $expiredEntries
        - Utilization: ${utilizationPercent.toInt()}%
        - Max Size: $maxSize
        - TTL: ${ttlMs / 1000}s
    """.trimIndent()
}
