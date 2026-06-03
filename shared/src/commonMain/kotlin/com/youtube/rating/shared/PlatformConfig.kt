package com.youtube.rating.shared

/**
 * Platform-specific configuration hooks.
 *
 * Keep commonMain free of reflection / JVM-only APIs so it can compile for iOS.
 */
internal expect object PlatformConfig {
    fun baseUrl(): String?
    fun dataBaseUrl(): String?
    fun streamBaseUrl(): String?
    val isLocalDev: Boolean

    fun setBaseUrls(baseUrl: String?, dataBaseUrl: String? = null)
}
