package com.youtube.rating.shared.common

import kotlin.reflect.KClass

/**
 * In-memory (process-lifetime) cache for small ephemeral values.
 *
 * Notes:
 * - This is not persistent storage. Process death clears it.
 * - Keep values small and avoid storing Context/View/Activity to prevent leaks.
 */
object InMemoryCache {
    private val map = HashMap<String, Any?>()

    fun put(key: String, value: Any?): InMemoryCache {
        map[key] = value
        return this
    }

    operator fun get(key: String): Any? = map[key]

    fun contains(key: String) = map.containsKey(key)

    fun clear() = map.clear()

    fun getAll(): Map<String, Any?> = map.toMap()

    fun getAllByType(clazz: KClass<*>) = getAll().filter {
        val classValue = it.value
        classValue != null && classValue::class == clazz
    }
}

fun <T> getFromMemory(key: String): T? = InMemoryCache[key] as? T

fun putInMemory(key: String, any: Any?) = InMemoryCache.put(key, any)
