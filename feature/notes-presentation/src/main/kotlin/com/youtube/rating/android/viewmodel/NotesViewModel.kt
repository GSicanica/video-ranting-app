package com.youtube.rating.android.viewmodel

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import androidx.lifecycle.ViewModel
import com.youtube.rating.android.localization.UiText
import com.youtube.rating.android.storage.Note
import com.youtube.rating.android.storage.NotesManager
import com.youtube.rating.android.localization.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * ViewModel for Notes Screen
 */
class NotesViewModel(
    private val notesManager: NotesManager
) : ViewModel() {
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<UiText?>(null)
    val error: StateFlow<UiText?> = _error.asStateFlow()

    private fun setError(e: Exception, message: String) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        _error.value = UiText.Dynamic(message)
    }
    
    /**
     * Load all notes
     * ✅ FIXED: StateFlow updates are thread-safe, no need for withContext(Main)
     */
    fun loadNotes(context: Context) {
        makeIOCall {
            _isLoading.value = true
            _error.value = null
            
            try {
                _notes.value = notesManager.getAllNotesAsync()
            } catch (e: Exception) {
                setError(e = e, message = e.message ?: Strings.failedLoadNotes)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Add new note
     * ✅ FIXED: StateFlow updates are thread-safe
     */
    fun addNote(context: Context, title: String, content: String, onComplete: (Boolean) -> Unit) {
        makeIOCall {
            try {
                val note = notesManager.addNoteAsync(title, content)
                onComplete(note != null)
                if (note != null) {
                    loadNotes(context = context)
                }
            } catch (e: Exception) {
                setError(e = e, message = Strings.failedAddNote(e.message))
                onComplete(false)
            }
        }
    }
    
    /**
     * Update existing note
     * ✅ FIXED: StateFlow updates are thread-safe
     */
    fun updateNote(context: Context, id: String, title: String, content: String, onComplete: (Boolean) -> Unit) {
        makeIOCall {
            try {
                notesManager.updateNoteAsync(id, title, content)
                onComplete(true)
                loadNotes(context = context)
            } catch (e: Exception) {
                setError(e = e, message = Strings.failedUpdateNote(e.message))
                onComplete(false)
            }
        }
    }
    
    /**
     * Delete note
     * ✅ FIXED: StateFlow updates are thread-safe
     */
    fun deleteNote(context: Context, id: String, onComplete: (Boolean) -> Unit) {
        makeIOCall {
            try {
                notesManager.deleteNoteAsync(id)
                onComplete(true)
                loadNotes(context = context)
            } catch (e: Exception) {
                setError(e = e, message = Strings.failedDeleteNote(e.message))
                onComplete(false)
            }
        }
    }
    
    /**
     * Get note by ID
     */
    suspend fun getNoteById(context: Context, id: String): Note? {
        return withContext(ioDispatcher) {
            try {
                notesManager.getNoteByIdAsync(id)
            } catch (e: Exception) {
                setError(e = e, message = Strings.failedGetNote(e.message))
                null
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
