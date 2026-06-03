package com.youtube.rating.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.MemoryStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual fun createPlatformHttpClient(
    jsonConfig: Json,
    enableLogging: Boolean,
    requestTimeoutMillis: Long,
    connectTimeoutMillis: Long,
    socketTimeoutMillis: Long,
    context: Any? = null
): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }

        install(HttpCookies)

        // 🚀 HTTP CACHING: Dramatically improve performance by caching API responses
        // Desktop uses memory storage for simplicity
        install(HttpCache) {
            publicStorage(MemoryStorage())
        }

        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeoutMillis
            this.connectTimeoutMillis = connectTimeoutMillis
            this.socketTimeoutMillis = socketTimeoutMillis
        }

        if (enableLogging) {
            install(Logging) {
                level = LogLevel.INFO
                logger = SIMPLE
            }
        }
    }
}