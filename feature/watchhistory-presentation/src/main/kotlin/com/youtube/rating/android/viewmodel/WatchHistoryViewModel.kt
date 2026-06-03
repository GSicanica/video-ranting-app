package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.watchhistory.domain.WatchHistoryEntry
import com.youtube.rating.watchhistory.domain.WatchHistoryRemoteRepository
import com.youtube.rating.watchhistory.domain.WatchHistoryRemoteResult
import com.youtube.rating.watchhistory.domain.WatchHistoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Watch History Screen
 * Manages watch history data and user interactions
 */
class WatchHistoryViewModel(
    private val manager: WatchHistoryStore,
    private val repository: WatchHistoryRemoteRepository
) : ViewModel() {

    // Directly expose manager's history flow for reactive updates
    val history: StateFlow<List<WatchHistoryEntry>> = manager.historyFlow

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Manager automatically loads history in its init block
        // No need to load here since we're directly exposing manager.historyFlow
    }

    /**
     * Load watch history
     * @param forceRefresh If true, fetch from server and sync to local; otherwise use local cache
     */
    fun loadHistory(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }
            _error.value = null

            try {
                if (forceRefresh) {
                    // Load from server and sync to local manager
                    when (val result = repository.loadHistory()) {
                        is WatchHistoryRemoteResult.Success -> {
                            // Batch-merge to avoid repeated file I/O and keep most recent per videoId
                            manager.mergeFromServer(result.data)
                            android.util.Log.d(
                                "WatchHistoryVM",
                                "Merged ${result.data.size} entries from server"
                            )
                        }
                        is WatchHistoryRemoteResult.Retryable -> {
                            _error.value = "Privremena mrežna greška: ${result.message}"
                        }
                        is WatchHistoryRemoteResult.Error -> {
                            // Local cache is already exposed via manager.historyFlow
                            _error.value = "Greška pri učitavanju sa servera: ${result.message}"
                        }
                    }
                } else {
                    // Local cache is automatically available via manager.historyFlow
                    // Just reload from file to ensure fresh data
                    manager.reloadHistory()
            }
        } catch (e: Exception) {
                _error.value = "Greška: ${e.message}"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Clear all watch history
     */
    fun clearHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Clear local history (manager will automatically update historyFlow)
                val localSuccess = manager.clearHistory()

                if (localSuccess) {
                    // Try to clear on server (don't fail if server call fails)
                    when (val serverResult = repository.clearRemoteHistory()) {
                        is WatchHistoryRemoteResult.Success -> Unit
                        is WatchHistoryRemoteResult.Retryable -> {
                            android.util.Log.w(
                                "WatchHistoryVM",
                                "Retryable while clearing server history: ${serverResult.message}"
                            )
                        }
                        is WatchHistoryRemoteResult.Error -> {
                            android.util.Log.w(
                                "WatchHistoryVM",
                                "Failed to clear server history: ${serverResult.message}"
                            )
                        }
                    }
                    // No need to update _history - manager.historyFlow is automatically updated
                } else {
                    _error.value = "Neuspješno brisanje historije"
                }
            } catch (e: Exception) {
                _error.value = "Greška: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Remove specific video from history
     */
    fun removeVideo(videoId: String) {
        viewModelScope.launch {
            try {
                // Manager will automatically update historyFlow when entry is removed
                manager.removeFromHistory(videoId)
            } catch (e: Exception) {
                _error.value = "Greška pri brisanju: ${e.message}"
            }
        }
    }

    /**
     * Refresh history from server
     */
    fun refresh() {
        loadHistory(forceRefresh = true)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get history count
     */
    fun getHistoryCount(): Int = manager.getHistoryCount()
}
