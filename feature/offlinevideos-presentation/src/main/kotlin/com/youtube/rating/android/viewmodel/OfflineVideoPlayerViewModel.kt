package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import com.youtube.rating.android.data.WatchHistoryRepository
import com.youtube.rating.android.storage.WatchHistoryEntry

class OfflineVideoPlayerViewModel(
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {
    suspend fun recordView(entry: WatchHistoryEntry) {
        watchHistoryRepository.recordView(entry)
    }
}
