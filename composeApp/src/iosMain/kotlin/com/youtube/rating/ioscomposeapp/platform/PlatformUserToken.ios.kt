package com.youtube.rating.ioscomposeapp.platform

import com.youtube.rating.ioscomposeapp.storage.UserTokenStore
import com.youtube.rating.shared.api.RatingApiClient

internal actual object PlatformUserToken {
    actual suspend fun getOrCreate(api: RatingApiClient): String = UserTokenStore.getOrCreateToken(api)
    actual fun getCachedOrNull(): String? = UserTokenStore.getCachedToken()
}

