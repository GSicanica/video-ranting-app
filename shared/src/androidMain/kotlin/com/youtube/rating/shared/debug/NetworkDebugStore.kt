package com.youtube.rating.shared.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NetworkDebugStore {
    private const val MAX_ENTRIES = 400
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    }

    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    fun add(message: String) {
        val ts = dateFormat.get().format(Date())
        val line = "[$ts] $message"
        val next = (_entries.value + line).takeLast(MAX_ENTRIES)
        _entries.value = next
    }

    fun clear() {
        _entries.value = emptyList()
    }
}

