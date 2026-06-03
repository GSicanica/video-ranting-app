package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import com.youtube.rating.android.storage.FavoriteVideo
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.core.coroutines.makeIOCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Favorites Screen
 */
class FavoritesViewModel(private val favoritesGateway: FavoritesGateway) : ViewModel() {
    
    private val _favorites = MutableStateFlow<List<FavoriteVideo>>(emptyList())
    val favorites: StateFlow<List<FavoriteVideo>> = _favorites.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load all favorites
     * ✅ FIXED: StateFlow updates are thread-safe, no need for withContext(Main)
     */
    fun loadFavorites() {
        _isLoading.value = true
        _error.value = null

        makeIOCall(
            onCallExecuted = { _isLoading.value = false },
            onErrorAction = { t -> _error.value = t.message ?: "Failed to load favorites" },
            ioCall = { favoritesGateway.getFavoritesSuspend() },
            onCalled = { favoritesList -> _favorites.value = favoritesList }
        )
    }
    
    /**
     * Add to favorites
     * ✅ FIXED: StateFlow updates are thread-safe
     */
    fun addFavorite(video: FavoriteVideo, onComplete: (Boolean) -> Unit) {
        makeIOCall(
            onErrorAction = { t ->
                _error.value = "Failed to add favorite: ${t.message}"
                onComplete(false)
            },
            ioCall = { favoritesGateway.addFavoriteSuspend(video) },
            onCalled = { success ->
                onComplete(success)
                if (success) loadFavorites()
            }
        )
    }
    
    /**
     * Remove from favorites
     * ✅ FIXED: StateFlow updates are thread-safe
     */
    fun removeFavorite(videoId: String, onComplete: (Boolean) -> Unit) {
        makeIOCall(
            onErrorAction = { t ->
                _error.value = "Failed to remove favorite: ${t.message}"
                onComplete(false)
            },
            ioCall = { favoritesGateway.removeFavoriteSuspend(videoId) },
            onCalled = { success ->
                onComplete(success)
                if (success) loadFavorites()
            }
        )
    }
    
    /**
     * Check if video is favorite
     */
    fun isFavorite(videoId: String): Boolean {
        return _favorites.value.any { it.videoId == videoId }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
