package com.youtube.rating.android.storage

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import com.youtube.rating.android.utils.BackupTrigger
import com.youtube.rating.android.utils.ChangeType
import com.youtube.rating.shared.data.NotesRepository
import kotlinx.coroutines.flow.first

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val lastModified: Long = timestamp
)

class NotesManager(
    private val context: Context,
    private val repository: NotesRepository,
    private val backupTrigger: BackupTrigger
) {

    suspend fun getAllNotesAsync(): List<Note> = try {
        kotlinx.coroutines.withContext(ioDispatcher) {
            repository.getAllNotes().first().map { model ->
                Note(
                    id = model.id,
                    title = model.title,
                    content = model.content,
                    timestamp = model.timestamp,
                    lastModified = model.lastModified
                )
            }
        }
    } catch (e: Exception) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        emptyList()
    }

    suspend fun addNoteAsync(title: String, content: String): Note? =
        kotlinx.coroutines.withContext(ioDispatcher) {
            when (val res = repository.addNote(title, content)) {
                is com.youtube.rating.shared.data.DataResult.Success -> {
                    val m = res.data
                    // Trigger auto-backup after adding note
                    backupTrigger.trigger(ChangeType.NOTE_ADDED)
                    Note(id = m.id, title = m.title, content = m.content, timestamp = m.timestamp, lastModified = m.lastModified)
                }
                else -> null
            }
        }

    suspend fun updateNoteAsync(id: String, title: String, content: String) =
        kotlinx.coroutines.withContext(ioDispatcher) {
            repository.updateNote(id, title, content)
            // Trigger auto-backup after update
            backupTrigger.trigger(ChangeType.NOTE_UPDATED)
        }

    suspend fun deleteNoteAsync(id: String) =
        kotlinx.coroutines.withContext(ioDispatcher) {
            repository.deleteNote(id)
            // Trigger auto-backup after delete
            backupTrigger.trigger(ChangeType.NOTE_DELETED)
        }

    suspend fun getNoteByIdAsync(id: String): Note? =
        kotlinx.coroutines.withContext(ioDispatcher) {
            repository.getAllNotes().first().firstOrNull { it.id == id }?.let { m ->
                Note(id = m.id, title = m.title, content = m.content, timestamp = m.timestamp, lastModified = m.lastModified)
            }
        }

    suspend fun clearAllAsync() =
        kotlinx.coroutines.withContext(ioDispatcher) {
            repository.clearAll()
        }

    fun getBackupTimestamp(): Long = 0L
}
