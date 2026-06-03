package com.youtube.rating.android.cache

import java.util.LinkedHashMap

/**
 * Generic LRU cache with TTL for API responses.
 * Can be used with any key/value types across ViewModels.
 *
 * @param maxSize Maximum number of cached entries
 * @param ttlMs Time to live in milliseconds
 */
class ApiCache<K : Any, V : Any>(
    private val maxSize: Int = 20,
    private val ttlMs: Long = 2 * 60 * 1000L // 2 minutes default
) {
    private val cache = object : LinkedHashMap<K, CacheEntry<V>>(
        maxSize, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, CacheEntry<V>>?): Boolean {
            return size > maxSize
        }
    }

    private data class CacheEntry<V>(
        val data: V,
        val timestamp: Long
    )

    @Synchronized
    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            cache.remove(key)
            return null
        }
        return entry.data
    }

    @Synchronized
    fun put(key: K, data: V) {
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }

    @Synchronized
    fun invalidate(key: K) {
        cache.remove(key)
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
