package com.youtube.rating.android.ui.screens

import androidx.compose.runtime.Composable
import com.youtube.rating.android.data.OfflineRepository
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.shared.api.RatingApiClient
import org.koin.compose.koinInject

internal data class FavoritesScreenDependencies(
    val favoritesGateway: FavoritesGateway,
    val offlineRepository: OfflineRepository,
    val apiClient: RatingApiClient
)

@Composable
internal fun rememberFavoritesScreenDependencies(): FavoritesScreenDependencies =
    FavoritesScreenDependencies(
        favoritesGateway = koinInject(),
        offlineRepository = koinInject(),
        apiClient = koinInject()
    )
