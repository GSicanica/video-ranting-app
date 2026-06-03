package com.youtube.rating.android.data

import com.youtube.rating.android.storage.FavoriteVideo
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getFavoritesFlow(): Flow<List<FavoriteVideo>>
    suspend fun addFavorite(video: FavoriteVideo): Boolean
    suspend fun removeFavorite(videoId: String): Boolean
    suspend fun updateFavoriteCategory(videoId: String, category: String?): Boolean
    suspend fun clearAll(): Boolean
}
