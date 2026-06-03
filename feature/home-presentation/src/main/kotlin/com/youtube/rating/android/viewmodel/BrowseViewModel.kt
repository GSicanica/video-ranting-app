package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.VideoSearchResult
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.RequestDeduplicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Unified Browse Screen
 * Manages browsing videos with pagination
 */
class BrowseViewModel(
    private val apiClient: RatingApiClient
) : ViewModel() {

    private val requestDeduplicator = RequestDeduplicator()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sort By
    private val _sortBy = MutableStateFlow("popular")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    // Videos
    private val _videos = MutableStateFlow<List<VideoSearchResult>>(emptyList())
    val videos: StateFlow<List<VideoSearchResult>> = _videos.asStateFlow()

    // Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Error State
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Pagination
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val pageSize = 20

    // Update functions
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Load videos with current filters
     * ✅ FIX: StateFlow updates are thread-safe, no need for Main dispatcher
     */
    fun loadVideos(languageCodes: List<String>, resetPage: Boolean = true) {
        viewModelScope.launch {
            val requestKey = "browse:${_searchQuery.value}:${_sortBy.value}:${languageCodes.joinToString(",")}:${if (resetPage) 1 else _currentPage.value + 1}"
            
            // Spriječi simultane duplikate
            if (!requestDeduplicator.shouldExecute(requestKey)) return@launch

            if (resetPage) {
                _isLoading.value = true
                _error.value = null
                _currentPage.value = 1
                _videos.value = emptyList()
                _hasMore.value = true
            } else {
                _isLoadingMore.value = true
            }

            try {
                val page = if (resetPage) 1 else _currentPage.value + 1
                val response = apiClient.searchVideosV2(
                    searchQuery = _searchQuery.value.ifBlank { null },
                    minLove = 0,
                    minFaith = 0,
                    minHope = 0,
                    sortBy = _sortBy.value,
                    languages = languageCodes,
                    page = page,
                    perPage = pageSize,
                    forceRefresh = false
                )

                if (resetPage) {
                    _videos.value = response.videos
                    _currentPage.value = 1
                } else {
                    _videos.value = _videos.value + response.videos
                    _currentPage.value = page
                }
                _hasMore.value = response.hasMore
                LogConfig.log("📚 Browse loaded: ${response.videos.size} videos")
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _error.value = "Greška: ${e.message}"
                LogConfig.logError("❌ Browse error", e)
            } finally {
                _isLoading.value = false
                _isLoadingMore.value = false
                requestDeduplicator.markComplete(requestKey)
            }
        }
    }

    /**
     * Load more videos (pagination)
     */
    fun loadMore(languageCodes: List<String>) {
        if (_isLoadingMore.value || !_hasMore.value) return
        loadVideos(languageCodes, resetPage = false)
    }

    /**
     * Refresh videos
     */
    fun refresh(languageCodes: List<String>) {
        loadVideos(languageCodes, resetPage = true)
    }
}

/**
 * Factory for creating BrowseViewModel
 */
class BrowseViewModelFactory(
    private val apiClient: RatingApiClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowseViewModel::class.java)) {
            return BrowseViewModel(apiClient = apiClient) as T
        }
        val e = IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e,
            tags = mapOf("where" to "BrowseViewModelFactory.create")
        )
        error(e.message ?: "Unknown ViewModel class")
    }
}
