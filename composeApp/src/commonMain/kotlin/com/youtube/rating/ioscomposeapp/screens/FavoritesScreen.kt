package com.youtube.rating.ioscomposeapp.screens

import androidx.compose.runtime.Composable
import com.youtube.rating.shared.api.RatingApiClient

@Composable
internal fun FavoritesScreen(
    api: RatingApiClient
) {
    RatedScreen(api = api, title = "Favorites")
}
