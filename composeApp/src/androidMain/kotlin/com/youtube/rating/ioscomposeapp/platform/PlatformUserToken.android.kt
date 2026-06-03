package com.youtube.rating.ioscomposeapp.platform

import com.youtube.rating.shared.api.RatingApiClient

internal actual object PlatformUserToken {
    @Volatile
    private var cached: String? = null

    actual suspend fun getOrCreate(api: RatingApiClient): String {
        cached?.let { return it }
        val token = api.anonymousRegister().userToken
        cached = token
        return token
    }

    actual fun getCachedOrNull(): String? = cached
}

