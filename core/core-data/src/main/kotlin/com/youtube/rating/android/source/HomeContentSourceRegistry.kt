package com.youtube.rating.android.source

import java.util.concurrent.atomic.AtomicReference

/**
 * Simple registry to enable plugin-like content sources.
 * Default is the first registered source.
 */
class HomeContentSourceRegistry(
    sources: List<HomeContentSource>
) {
    private val sourceMap: Map<String, HomeContentSource> = sources.associateBy { it.id }
    private val activeSourceId = AtomicReference(sources.firstOrNull()?.id)

    fun all(): List<HomeContentSource> = sourceMap.values.toList()

    fun active(): HomeContentSource {
        val id = activeSourceId.get()
        return if (id != null) {
            sourceMap[id] ?: sourceMap.values.first()
        } else {
            sourceMap.values.first()
        }
    }

    fun setActive(id: String): Boolean {
        if (!sourceMap.containsKey(id)) return false
        activeSourceId.set(id)
        return true
    }
}
