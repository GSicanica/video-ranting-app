package com.youtube.rating.android.sentry

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

class SentryNetworkInterceptor(
    private val allowedHosts: Set<String> = emptySet()
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val host = url.host.lowercase()
        val shouldTrack = allowedHosts.isEmpty() || allowedHosts.any { host.endsWith(it) }

        val startNs = System.nanoTime()
        try {
            val response = chain.proceed(request)
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

            if (shouldTrack) {
                SentryBreadcrumbs.network(request.method, url.toString(), response.code, durationMs)
                SentryLogger.metricDistribution("http_response_time_ms", durationMs.toDouble())
                if (response.code >= 500 || response.code == 401 || response.code == 403 || response.code == 429) {
                    SentryLogger.captureMessage(
                        "HTTP ${response.code} ${request.method}",
                        tags = mapOf(
                            "endpoint" to safePath(rawUrl = url.toString()),
                            "http_status" to response.code.toString()
                        )
                    )
                }
            }

            return response
        } catch (e: IOException) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
            if (shouldTrack) {
                SentryBreadcrumbs.network(request.method, url.toString(), null, durationMs)
                SentryLogger.captureException(
                    e,
                    tags = mapOf(
                        "endpoint" to safePath(rawUrl = url.toString()),
                        "io" to "true"
                    )
                )
            }
            // For image loading we don't want telemetry to escalate into crashes.
            // Return a synthetic response so callers can handle it like an HTTP error.
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(599)
                .message(e.message ?: "Network error")
                .body("".toResponseBody("text/plain".toMediaTypeOrNull()))
                .build()
        }
    }

    private fun safePath(rawUrl: String): String {
        return try {
            val uri = URI(rawUrl)
            uri.path ?: rawUrl
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            rawUrl
        }
    }
}
