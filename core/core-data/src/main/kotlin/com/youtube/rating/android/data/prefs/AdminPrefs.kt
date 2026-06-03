package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow

object AdminPrefs : BasePrefs() {
    private val KEY_ADMIN_MODE = booleanPreferencesKey("admin_mode")
    private val KEY_DEBUG_UNLOCKED = booleanPreferencesKey("debug_items_unlocked")
    private val KEY_RELEASE_LOGS_ENABLED = booleanPreferencesKey("release_logcat_logs_enabled")

    @Volatile
    private var cachedAdminMode: Boolean? = null
    @Volatile
    private var cachedDebugUnlocked: Boolean? = null
    @Volatile
    private var cachedReleaseLogsEnabled: Boolean? = null

    fun adminModeFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_ADMIN_MODE, false)

    suspend fun getAdminMode(context: Context): Boolean {
        return cachedAdminMode
            ?: readPref(context, KEY_ADMIN_MODE, false).also { cachedAdminMode = it }
    }

    suspend fun setAdminMode(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_ADMIN_MODE] = enabled }
        cachedAdminMode = enabled
    }

    suspend fun getDebugUnlocked(context: Context): Boolean {
        return cachedDebugUnlocked
            ?: readPref(context, KEY_DEBUG_UNLOCKED, false).also { cachedDebugUnlocked = it }
    }

    suspend fun setDebugUnlocked(context: Context, unlocked: Boolean) {
        editPref(context) { it[KEY_DEBUG_UNLOCKED] = unlocked }
        cachedDebugUnlocked = unlocked
    }

    fun releaseLogsEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_RELEASE_LOGS_ENABLED, false)

    suspend fun getReleaseLogsEnabled(context: Context): Boolean {
        return cachedReleaseLogsEnabled
            ?: readPref(context, KEY_RELEASE_LOGS_ENABLED, false).also { cachedReleaseLogsEnabled = it }
    }

    suspend fun setReleaseLogsEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_RELEASE_LOGS_ENABLED] = enabled }
        cachedReleaseLogsEnabled = enabled
    }
}