package com.youtube.rating.ioscomposeapp.platform

import com.youtube.rating.ioscomposeapp.storage.IosPrefs
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max

internal actual object PlatformNotesStore {
    private const val KEY = "notes_v1"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Serializable
    private data class StoredNote(val id: Long, val text: String, val createdAtMs: Long)

    private fun decode(raw: String?): List<StoredNote> {
        val v = raw?.trim().orEmpty()
        if (v.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<StoredNote>>(v) }.getOrElse { emptyList() }
    }

    private fun encode(list: List<StoredNote>): String = json.encodeToString(list)

    private fun nowMs(): Long = kotlin.system.getTimeMillis()

    private fun nextId(existing: List<StoredNote>): Long {
        val maxId = existing.maxOfOrNull { it.id } ?: 0L
        return maxId + 1L
    }

    actual fun load(): List<NoteItem> {
        val stored = decode(IosPrefs.getString(KEY))
        return stored.map { NoteItem(id = it.id, text = it.text, createdAtMs = it.createdAtMs) }
    }

    actual fun add(text: String): List<NoteItem> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return load()

        val current = decode(IosPrefs.getString(KEY)).toMutableList()
        val note = StoredNote(
            id = max(1L, nextId(current)),
            text = trimmed,
            createdAtMs = nowMs()
        )
        val next = listOf(note) + current
        IosPrefs.putString(KEY, encode(next))
        return next.map { NoteItem(it.id, it.text, it.createdAtMs) }
    }

    actual fun delete(id: Long): List<NoteItem> {
        val current = decode(IosPrefs.getString(KEY))
        val next = current.filterNot { it.id == id }
        IosPrefs.putString(KEY, encode(next))
        return next.map { NoteItem(it.id, it.text, it.createdAtMs) }
    }
}

