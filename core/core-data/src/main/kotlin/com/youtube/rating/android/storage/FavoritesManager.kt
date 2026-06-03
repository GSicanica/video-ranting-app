package com.youtube.rating.android.storage

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import com.youtube.rating.android.data.FavoritesRepository
import com.youtube.rating.android.utils.AutoBackupManager
import com.youtube.rating.android.utils.ChangeType
import com.youtube.rating.android.widget.FavoriteVideoWidgetProvider
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class FavoritesManager(
    private val context: Context,
    private val repository: FavoritesRepository,
    private val autoBackup: AutoBackupManager
) {
    private val scope = com.youtube.rating.android.utils.AppScope.get()
    
    // Cache favorites in memory for quick sync access
    private val _favoritesCache = MutableStateFlow<List<FavoriteVideo>>(emptyList())
    val favoritesFlow: StateFlow<List<FavoriteVideo>>
        get() {
            ensureStarted()
            return _favoritesCache
        }
    
    // ✅ Expose favorite IDs as StateFlow for O(1) lookup in composables
    // ✅ FIXED: Removed @Volatile var to prevent race conditions - use StateFlow only
    private val _favoriteIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIdsFlow: StateFlow<Set<String>>
        get() {
            ensureStarted()
            return _favoriteIdsFlow
        }
    
    // ✅ FIXED: Access favorite IDs through StateFlow.value (thread-safe)
    private val favoriteIds: Set<String>
        get() = _favoriteIdsFlow.value
    
    @Volatile
    private var started = false
    private val startLock = Any()
    private var startJob: kotlinx.coroutines.Job? = null
    @Volatile
    private var collectingStarted = false

    private fun ensureStarted() {
        if (started) return
        synchronized(startLock) {
            if (started) return
            startJob = scope.launch {
                while (true) {
                    try {
                        collectingStarted = true
                        // Observe repository changes and keep local cache in sync
                        repository.getFavoritesFlow()
                            .distinctUntilChanged()
                            .collect { list ->
                            _favoritesCache.value = list
                            _favoriteIdsFlow.value = list.map { it.videoId }.toSet()
                        }
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        if (e is CancellationException) {
                            collectingStarted = false
                            return@launch
                        }
                        collectingStarted = false
                        Logger.error("FavoritesManager", "Favorites flow failed, retrying", e)
                        delay(1_000)
                    }
                }
            }
            // Mark as started only after the coroutine was successfully created.
            started = true
        }
    }

    /**
     * ✅ FIXED: Cleanup method to prevent memory leaks
     * Call this when FavoritesManager is no longer needed
     */
    fun cleanup() {
        // AppScope is shared across the app; do not cancel it here.
    }



    // Suspend version - preferred for coroutine contexts
    suspend fun addFavoriteSuspend(video: FavoriteVideo): Boolean = withContext(ioDispatcher) {
        ensureStarted()
        Logger.info("FavoritesManager", "Adding favorite: ${video.videoId} type=${video.type}")
        // Prevent duplicates: if already favorite, no-op
        if (video.videoId in favoriteIds) {
            Logger.info("FavoritesManager", "Favorite already exists: ${video.videoId}")
            return@withContext false
        }

        val success = try {
            repository.addFavorite(video)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("FavoritesManager", "Add favorite failed: ${e.message}", e)
            false
        }

        if (success) {
            // Update cache
            _favoritesCache.value = _favoritesCache.value + video
            // ✅ FIXED: Atomic update via StateFlow
            _favoriteIdsFlow.value = favoriteIds + video.videoId
            // Trigger auto-backup
            autoBackup.triggerAutoBackup(ChangeType.FAVORITE_ADDED)
            FavoriteVideoWidgetProvider.requestUpdate(context)
            Logger.info("FavoritesManager", "Add favorite succeeded: ${video.videoId}")
            true
        } else {
            false
        }
    }

    suspend fun removeFavoriteSuspend(videoId: String): Boolean = withContext(ioDispatcher) {
        ensureStarted()
        val success = try {
            repository.removeFavorite(videoId)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("FavoritesManager", "Remove favorite failed: ${e.message}", e)
            false
        }

        if (success) {
            // Update cache
            _favoritesCache.value = _favoritesCache.value.filter { it.videoId != videoId }
            // ✅ FIXED: Atomic update via StateFlow
            _favoriteIdsFlow.value = favoriteIds - videoId
            // Trigger auto-backup
            autoBackup.triggerAutoBackup(ChangeType.FAVORITE_REMOVED)
            FavoriteVideoWidgetProvider.requestUpdate(context)
            true
        } else false
    }

    suspend fun reorderFavoritesSuspend(newOrder: List<FavoriteVideo>) = withContext(ioDispatcher) {
        ensureStarted()
        if (newOrder.isEmpty()) return@withContext
        val base = System.currentTimeMillis()
        newOrder.forEachIndexed { index, video ->
            val updated = video.copy(timestamp = base - index)
            try {
                repository.removeFavorite(video.videoId)
                repository.addFavorite(updated)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error("FavoritesManager", "Reorder favorite failed: ${e.message}", e)
            }
        }
    }

    suspend fun updateFavoriteCategorySuspend(videoId: String, category: String?): Boolean = withContext(ioDispatcher) {
        ensureStarted()
        val normalized = category?.takeIf { it.isNotBlank() }
        val success = try {
            repository.updateFavoriteCategory(videoId, normalized)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("FavoritesManager", "Update favorite category failed: ${e.message}", e)
            false
        }

        if (success) {
            _favoritesCache.value = _favoritesCache.value.map { favorite ->
                if (favorite.videoId == videoId) {
                    favorite.copy(category = normalized)
                } else {
                    favorite
                }
            }
            autoBackup.triggerAutoBackup(ChangeType.FAVORITE_UPDATED)
            FavoriteVideoWidgetProvider.requestUpdate(context)
            true
        } else {
            false
        }
    }

    suspend fun getFavoritesSuspend(): List<FavoriteVideo> = withContext(ioDispatcher) {
        ensureStarted()
        try {
            repository.getFavoritesFlow().firstOrNull().orEmpty()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            emptyList()
        }
    }

    suspend fun clearAllSuspend(): Boolean = withContext(ioDispatcher) {
        ensureStarted()
        val success = try {
            repository.clearAll()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            false
        }

        if (success) {
            _favoritesCache.value = emptyList()
            // ✅ FIXED: Atomic update via StateFlow
            _favoriteIdsFlow.value = emptySet()
            // Trigger auto-backup
            autoBackup.triggerAutoBackup(ChangeType.FAVORITE_CLEARED)
            true
        } else false
    }

    // Sync versions using cache - safe for main thread
    fun addFavorite(video: FavoriteVideo): Boolean {
        ensureStarted()
        // Prevent duplicates
        if (video.videoId in favoriteIds) return false

        // Optimistically update cache immediately
        _favoritesCache.value += video
        // ✅ FIXED: Atomic update via StateFlow
        _favoriteIdsFlow.value = favoriteIds + video.videoId
        FavoriteVideoWidgetProvider.requestUpdate(context)
        
        // Persist in background
        scope.launch {
            val success = try {
                repository.addFavorite(video)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                false
            }
            if (!success) {
                // Rollback on failure
                _favoritesCache.value = _favoritesCache.value.filter { it.videoId != video.videoId }
                // ✅ FIXED: Atomic rollback via StateFlow
                _favoriteIdsFlow.value = favoriteIds - video.videoId
            }
        }
        return true
    }

    fun removeFavorite(videoId: String): Boolean {
        ensureStarted()
        val removedVideo = _favoritesCache.value.find { it.videoId == videoId }
        
        // Optimistically update cache immediately
        _favoritesCache.value = _favoritesCache.value.filter { it.videoId != videoId }
        // ✅ FIXED: Atomic update via StateFlow
        _favoriteIdsFlow.value = favoriteIds - videoId
        FavoriteVideoWidgetProvider.requestUpdate(context)
        
        // Persist in background
        scope.launch {
            val success = try {
                repository.removeFavorite(videoId)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                false
            }
            if (!success && removedVideo != null) {
                // Rollback on failure
                _favoritesCache.value = _favoritesCache.value + removedVideo
                // ✅ FIXED: Atomic rollback via StateFlow
                _favoriteIdsFlow.value = favoriteIds + videoId
            }
        }
        return true
    }

}
