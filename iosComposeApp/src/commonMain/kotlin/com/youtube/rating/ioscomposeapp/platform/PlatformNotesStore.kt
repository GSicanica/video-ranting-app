package com.youtube.rating.ioscomposeapp.platform

internal data class NoteItem(
    val id: Long,
    val text: String,
    val createdAtMs: Long
)

internal expect object PlatformNotesStore {
    fun load(): List<NoteItem>
    fun add(text: String): List<NoteItem>
    fun delete(id: Long): List<NoteItem>
}

