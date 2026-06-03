package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object VideoPrefs : BasePrefs() {
    private val KEY_WATCH_HISTORY_ENABLED = booleanPreferencesKey("watch_history_enabled")
    private val KEY_RESUME_PLAYBACK_ENABLED = booleanPreferencesKey("resume_playback_enabled")
    private val KEY_LAST_PLAYBACK_VIDEO_ID = stringPreferencesKey("last_playback_video_id")
    private val KEY_LAST_PLAYBACK_SECOND = intPreferencesKey("last_playback_second")
    private val KEY_VIDEO_CLOSE_ACTION = stringPreferencesKey("video_close_action")
    private val KEY_BROWSE_CACHE_JSON = stringPreferencesKey("browse_cache_json")
    private val KEY_NEW_VIDEO_NOTIFICATIONS_ENABLED = booleanPreferencesKey("new_video_notifications_enabled")
    private val KEY_NEW_VIDEO_LAST_SEEN_ID = stringPreferencesKey("new_video_last_seen_id")

    // --- Watch History ---
    fun watchHistoryEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_WATCH_HISTORY_ENABLED, true)

    suspend fun getWatchHistoryEnabled(context: Context): Boolean =
        readPref(context, KEY_WATCH_HISTORY_ENABLED, true)

    suspend fun setWatchHistoryEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_WATCH_HISTORY_ENABLED] = enabled }
    }

    // --- Resume Playback ---
    fun resumePlaybackEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_RESUME_PLAYBACK_ENABLED, false)

    suspend fun getResumePlaybackEnabled(context: Context): Boolean =
        readPref(context, KEY_RESUME_PLAYBACK_ENABLED, false)

    suspend fun setResumePlaybackEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_RESUME_PLAYBACK_ENABLED] = enabled }
    }

    suspend fun saveLastPlaybackPosition(context: Context, videoId: String, second: Int) {
        editPref(context) {
            it[KEY_LAST_PLAYBACK_VIDEO_ID] = videoId
            it[KEY_LAST_PLAYBACK_SECOND] = second
        }
    }

    suspend fun getLastPlaybackPosition(context: Context): Pair<String?, Int> {
        val prefs = dataStoreFlow(context = context).map { it }.first()
        return prefs[KEY_LAST_PLAYBACK_VIDEO_ID] to (prefs[KEY_LAST_PLAYBACK_SECOND] ?: 0)
    }

    // --- Video close action ---
    fun videoCloseActionFlow(context: Context): Flow<String?> =
        dataStoreFlow(context = context).map { it[KEY_VIDEO_CLOSE_ACTION] }

    suspend fun getVideoCloseAction(context: Context): String? =
        readPrefNullable(context, KEY_VIDEO_CLOSE_ACTION)

    suspend fun setVideoCloseAction(context: Context, action: String?) {
        editPref(context) { prefs ->
            if (action == null) prefs.remove(KEY_VIDEO_CLOSE_ACTION) else prefs[KEY_VIDEO_CLOSE_ACTION] = action
        }
    }

    // --- Browse cache ---
    suspend fun getBrowseCacheJson(context: Context): String? =
        readPrefNullable(context, KEY_BROWSE_CACHE_JSON)

    suspend fun setBrowseCacheJson(context: Context, json: String?) {
        editPref(context) { prefs ->
            if (json.isNullOrBlank()) prefs.remove(KEY_BROWSE_CACHE_JSON)
            else prefs[KEY_BROWSE_CACHE_JSON] = json
        }
    }

    // --- New video notifications ---
    fun newVideoNotificationsEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_NEW_VIDEO_NOTIFICATIONS_ENABLED, false)

    suspend fun getNewVideoNotificationsEnabled(context: Context): Boolean =
        readPref(context, KEY_NEW_VIDEO_NOTIFICATIONS_ENABLED, false)

    suspend fun setNewVideoNotificationsEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_NEW_VIDEO_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun getNewVideoLastSeenId(context: Context): String? =
        readPrefNullable(context, KEY_NEW_VIDEO_LAST_SEEN_ID)

    suspend fun setNewVideoLastSeenId(context: Context, videoId: String?) {
        editPref(context) { prefs ->
            if (videoId.isNullOrBlank()) prefs.remove(KEY_NEW_VIDEO_LAST_SEEN_ID)
            else prefs[KEY_NEW_VIDEO_LAST_SEEN_ID] = videoId
        }
    }
}