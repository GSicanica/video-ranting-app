package com.youtube.rating.android.data.cache

import com.youtube.rating.android.util.CacheConstants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Generic cache manager with expiration support
 * Thread-safe implementation using Mutex
 */
class CacheManager<K, V> {
    
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Long
    )
    
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val mutex = Mutex()
    
    /**
     * Get cached value if exists and not expired
     * @param key Cache key
     * @param maxAge Maximum age in milliseconds
     * @return Cached value or null if not found/expired
     */
    suspend fun get(key: K, maxAge: Long = CacheConstants.CACHE_DURATION_DEFAULT): V? {
        return mutex.withLock {
            val entry = cache[key] ?: return null
            
            val now = System.currentTimeMillis()
            val age = now - entry.timestamp
            
            if (age > maxAge) {
                // Expired - remove from cache
                cache.remove(key)
                null
            } else {
                entry.value
            }
        }
    }
    
    /**
     * Put value in cache with current timestamp
     * @param key Cache key
     * @param value Value to cache
     */
    suspend fun put(key: K, value: V) {
        mutex.withLock {
            cache[key] = CacheEntry(value, System.currentTimeMillis())
        }
    }
    
    /**
     * Check if cache has valid (non-expired) entry
     * @param key Cache key
     * @param maxAge Maximum age in milliseconds
     * @return true if valid entry exists
     */
    suspend fun hasValid(key: K, maxAge: Long = CacheConstants.CACHE_DURATION_DEFAULT): Boolean {
        return get(key, maxAge) != null
    }
    
    /**
     * Invalidate (remove) cache entry
     * @param key Cache key
     */
    suspend fun invalidate(key: K) {
        mutex.withLock {
            cache.remove(key)
        }
    }
    
    /**
     * Clear all cache entries
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    /**
     * Remove expired entries
     * @param maxAge Maximum age in milliseconds
     */
    suspend fun cleanExpired(maxAge: Long = CacheConstants.CACHE_DURATION_DEFAULT) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val keysToRemove = cache.entries
                .filter { (now - it.value.timestamp) > maxAge }
                .map { it.key }
            
            keysToRemove.forEach { cache.remove(it) }
        }
    }
    
    /**
     * Get cache size
     */
    fun size(): Int = cache.size
    
    /**
     * Get all cached keys
     */
    fun keys(): Set<K> = cache.keys.toSet()
}

/**
 * Cache key for video statistics
 */
data class VideoStatsCacheKey(val videoId: String)

/**
 * Cache key for video search results
 */
data class VideoSearchCacheKey(
    val query: String,
    val category: String?,
    val sortBy: String,
    val languages: List<String>
)

/**
 * Cache key for user ratings
 */
data class UserRatingsCacheKey(val userToken: String)
