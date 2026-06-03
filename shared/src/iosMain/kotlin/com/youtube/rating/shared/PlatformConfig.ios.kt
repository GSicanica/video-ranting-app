package com.youtube.rating.shared

internal actual object PlatformConfig {
    private var baseUrlOverride: String? = null

    private var dataBaseUrlOverride: String? = null

    actual fun setBaseUrls(baseUrl: String?, dataBaseUrl: String?) {
        baseUrlOverride = baseUrl?.trim()?.takeIf { it.isNotEmpty() }
        dataBaseUrlOverride = dataBaseUrl?.trim()?.takeIf { it.isNotEmpty() }
    }

    actual fun baseUrl(): String? = baseUrlOverride

    actual fun dataBaseUrl(): String? = dataBaseUrlOverride ?: baseUrlOverride

    actual fun streamBaseUrl(): String? = null   // iOS uses default

    actual val isLocalDev: Boolean
        get() = false
}
