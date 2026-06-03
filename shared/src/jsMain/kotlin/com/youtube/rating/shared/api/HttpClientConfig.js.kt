package com.youtube.rating.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual fun createPlatformHttpClient(
    jsonConfig: Json,
    enableLogging: Boolean,
    requestTimeoutMillis: Long,
    connectTimeoutMillis: Long,
    socketTimeoutMillis: Long,
    context: Any?
): HttpClient {
    return HttpClient(Js) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }

        install(HttpCookies)

        install(HttpTimeout) {
            this.requestTimeoutMillis = requestTimeoutMillis
            this.connectTimeoutMillis = connectTimeoutMillis
            this.socketTimeoutMillis = socketTimeoutMillis
        }

        defaultRequest {
            headers.append(HttpHeaders.Accept, "application/json")
            headers.append(HttpHeaders.AcceptCharset, "UTF-8")
            contentType(ContentType.Application.Json)
        }

        if (enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }
                level = LogLevel.INFO
            }
        }
    }
}
