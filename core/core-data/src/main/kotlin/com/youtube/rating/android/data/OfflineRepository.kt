package com.youtube.rating.android.data

import com.youtube.rating.android.storage.OfflineVideo
import kotlinx.coroutines.flow.Flow

interface OfflineRepository {
    fun getAllVideosFlow(): Flow<List<OfflineVideo>>
    suspend fun addVideo(video: OfflineVideo): Boolean
    suspend fun removeVideo(id: String): Boolean
}
