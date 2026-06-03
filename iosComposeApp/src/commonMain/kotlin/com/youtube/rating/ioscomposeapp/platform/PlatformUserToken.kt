package com.youtube.rating.ioscomposeapp.platform

import com.youtube.rating.shared.api.RatingApiClient

internal expect object PlatformUserToken {
    suspend fun getOrCreate(api: RatingApiClient): String
    fun getCachedOrNull(): String?
}

