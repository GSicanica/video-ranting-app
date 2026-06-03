package com.youtube.rating.ioscomposeapp.platform

import java.util.concurrent.atomic.AtomicLong

internal actual object PlatformNotesStore {
    private val nextId = AtomicLong(1L)
    private val lock = Any()

    @Volatile
    private var cached: List<NoteItem> = emptyList()

    actual fun load(): List<NoteItem> = synchronized(lock) { cached }

    actual fun add(text: String): List<NoteItem> = synchronized(lock) {
        val now = System.currentTimeMillis()
        val item = NoteItem(
            id = nextId.getAndIncrement(),
            text = text,
            createdAtMs = now
        )
        cached = listOf(item) + cached
        cached
    }

    actual fun delete(id: Long): List<NoteItem> = synchronized(lock) {
        cached = cached.filterNot { it.id == id }
        cached
    }
}

