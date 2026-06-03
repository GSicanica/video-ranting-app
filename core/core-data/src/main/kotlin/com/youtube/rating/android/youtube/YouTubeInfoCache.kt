package com.youtube.rating.android.youtube

import android.content.Context
import kotlinx.coroutines.launch
import com.youtube.rating.shared.models.YouTubeVideoInfo
import org.json.JSONObject
import com.youtube.rating.android.data.prefs.GenericPrefs

class YouTubeInfoCache(
    context: Context,
    private val ttlMs: Long
) {
    private val appContext = context.applicationContext
    private val scope = com.youtube.rating.android.utils.AppScope.get()
    private val prefs = appContext.getSharedPreferences("yt_info_cache", Context.MODE_PRIVATE)

    fun get(videoId: String): YouTubeVideoInfo? {
        // Prefer SharedPreferences for per-item cache (fast, no DataStore disk IO on main).
        val raw = prefs.getString(keyFor(videoId), null)
        raw ?: return null
        val entry = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val cachedAt = entry.optLong("cachedAt", 0L)
        if (isExpired(cachedAt = cachedAt)) {
            remove(videoId)
            return null
        }
        return YouTubeVideoInfo(
            videoId = entry.optString("videoId"),
            title = entry.optString("title"),
            thumbnail = entry.optString("thumbnail"),
            channelName = entry.optString("channelName"),
            language = entry.optString("language", "unknown")
        )
    }

    fun put(info: YouTubeVideoInfo) {
        val entry = JSONObject().apply {
            put("videoId", info.videoId)
            put("title", info.title)
            put("thumbnail", info.thumbnail)
            put("channelName", info.channelName)
            put("language", info.language)
            put("cachedAt", System.currentTimeMillis())
        }
        prefs.edit().putString(keyFor(info.videoId), entry.toString()).apply()
        // Best-effort mirror to DataStore for backward compatibility (async, non-blocking).
        scope.launch { runCatching { GenericPrefs.setString(appContext, keyFor(info.videoId), entry.toString()) } }
    }

    fun remove(videoId: String) {
        prefs.edit().remove(keyFor(videoId)).apply()
        scope.launch { runCatching { GenericPrefs.setString(appContext, keyFor(videoId), null) } }
    }

    fun invalidate(videoId: String) {
        remove(videoId)
    }

    fun clear() {
        prefs.edit().clear().apply()
        scope.launch { runCatching { GenericPrefs.removeKeysWithPrefix(appContext, "yt_info_") } }
    }

    private fun isExpired(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt > ttlMs
    }

    private fun keyFor(videoId: String) = "yt_info_$videoId"

}