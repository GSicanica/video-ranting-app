package com.youtube.rating.shared

/**
 * Runtime configuration bridge for platform apps (Swift/Android).
 *
 * iOS can call this from Swift to override base URLs at runtime.
 */
object RuntimeConfig {
    fun setBaseUrls(baseUrl: String?, dataBaseUrl: String? = null) {
        PlatformConfig.setBaseUrls(baseUrl = baseUrl, dataBaseUrl = dataBaseUrl)
    }
}

