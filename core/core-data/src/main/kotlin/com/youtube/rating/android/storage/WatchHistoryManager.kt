package com.youtube.rating.android.storage

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File

/**
 * Manager for local watch history storage
 * Handles reading/writing watch history to local JSON file
 */
class WatchHistoryManager private constructor(private val context: Context) {

    private val historyFile = File(context.filesDir, "watch_history.json")
    private val _historyCache = MutableStateFlow<List<WatchHistoryEntry>>(emptyList())
    val historyFlow: StateFlow<List<WatchHistoryEntry>> = _historyCache
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val saveLock = Mutex()

    companion object {
        private const val TAG = "WatchHistoryManager"
        private const val MAX_HISTORY_SIZE = 500

        @Volatile
        private var instance: WatchHistoryManager? = null

        fun getInstance(context: Context): WatchHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: WatchHistoryManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    init {
        // Load history off the main thread to avoid startup jank/ANR.
        scope.launch { loadHistorySync() }
    }

    /**
     * Load history from file synchronously (called from init)
     */
    private suspend fun loadHistorySync() {
        saveLock.withLock {
            try {
                if (historyFile.exists()) {
                    val json = historyFile.readText()
                    val jsonArray = JSONArray(json)
                    val history = mutableListOf<WatchHistoryEntry>()

                    for (i in 0 until jsonArray.length()) {
                        try {
                            history.add(WatchHistoryEntry.fromJson(jsonArray.getJSONObject(i)))
                        } catch (e: Exception) {
                            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                            Log.e(TAG, "Error parsing history entry at index $i", e)
                        }
                    }

                    _historyCache.value = history.sortedByDescending { it.viewedAt }
                } else {
                    Unit
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Log.e(TAG, "Error loading history", e)
            }
        }
    }

    /**
     * Add video to watch history
     * Removes duplicate entries (same video) and adds new entry at top
     *
     * @param entry Watch history entry to add
     * @return Success status
     */
    suspend fun addToHistory(entry: WatchHistoryEntry): Boolean = withContext(ioDispatcher) {
        saveLock.withLock {
            try {
                val current = _historyCache.value.toMutableList()

                // Remove duplicate (same video)
                current.removeAll { it.videoId == entry.videoId }

                // Add new entry at beginning
                current.add(0, entry)

                // Limit to MAX_HISTORY_SIZE entries
                val limited = current.take(MAX_HISTORY_SIZE)

                // Save to file
                writeHistoryFile(history = limited)

                // Update cache
                _historyCache.value = limited

                Log.d(TAG, "Added video to history: ${entry.videoId}")
                true
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Log.e(TAG, "Error adding to history", e)
                false
            }
        }
    }

    /**
     * Remove specific entry from history
     *
     * @param videoId Video ID to remove
     * @return Success status
     */
    suspend fun removeFromHistory(videoId: String): Boolean = withContext(ioDispatcher) {
        saveLock.withLock {
            try {
                val current = _historyCache.value.toMutableList()
                val removed = current.removeAll { it.videoId == videoId }

                if (removed) {
                    writeHistoryFile(history = current)
                    _historyCache.value = current
                    Log.d(TAG, "Removed video from history: $videoId")
                }

                removed
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Log.e(TAG, "Error removing from history", e)
                false
            }
        }
    }

    /**
     * Clear all watch history
     *
     * @return Success status
     */
    suspend fun clearHistory(): Boolean = withContext(ioDispatcher) {
        saveLock.withLock {
            try {
                if (historyFile.exists()) {
                    historyFile.delete()
                }
                _historyCache.value = emptyList()
                Log.d(TAG, "Cleared all watch history")
                true
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Log.e(TAG, "Error clearing history", e)
                false
            }
        }
    }

    /**
     * Get current history as list
     *
     * @return List of watch history entries
     */
    fun getHistory(): List<WatchHistoryEntry> = _historyCache.value

    /**
     * Get history count
     *
     * @return Number of entries in history
     */
    fun getHistoryCount(): Int = _historyCache.value.size

    /**
     * Check if video is in history
     *
     * @param videoId Video ID to check
     * @return True if video exists in history
     */
    fun isInHistory(videoId: String): Boolean {
        return _historyCache.value.any { it.videoId == videoId }
    }

    /**
     * Merge server history with local cache in one batch write (faster, avoids repeated I/O).
     * Keeps the most recent entry per videoId.
     */
    suspend fun mergeFromServer(entries: List<WatchHistoryEntry>) = withContext(ioDispatcher) {
        if (entries.isEmpty()) return@withContext
        saveLock.withLock {
            try {
                val mergedMap = LinkedHashMap<String, WatchHistoryEntry>()

                // Prefer the most recent entry for each videoId
                (entries + _historyCache.value).forEach { entry ->
                    val existing = mergedMap[entry.videoId]
                    if (existing == null || entry.viewedAt > existing.viewedAt) {
                        mergedMap[entry.videoId] = entry
                    }
                }

                val merged = mergedMap.values
                    .sortedByDescending { it.viewedAt }
                    .take(MAX_HISTORY_SIZE)

                writeHistoryFile(history = merged)
                _historyCache.value = merged
                Log.d(TAG, "Merged ${entries.size} server entries (total ${merged.size})")
            } catch (e: Exception) {
                Log.e(TAG, "Error merging history from server", e)
                com.youtube.rating.android.sentry.SentryLogger.captureException(
                    e,
                    tags = mapOf("where" to "WatchHistoryManager.mergeFromServer")
                )
            }
        }
    }

    private fun writeHistoryFile(history: List<WatchHistoryEntry>) {
        val jsonArray = JSONArray()
        history.forEach { jsonArray.put(it.toJson()) }
        historyFile.writeText(jsonArray.toString())
        Log.d(TAG, "Saved ${history.size} entries to file")
    }

    /**
     * Reload history from file
     */
    suspend fun reloadHistory() = withContext(ioDispatcher) {
        loadHistorySync()
    }
}
