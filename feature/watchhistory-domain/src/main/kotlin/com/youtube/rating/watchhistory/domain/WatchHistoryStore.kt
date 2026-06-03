package com.youtube.rating.watchhistory.domain

import kotlinx.coroutines.flow.StateFlow

interface WatchHistoryStore {
    val historyFlow: StateFlow<List<WatchHistoryEntry>>

    suspend fun mergeFromServer(entries: List<WatchHistoryEntry>)
    suspend fun reloadHistory()
    suspend fun clearHistory(): Boolean
    suspend fun removeFromHistory(videoId: String): Boolean
    fun getHistoryCount(): Int
}

interface WatchHistoryRemoteRepository {
    suspend fun loadHistory(): WatchHistoryRemoteResult<List<WatchHistoryEntry>>
    suspend fun clearRemoteHistory(): WatchHistoryRemoteResult<Unit>
}

sealed class WatchHistoryRemoteResult<out T> {
    data class Success<T>(val data: T) : WatchHistoryRemoteResult<T>()
    data class Retryable(val message: String) : WatchHistoryRemoteResult<Nothing>()
    data class Error(val message: String) : WatchHistoryRemoteResult<Nothing>()
}
