package com.youtube.rating.android.utils

import android.content.Intent
import android.net.Uri

/**
 * Utility class for extracting YouTube video IDs from various URL formats
 * and handling deep links.
 */
object DeepLinkUtil {

    private const val APP_CUSTOM_SCHEME = "youtuberating"
    private const val APP_CUSTOM_HOST = "rate"
    private const val APP_SHARE_SCHEME = "https"
    private const val APP_SHARE_HOST = "youtuberating.app"
    private val VIDEO_ID_REGEX = Regex("""[a-zA-Z0-9_-]{11}""")


    /**
     * Check if a URL is a valid YouTube URL.
     */
    fun isYouTubeUrl(url: String): Boolean {
        return extractVideoId(url) != null
    }

    /**
     * Extract video ID from an Intent (useful for handling shared links).
     */
    fun extractVideoIdFromIntent(intent: Intent): String? {
        val raw = intentPayload(intent) ?: return null
        val uri = parseUri(raw)

        extractVideoIdFromAppUri(uri)?.let { return it }
        extractVideoId(raw)?.let { return it }
        return null
    }

    /**
     * Extract optional start seconds from an Intent.
     * Supports both app deep links and YouTube links (start/t parameters).
     */
    fun extractStartSecondsFromIntent(intent: Intent): Int? {
        val raw = intentPayload(intent) ?: return null

        val uri = parseUri(raw) ?: return null
        val startCandidate = uri.getQueryParameter("start")
            ?: uri.getQueryParameter("t")
        return parseSeconds(startCandidate)
    }

    /**
     * Validate if a string is a valid video ID.
     */
    fun isValidVideoId(videoId: String): Boolean {
        return VIDEO_ID_REGEX.matches(videoId)
    }

    /**
     * Get the full YouTube watch URL from a video ID.
     */
    fun getWatchUrl(videoId: String): String {
        return getAppDeepLinkUrl(videoId = videoId)
    }

    /**
     * Get the shareable app URL from a video ID.
     */
    fun getAppShareUrl(videoId: String, startSeconds: Int? = null): String {
        return getAppDeepLinkUrl(videoId = videoId, startSeconds = startSeconds)
    }

    /**
     * Get the internal deep link URI that is handled by the app directly.
     */
    fun getAppDeepLinkUrl(videoId: String, startSeconds: Int? = null): String {
        val normalizedStart = startSeconds?.takeIf { it > 0 }
        val base = "$APP_CUSTOM_SCHEME://$APP_CUSTOM_HOST/$videoId"
        return if (normalizedStart != null) "$base?start=$normalizedStart" else base
    }

    private fun intentPayload(intent: Intent): String? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") intent.getStringExtra(Intent.EXTRA_TEXT) else null
            }
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun parseUri(raw: String): Uri? = runCatching { Uri.parse(raw) }.getOrNull()

    private fun extractVideoIdFromAppUri(uri: Uri?): String? {
        if (uri == null) return null

        val isCustom = uri.scheme.equals(APP_CUSTOM_SCHEME, ignoreCase = true) &&
            uri.host.equals(APP_CUSTOM_HOST, ignoreCase = true)
        val isShare = uri.scheme.equals(APP_SHARE_SCHEME, ignoreCase = true) &&
            uri.host.equals(APP_SHARE_HOST, ignoreCase = true)
        if (!isCustom && !isShare) return null

        val id = uri.pathSegments.lastOrNull()?.trim().orEmpty()
        return id.takeIf { isValidVideoId(it) }
    }

    private fun parseSeconds(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim().lowercase()

        // "123"
        trimmed.toIntOrNull()?.takeIf { it >= 0 }?.let { return it }

        // "123s"
        if (trimmed.endsWith("s")) {
            trimmed.removeSuffix("s").toIntOrNull()?.takeIf { it >= 0 }?.let { return it }
        }

        // "1m30s", "2h10m5s"
        val regex = Regex("""^(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""")
        val match = regex.matchEntire(trimmed) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: 0
        val m = match.groupValues[2].toIntOrNull() ?: 0
        val s = match.groupValues[3].toIntOrNull() ?: 0
        val total = h * 3600 + m * 60 + s
        return total.takeIf { it > 0 }
    }
}
