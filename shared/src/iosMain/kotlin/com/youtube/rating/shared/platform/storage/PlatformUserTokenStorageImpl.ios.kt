package com.youtube.rating.shared.platform.storage

open class PlatformUserTokenStorage {
    private var token: String? = null

    suspend fun saveToken(token: String) {
        this.token = token
    }

    suspend fun getToken(): String? = token

    suspend fun deleteToken() {
        token = null
    }

    suspend fun isTokenValid(): Boolean = !token.isNullOrBlank()
}

class PlatformUserTokenStorageImpl : PlatformUserTokenStorage()
