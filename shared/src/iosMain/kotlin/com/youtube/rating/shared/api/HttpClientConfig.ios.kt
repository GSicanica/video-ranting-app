package com.youtube.rating.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * iOS-specific HttpClient configuration
 * Uses Darwin engine (NSURLSession under the hood) for optimal iOS integration
 * - HTTP/2 automatically handled by NSURLSession
 * - System-level certificate pinning
 * - Better battery efficiency
 */
internal actual fun createPlatformHttpClient(
    jsonConfig: Json,
    enableLogging: Boolean,
    requestTimeoutMillis: Long,
    connectTimeoutMillis: Long,
    socketTimeoutMillis: Long,
    context: Any?
): HttpClient {
    return HttpClient(Darwin) {
        // Configure Darwin engine for iOS optimization
        engine {
            configureRequest {
                // Allow cellular access (important for reliability)
                setAllowsCellularAccess(true)
            }
        }

        install(ContentNegotiation) {
            json(jsonConfig)
        }

        install(HttpCookies)

        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeoutMillis
            this.connectTimeoutMillis = connectTimeoutMillis
            this.socketTimeoutMillis = socketTimeoutMillis
        }

        if (enableLogging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}
