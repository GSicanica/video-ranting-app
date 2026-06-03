package com.youtube.rating.shared

/**
 * Centralized configuration constants for the app.
 * Keep BASE_URL in one place so all platforms and modules use the same value.
 *
 * 🔧 DEVELOPMENT MODE:
 * - Debug builds automatically use local server (configured in androidApp/build.gradle.kts)
 * - Release builds use production server
 * - Change the IP in build.gradle.kts to match your computer's local network IP
 */

// Production server URL (default)
private const val PRODUCTION_URL = "https://tmbv-hms.com/aYOUTUBEocjenivanje5"

private fun normalizeBaseUrl(url: String): String = url.trimEnd('/')

/**
 * Resolve base URL dynamically when possible.
 */
fun resolveBaseUrl(): String = normalizeBaseUrl(url = PlatformConfig.baseUrl()
        ?: PRODUCTION_URL)

fun resolveDataBaseUrl(): String = normalizeBaseUrl(url = PlatformConfig.dataBaseUrl()
        ?: resolveBaseUrl())

/**
 * Base URL for API calls
 * - Android Debug builds: Uses BuildConfig.BASE_URL (local development server)
 * - Android Release builds: Uses production server
 * - Other platforms: Uses production server
 */
val BASE_URL: String
    get() = resolveBaseUrl()

// These always point to production for data fetching
val YOUTUBE_DATA_BASE_URL: String
    get() = resolveDataBaseUrl()

// Crash reports are collected on the same backend
val CRASH_BASE_URL: String
    get() = resolveDataBaseUrl()

// Hetzner streaming server (upload + playback)
// TODO: Move to environment/build config instead of hardcoding the IP.
private const val DEFAULT_STREAM_BASE_URL: String = "http://46.225.108.6"

val STREAM_BASE_URL: String
    get() = PlatformConfig.streamBaseUrl() ?: DEFAULT_STREAM_BASE_URL

val STREAM_PLAYLIST_DEFAULT_URL: String
    get() = "$STREAM_BASE_URL/stream/video.m3u8"

val STREAM_UPLOAD_ENDPOINT: String
    get() = "$STREAM_BASE_URL/api/streams/upload.php"

/**
 * Check if using local development server
 */
val IS_LOCAL_DEV: Boolean = try {
    PlatformConfig.isLocalDev
} catch (_: Exception) {
    false
}
