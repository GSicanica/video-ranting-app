package com.youtube.rating.android.storage

import kotlinx.coroutines.flow.StateFlow

interface FavoritesGateway {
    val favoritesFlow: StateFlow<List<FavoriteVideo>>

    suspend fun getFavoritesSuspend(): List<FavoriteVideo>
    suspend fun addFavoriteSuspend(video: FavoriteVideo): Boolean
    suspend fun removeFavoriteSuspend(videoId: String): Boolean
    suspend fun reorderFavoritesSuspend(newOrder: List<FavoriteVideo>)

    fun addFavorite(video: FavoriteVideo): Boolean
    fun removeFavorite(videoId: String): Boolean
}
