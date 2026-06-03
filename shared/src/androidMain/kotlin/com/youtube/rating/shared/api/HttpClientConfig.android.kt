package com.youtube.rating.shared.api

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import com.youtube.rating.shared.debug.NetworkDebugStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
internal actual fun createPlatformHttpClient(
    jsonConfig: Json,
    enableLogging: Boolean,
    requestTimeoutMillis: Long,
    connectTimeoutMillis: Long,
    socketTimeoutMillis: Long,
    context: Any?
): HttpClient {
    val tag = "RatingApiHttp"

    // Preimenujemo parametre da izbjegnemo "val cannot be reassigned"
    val reqTimeoutMs = requestTimeoutMillis
    val connTimeoutMs = connectTimeoutMillis
    val sockTimeoutMs = socketTimeoutMillis

    val resilientJson = Json(jsonConfig) {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
        allowTrailingComma = true
        allowComments = true
    }

    return HttpClient(Android) {
        engine {
            connectTimeout = connTimeoutMs.toInt()
            socketTimeout = sockTimeoutMs.toInt()
            // threadsCount is deprecated; IO dispatcher is used by default
            pipelining = false
        }

        install(HttpTimeout) {
            // ✅ Ovdje sad nema konflikta imena
            this.requestTimeoutMillis = reqTimeoutMs
            this.connectTimeoutMillis = connTimeoutMs
            this.socketTimeoutMillis = sockTimeoutMs
        }

        install(ContentNegotiation) {
            json(resilientJson)
        }

        // 🚀 HTTP CACHING: Dramatically improve performance by caching API responses
        // Reduces response time and minimizes network requests (persists across app restarts)
        install(HttpCache) {
            val cacheRoot = AndroidCacheDirProvider.cacheDir
            if (cacheRoot != null) {
                publicStorage(FileStorage(File(cacheRoot, "ktor_http_cache")))
            }
        }

        // ✅ KLJUČ: PHPSESSID cookie se automatski sprema i šalje na sljedeće requestove
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }

        defaultRequest {
            val requestId = UUID.randomUUID().toString()
            headers.append(HttpHeaders.Accept, "application/json")
            headers.append(HttpHeaders.AcceptCharset, "UTF-8")
            headers.append("X-Request-Id", requestId)
            contentType(ContentType.Application.Json)
            NetworkDebugStore.add("KTOR REQUEST_ID $requestId")
        }

        if (enableLogging) {
            install(Logging) {
                // Avoid logging full bodies/headers. Even debug logs can accidentally include
                // cookies/tokens (PHPSESSID, CSRF, user_token, Authorization, etc.).
                level = LogLevel.INFO
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        val sanitized = redactSensitive(message = message)
                        Log.d(tag, sanitized)
                        NetworkDebugStore.add("KTOR $sanitized")
                    }
                }
            }

            install(ResponseObserver) {
                onResponse { response ->
                    val rid = response.request.headers["X-Request-Id"] ?: "n/a"
                    Log.d(tag, "RESPONSE[$rid]: ${response.status} FROM: ${response.request.url}")
                    NetworkDebugStore.add("KTOR RESPONSE[$rid] ${response.status} ${response.request.method.value} ${response.request.url}")
                }
            }
        }
    }
}

private fun redactSensitive(message: String): String {
    // Best-effort redaction for common secrets that may appear in logs.
    // Keep conservative to avoid destroying log usefulness.
    val patterns = listOf(
        // Authorization: Bearer ...
        Regex("(?i)(Authorization\\s*:\\s*)(Bearer\\s+[^\\s]+)"),
        // Cookies can contain PHPSESSID etc.
        Regex("(?i)(Cookie\\s*:\\s*)(.*)"),
        Regex("(?i)(Set-Cookie\\s*:\\s*)(.*)"),
        // CSRF header value
        Regex("(?i)(X-CSRF-Token\\s*:\\s*)([^\\s]+)"),
        // Query/body common token param names
        Regex("(?i)(user_token=)([^&\\s]+)"),
        Regex("(?i)(userToken=)([^&\\s]+)")
    )

    var out = message
    for (p in patterns) {
        out = out.replace(p) { mr ->
            val prefix = mr.groupValues.getOrNull(1) ?: ""
            prefix + "[REDACTED]"
        }
    }
    return out
}
