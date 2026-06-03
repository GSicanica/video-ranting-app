package com.youtube.rating.android.data

import com.youtube.rating.android.storage.FavoriteItemType
import com.youtube.rating.android.storage.FavoriteVideo
import com.youtube.rating.shared.data.RepositoryFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepositoryImpl(private val factory: RepositoryFactory) : FavoritesRepository {
    private val repo by lazy { factory.createFavoritesRepository() }

    override fun getFavoritesFlow(): Flow<List<FavoriteVideo>> {
        return repo.getAllFavorites().map { list ->
            list.map { model ->
                FavoriteVideo(
                    videoId = model.videoId,
                    title = model.title,
                    thumbnail = model.thumbnail,
                    channelName = model.channelName,
                    avgLove = model.avgLove,
                    avgFaith = model.avgFaith,
                    avgHope = model.avgHope,
                    totalRatings = model.totalRatings,
                    category = model.category,
                    timestamp = model.timestamp,
                    type = try {
                        FavoriteItemType.valueOf(model.type.name)
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        FavoriteItemType.VIDEO
                    }
                )
            }
        }
    }

    override suspend fun addFavorite(video: FavoriteVideo): Boolean {

        // ✅ blokiraj slike
        if (video.type == FavoriteItemType.IMAGE) return false
        // dodatna zaštita: ako je file path / uri
        if (video.videoId.startsWith("/") || video.videoId.startsWith("file://")) return false


        val model = com.youtube.rating.shared.data.FavoriteVideoModel(
            videoId = video.videoId,
            title = video.title,
            thumbnail = video.thumbnail,
            channelName = video.channelName,
            avgLove = video.avgLove,
            avgFaith = video.avgFaith,
            avgHope = video.avgHope,
            totalRatings = video.totalRatings,
            category = video.category,
            timestamp = video.timestamp,
            type = try {
                com.youtube.rating.shared.data.FavoriteItemTypeModels.valueOf(video.type.name)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                com.youtube.rating.shared.data.FavoriteItemTypeModels.VIDEO
            }
        )
        return when (repo.addFavorite(model)) {
            is com.youtube.rating.shared.data.DataResult.Success -> true
            else -> false
        }
    }

    override suspend fun removeFavorite(videoId: String): Boolean {
        return when (repo.removeFavorite(videoId)) {
            is com.youtube.rating.shared.data.DataResult.Success -> true
            else -> false
        }
    }

    override suspend fun updateFavoriteCategory(videoId: String, category: String?): Boolean {
        return when (repo.updateFavoriteCategory(videoId, category)) {
            is com.youtube.rating.shared.data.DataResult.Success -> true
            else -> false
        }
    }

    override suspend fun clearAll(): Boolean {
        return when (repo.clearAll()) {
            is com.youtube.rating.shared.data.DataResult.Success -> true
            else -> false
        }
    }
}
