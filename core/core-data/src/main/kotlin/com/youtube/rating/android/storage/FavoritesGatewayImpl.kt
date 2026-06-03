package com.youtube.rating.android.storage

import kotlinx.coroutines.flow.StateFlow

class FavoritesGatewayImpl(
    private val manager: FavoritesManager
) : FavoritesGateway {
    override val favoritesFlow: StateFlow<List<FavoriteVideo>>
        get() = manager.favoritesFlow

    override suspend fun getFavoritesSuspend(): List<FavoriteVideo> = manager.getFavoritesSuspend()

    override suspend fun addFavoriteSuspend(video: FavoriteVideo): Boolean =
        manager.addFavoriteSuspend(video)

    override suspend fun removeFavoriteSuspend(videoId: String): Boolean =
        manager.removeFavoriteSuspend(videoId)

    override suspend fun reorderFavoritesSuspend(newOrder: List<FavoriteVideo>) =
        manager.reorderFavoritesSuspend(newOrder)

    override fun addFavorite(video: FavoriteVideo): Boolean = manager.addFavorite(video)

    override fun removeFavorite(videoId: String): Boolean = manager.removeFavorite(videoId)
}
