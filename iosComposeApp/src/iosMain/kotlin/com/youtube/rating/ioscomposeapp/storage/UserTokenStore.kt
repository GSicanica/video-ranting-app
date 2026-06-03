package com.youtube.rating.ioscomposeapp.storage

import com.youtube.rating.shared.api.RatingApiClient

internal object UserTokenStore {
    private const val KEY_USER_TOKEN = "user_token"

    fun getCachedToken(): String? = IosPrefs.getString(KEY_USER_TOKEN)?.trim()?.takeIf { it.isNotBlank() }

    suspend fun getOrCreateToken(api: RatingApiClient): String {
        val cached = getCachedToken()
        if (cached != null) return cached

        val res = api.anonymousRegister()
        val token = res.userToken.trim()
        IosPrefs.putString(KEY_USER_TOKEN, token)
        return token
    }
}

