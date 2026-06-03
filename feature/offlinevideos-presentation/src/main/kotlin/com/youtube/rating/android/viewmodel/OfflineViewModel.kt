// OfflineViewModel.kt (tvoj kod je OK – bez promjena za slike, jer slike su “Galerija” screen/tab)
// Ostavljam 1:1 kako si poslao (copy/paste).
package com.youtube.rating.android.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.youtube.rating.android.storage.DeviceVideo
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.android.storage.OfflineVideoManager
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.youtube.rating.core.coroutines.makeIOCall

private const val TAG = "OfflineViewModel"

class OfflineViewModel(
    private val offlineVideoManager: OfflineVideoManager? = null
) : ViewModel() {

    private val _videos = MutableStateFlow<List<OfflineVideo>>(emptyList())
    val videos: StateFlow<List<OfflineVideo>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deviceVideos = MutableStateFlow<List<DeviceVideo>>(emptyList())
    val deviceVideos: StateFlow<List<DeviceVideo>> = _deviceVideos.asStateFlow()

    private val _isLoadingDeviceVideos = MutableStateFlow(false)
    val isLoadingDeviceVideos: StateFlow<Boolean> = _isLoadingDeviceVideos.asStateFlow()

    private val _totalStorageUsed = MutableStateFlow(0L)
    val totalStorageUsed: StateFlow<Long> = _totalStorageUsed.asStateFlow()

    private fun manager(context: Context): OfflineVideoManager =
        offlineVideoManager ?: OfflineVideoManager(context = context)

    fun loadVideos(context: Context) {
        Logger.debug(TAG, "loadVideos called")
        makeIOCall {
            _isLoading.value = true
            _error.value = null

            try {
                val manager = manager(context)
                val videoList = manager.getOfflineVideosAsync()
                val storage = manager.getTotalStorageUsedAsync()

                Logger.debug(TAG, "loadVideos result - count: ${videoList.size}")
                _videos.value = videoList
                _totalStorageUsed.value = storage
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error(TAG, "loadVideos exception", e)
                _error.value = e.message ?: "Failed to load videos"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun scanDeviceVideos(context: Context) {
        makeIOCall {
            _isLoadingDeviceVideos.value = true
            try {
                val manager = manager(context)
                val videos = manager.scanDeviceVideosAsync()
                _deviceVideos.value = videos
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _error.value = "Failed to scan device videos: ${e.message}"
            } finally {
                _isLoadingDeviceVideos.value = false
            }
        }
    }

    fun addVideoFromDevice(
        context: Context,
        uri: Uri,
        title: String? = null,
        category: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        Logger.debug(TAG, "addVideoFromDevice called - URI: $uri, title: $title")
        makeIOCall {
            try {
                val manager = manager(context)
                val result = manager.addVideoFromDevice(uri, title, category)

                if (result != null) {
                    val currentVideos = _videos.value.toMutableList()
                    currentVideos.add(0, result)
                    _videos.value = currentVideos

                    val storage = manager.getTotalStorageUsedAsync()
                    _totalStorageUsed.value = storage

                    withContext(Dispatchers.Main) { onComplete(true) }
                } else {
                    _error.value = "Failed to add video"
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _error.value = "Failed to add video: ${e.message}"
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun deleteVideo(context: Context, videoId: String, onComplete: (Boolean) -> Unit) {
        makeIOCall {
            try {
                val manager = manager(context)
                val success = manager.removeVideoAsync(videoId)

                if (success) {
                    val currentVideos = _videos.value.toMutableList()
                    currentVideos.removeAll { it.id == videoId }
                    _videos.value = currentVideos

                    val storage = manager.getTotalStorageUsedAsync()
                    _totalStorageUsed.value = storage

                    withContext(Dispatchers.Main) { onComplete(true) }
                } else {
                    _error.value = "Failed to delete video"
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _error.value = "Failed to delete video: ${e.message}"
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun updateVideo(context: Context, videoId: String, newTitle: String?, newCategory: String?, onComplete: (Boolean) -> Unit) {
        makeIOCall {
            try {
                val manager = manager(context)
                val success = manager.updateVideoAsync(videoId, newTitle, newCategory)

                if (success) {
                    val currentVideos = _videos.value.toMutableList()
                    val index = currentVideos.indexOfFirst { it.id == videoId }
                    if (index >= 0) {
                        val updatedVideo = currentVideos[index].copy(
                            title = newTitle ?: currentVideos[index].title,
                            category = newCategory ?: currentVideos[index].category
                        )
                        currentVideos[index] = updatedVideo
                        _videos.value = currentVideos
                    }
                    withContext(Dispatchers.Main) { onComplete(true) }
                } else {
                    _error.value = "Failed to update video"
                    withContext(Dispatchers.Main) { onComplete(false) }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _error.value = "Failed to update video: ${e.message}"
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
