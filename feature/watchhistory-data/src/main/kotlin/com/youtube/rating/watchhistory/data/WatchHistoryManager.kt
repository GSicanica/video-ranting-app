package com.youtube.rating.watchhistory.data

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.util.Log
import com.youtube.rating.watchhistory.domain.WatchHistoryEntry
import com.youtube.rating.watchhistory.domain.WatchHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class WatchHistoryManager private constructor(private val context: Context) : WatchHistoryStore {

    private val historyFile = File(context.filesDir, "watch_history.json")
    private val _historyCache = MutableStateFlow<List<WatchHistoryEntry>>(emptyList())
    override val historyFlow: StateFlow<List<WatchHistoryEntry>> = _historyCache
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
        scope.launch { loadHistorySync() }
    }

    private suspend fun loadHistorySync() {
        saveLock.withLock {
            try {
                if (historyFile.exists()) {
                    val jsonArray = JSONArray(historyFile.readText())
                    val history = mutableListOf<WatchHistoryEntry>()

                    for (i in 0 until jsonArray.length()) {
                        runCatching { jsonArray.getJSONObject(i).toWatchHistoryEntry() }
                            .onSuccess { history.add(it) }
                            .onFailure { Log.e(TAG, "Error parsing history entry at index $i", it) }
                    }

                    _historyCache.value = history.sortedByDescending { it.viewedAt }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading history", e)
            }
        }
    }

    suspend fun addToHistory(entry: WatchHistoryEntry): Boolean = withContext(ioDispatcher) {
        saveLock.withLock {
            try {
                val current = _historyCache.value.toMutableList()
                current.removeAll { it.videoId == entry.videoId }
                current.add(0, entry)
                val limited = current.take(MAX_HISTORY_SIZE)
                writeHistoryFile(history = limited)
                _historyCache.value = limited
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to history", e)
                false
            }
        }
    }

    override suspend fun removeFromHistory(videoId: String): Boolean = withContext(ioDispatcher) {
        saveLock.withLock {
            try {
                val current = _historyCache.value.toMutableList()
                val removed = current.removeAll { it.videoId == videoId }
                if (removed) {
                    writeHistoryFile(history = current)
                    _historyCache.value = current
                }
                removed
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from history", e)
                false
            }
        }
    }

    override suspend fun clearHistory(): Boolean = withContext(ioDispatcher) {
        saveLock.withLock {
            try {
                if (historyFile.exists()) {
                    historyFile.delete()
                }
                _historyCache.value = emptyList()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing history", e)
                false
            }
        }
    }

    fun getHistory(): List<WatchHistoryEntry> = _historyCache.value

    override fun getHistoryCount(): Int = _historyCache.value.size

    fun isInHistory(videoId: String): Boolean =
        _historyCache.value.any { it.videoId == videoId }

    override suspend fun mergeFromServer(entries: List<WatchHistoryEntry>) {
        withContext(ioDispatcher) {
            if (entries.isEmpty()) return@withContext
            saveLock.withLock {
                try {
                    val mergedMap = LinkedHashMap<String, WatchHistoryEntry>()
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
                } catch (e: Exception) {
                    Log.e(TAG, "Error merging history from server", e)
                }
            }
        }
    }

    private fun writeHistoryFile(history: List<WatchHistoryEntry>) {
        val jsonArray = JSONArray()
        history.forEach { jsonArray.put(it.toJson()) }
        historyFile.writeText(jsonArray.toString())
    }

    override suspend fun reloadHistory() = withContext(ioDispatcher) {
        loadHistorySync()
    }
}
