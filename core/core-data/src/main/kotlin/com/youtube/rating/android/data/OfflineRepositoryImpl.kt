package com.youtube.rating.android.data

import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.shared.data.RepositoryFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineRepositoryImpl(private val factory: RepositoryFactory) : OfflineRepository {
    private val repo = factory.createOfflineVideosRepository()

    override fun getAllVideosFlow(): Flow<List<OfflineVideo>> {
        return repo.getAllVideos().map { list ->
            list.map { model ->
                OfflineVideo(
                    id = model.id,
                    youtubeId = model.youtubeId,
                    title = model.title,
                    channelName = model.channelName,
                    localPath = model.localPath,
                    thumbnailPath = model.thumbnailPath,
                    thumbnailUrl = model.thumbnailUrl,
                    duration = model.duration,
                    fileSize = model.fileSize,
                    isFromDevice = model.youtubeId == null,
                    category = model.category,
                    addedAt = model.addedAt
                )
            }
        }
    }

    override suspend fun addVideo(video: OfflineVideo): Boolean {
        val model = com.youtube.rating.shared.data.OfflineVideoModel(
            id = video.id,
            youtubeId = video.youtubeId,
            title = video.title,
            channelName = video.channelName,
            localPath = video.localPath,
            thumbnailPath = video.thumbnailPath,
            thumbnailUrl = video.thumbnailUrl,
            duration = video.duration,
            fileSize = video.fileSize,
            category = video.category,
            addedAt = video.addedAt
        )
        return when (repo.addVideo(model)) {
            is com.youtube.rating.shared.data.DataResult.Success -> true
            else -> false
        }
    }

    override suspend fun removeVideo(id: String): Boolean {
        return when (repo.deleteVideo(id)) {
            is com.youtube.rating.shared.data.DataResult.Success -> true
            else -> false
        }
    }
}
