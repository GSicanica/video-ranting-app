package com.youtube.rating.android.sentry

import java.net.URI

object SentryBreadcrumbs {
    fun screen(name: String) {
        SentryLogger.addBreadcrumb("screen", name)
        SentryLogger.updateContext(tags = mapOf("screen" to name))
    }

    fun action(name: String, data: Map<String, String> = emptyMap()) {
        SentryLogger.addBreadcrumb("action", name, data)
        SentryLogger.updateContext(tags = mapOf("last_action" to name))
    }

    fun network(method: String, url: String, status: Int?, durationMs: Long) {
        val data = mutableMapOf(
            "method" to method,
            "url" to sanitizeUrl(rawUrl = url),
            "durationMs" to durationMs.toString()
        )
        status?.let { data["status"] = it.toString() }
        SentryLogger.addBreadcrumb("network", "HTTP $method", data)
    }

    private fun sanitizeUrl(rawUrl: String): String {
        return try {
            val uri = URI(rawUrl)
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            when {
                host.isNotBlank() && path.isNotBlank() -> "$host$path"
                path.isNotBlank() -> path
                else -> rawUrl.substringBefore('?').substringBefore('#')
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            rawUrl.substringBefore('?').substringBefore('#')
        }
    }
}
