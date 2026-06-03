package com.youtube.rating.android.core

object LegacyBuildConfig {
    private const val GENERATED_BUILD_CONFIG = "com.youtube.rating.android.BuildConfig"

    val DEBUG: Boolean
        get() = readBoolean(name = "DEBUG", fallback = false)

    val VERSION_NAME: String
        get() = readString(name = "VERSION_NAME", fallback = "unknown")

    val VERSION_CODE: Int
        get() = readInt(name = "VERSION_CODE", fallback = 0)

    val APPLICATION_ID: String
        get() = readString(name = "APPLICATION_ID", fallback = "com.youtube.rating.android")

    val BASE_URL: String
        get() = readString(name = "BASE_URL", fallback = "")

    val SENTRY_DSN: String
        get() = readString(name = "SENTRY_DSN", fallback = "")

    val DEBUG_UNLOCK_PASSWORD: String
        get() = readString(name = "DEBUG_UNLOCK_PASSWORD", fallback = "")

    val ENABLE_LOGGING: Boolean
        get() = readBoolean(name = "ENABLE_LOGGING", fallback = DEBUG)

    val BUILD_TYPE: String
        get() = readString(name = "BUILD_TYPE", fallback = if (DEBUG) "debug" else "release")

    val MIN_SDK: Int
        get() = readInt(name = "MIN_SDK", fallback = 24)

    val TARGET_SDK: Int
        get() = readInt(name = "TARGET_SDK", fallback = 35)

    val COMPILE_SDK: Int
        get() = readInt(name = "COMPILE_SDK", fallback = 36)

    val USE_LOCAL_SERVER: Boolean
        get() = readBoolean(name = "USE_LOCAL_SERVER", fallback = false)

    private fun readString(name: String, fallback: String): String =
        readField(name = name) as? String ?: fallback

    private fun readInt(name: String, fallback: Int): Int =
        when (val value = readField(name = name)) {
            is Int -> value
            is Number -> value.toInt()
            else -> fallback
        }

    private fun readBoolean(name: String, fallback: Boolean): Boolean =
        readField(name = name) as? Boolean ?: fallback

    private fun readField(name: String): Any? =
        runCatching {
            Class.forName(GENERATED_BUILD_CONFIG).getField(name).get(null)
        }.getOrNull()
}
