package com.youtube.rating.shared

internal actual object PlatformConfig {
    private fun readStaticString(className: String, methodOrField: String): String? {
        return runCatching {
            val cls = Class.forName(className)
            val method = cls.methods.firstOrNull { it.name == methodOrField && it.parameterTypes.isEmpty() }
            when {
                method != null -> method.invoke(null) as? String
                else -> cls.getField(methodOrField).get(null) as? String
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun readStaticBoolean(className: String, field: String): Boolean? {
        return runCatching {
            Class.forName(className).getField(field).get(null) as? Boolean
        }.getOrNull()
    }

    actual fun baseUrl(): String? =
        readStaticString(className = "com.youtube.rating.android.network.BaseUrlProvider", methodOrField = "getBaseUrl")
            ?: readStaticString(className = "com.youtube.rating.android.BuildConfig", methodOrField = "BASE_URL")

    actual fun dataBaseUrl(): String? =
        readStaticString(className = "com.youtube.rating.android.network.BaseUrlProvider", methodOrField = "getDataBaseUrl")
            ?: baseUrl()

    actual fun streamBaseUrl(): String? =
        readStaticString(className = "com.youtube.rating.android.network.BaseUrlProvider", methodOrField = "getStreamBaseUrl")

    actual val isLocalDev: Boolean
        get() = readStaticBoolean(className = "com.youtube.rating.android.BuildConfig", field = "USE_LOCAL_SERVER") ?: false

    actual fun setBaseUrls(baseUrl: String?, dataBaseUrl: String?) {
        // Android owns dynamic base URL via BaseUrlProvider; shared shouldn't override it.
    }
}
