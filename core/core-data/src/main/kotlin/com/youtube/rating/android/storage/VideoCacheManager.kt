package com.youtube.rating.android.storage

import android.content.Context
import com.youtube.rating.shared.models.VideoSearchResult
import org.json.JSONArray
import org.json.JSONObject
import com.youtube.rating.android.data.prefs.GenericPrefs

data class CachedVideo(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnail: String,
    val category: String?,
    val language: String,
    val avgLove: Double,
    val avgFaith: Double,
    val avgHope: Double,
    val avgTotal: Double,
    val totalRatings: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("videoId", videoId)
            put("title", title)
            put("channelName", channelName)
            put("thumbnail", thumbnail)
            put("category", category ?: "")
            put("language", language)
            put("avgLove", avgLove)
            put("avgFaith", avgFaith)
            put("avgHope", avgHope)
            put("avgTotal", avgTotal)
            put("totalRatings", totalRatings)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CachedVideo {
            return CachedVideo(
                videoId = json.getString("videoId"),
                title = json.getString("title"),
                channelName = json.getString("channelName"),
                thumbnail = json.getString("thumbnail"),
                category = json.optString("category").ifBlank { null },
                language = json.getString("language"),
                avgLove = json.getDouble("avgLove"),
                avgFaith = json.getDouble("avgFaith"),
                avgHope = json.getDouble("avgHope"),
                avgTotal = json.getDouble("avgTotal"),
                totalRatings = json.getInt("totalRatings"),
                timestamp = json.getLong("timestamp")
            )
        }
    }
}

class VideoCacheManager(context: Context) {
    private val appContext = context.applicationContext
    
    companion object {
        private const val KEY_CACHED_VIDEOS = "cached_videos"
        private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
    }

    suspend fun cacheVideos(videos: List<VideoSearchResult>) {
        val cachedVideos = videos.map {
            CachedVideo(
                videoId = it.videoId,
                title = it.title,
                channelName = it.channelName,
                thumbnail = it.thumbnail,
                category = it.category,
                language = it.language,
                avgLove = it.avgLove,
                avgFaith = it.avgFaith,
                avgHope = it.avgHope,
                avgTotal = it.avgTotal,
                totalRatings = it.totalRatings
            )
        }
        
        val jsonArray = JSONArray()
        cachedVideos.forEach { jsonArray.put(it.toJson()) }
        
        GenericPrefs.setString(appContext, KEY_CACHED_VIDEOS, jsonArray.toString())
        GenericPrefs.setLong(appContext, KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
    }

    suspend fun getCachedVideos(): List<VideoSearchResult>? {
        var cachedTimestamp = GenericPrefs.getLong(appContext, KEY_CACHE_TIMESTAMP, 0L)
        if (cachedTimestamp == 0L) {
            val legacy = appContext.getSharedPreferences("video_cache", Context.MODE_PRIVATE)
            cachedTimestamp = legacy.getLong(KEY_CACHE_TIMESTAMP, 0L)
            if (cachedTimestamp > 0L) {
                GenericPrefs.setLong(appContext, KEY_CACHE_TIMESTAMP, cachedTimestamp)
                val legacyJson = legacy.getString(KEY_CACHED_VIDEOS, null)
                if (!legacyJson.isNullOrBlank()) {
                    GenericPrefs.setString(appContext, KEY_CACHED_VIDEOS, legacyJson)
                }
            }
        }
        val now = System.currentTimeMillis()
        
        if (now - cachedTimestamp > CACHE_EXPIRY_MS) {
            clearCache()
            return null
        }
        
        val jsonString = GenericPrefs.getString(appContext, KEY_CACHED_VIDEOS, null) ?: return null
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val cachedVideos = mutableListOf<VideoSearchResult>()
            
            for (i in 0 until jsonArray.length()) {
                val cached = CachedVideo.fromJson(jsonArray.getJSONObject(i))
                cachedVideos.add(
                    VideoSearchResult(
                        videoId = cached.videoId,
                        title = cached.title,
                        channelName = cached.channelName,
                        thumbnail = cached.thumbnail,
                        category = cached.category,
                        language = cached.language,
                        avgLove = cached.avgLove,
                        avgFaith = cached.avgFaith,
                        avgHope = cached.avgHope,
                        avgTotal = cached.avgTotal,
                        totalRatings = cached.totalRatings,
                        createdAt = 0L
                    )
                )
            }
            
            cachedVideos
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    suspend fun isCacheValid(): Boolean {
        val cachedTimestamp = GenericPrefs.getLong(appContext, KEY_CACHE_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        return (now - cachedTimestamp) <= CACHE_EXPIRY_MS
    }

    suspend fun clearCache() {
        GenericPrefs.setString(appContext, KEY_CACHED_VIDEOS, null)
        GenericPrefs.setLong(appContext, KEY_CACHE_TIMESTAMP, 0L)
    }
}