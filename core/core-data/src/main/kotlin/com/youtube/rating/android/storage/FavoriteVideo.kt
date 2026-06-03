package com.youtube.rating.android.storage

import org.json.JSONObject

data class FavoriteVideo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val avgLove: Double,
    val avgFaith: Double,
    val avgHope: Double,
    val totalRatings: Int,
    val category: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val type: FavoriteItemType = FavoriteItemType.VIDEO
) {
    fun toVideoSearchResult(): com.youtube.rating.shared.models.VideoSearchResult {
        return com.youtube.rating.shared.models.VideoSearchResult(
            videoId = videoId,
            title = title,
            channelName = channelName,
            thumbnail = thumbnail,
            totalRatings = totalRatings,
            avgLove = avgLove,
            avgFaith = avgFaith,
            avgHope = avgHope,
            avgTotal = if (totalRatings > 0) (avgLove + avgFaith + avgHope) / 3.0 else 0.0,
            category = category
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("videoId", videoId)
            put("title", title)
            put("thumbnail", thumbnail)
            put("channelName", channelName)
            put("avgLove", avgLove)
            put("avgFaith", avgFaith)
            put("avgHope", avgHope)
            put("totalRatings", totalRatings)
            put("category", category ?: "")
            put("timestamp", timestamp)
            put("type", type.name)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): FavoriteVideo {
            return FavoriteVideo(
                videoId = json.getString("videoId"),
                title = json.getString("title"),
                thumbnail = json.getString("thumbnail"),
                channelName = json.getString("channelName"),
                avgLove = json.getDouble("avgLove"),
                avgFaith = json.getDouble("avgFaith"),
                avgHope = json.getDouble("avgHope"),
                totalRatings = json.getInt("totalRatings"),
                category = json.getString("category").takeIf { it.isNotEmpty() },
                timestamp = json.getLong("timestamp"),
                type = try {
                    FavoriteItemType.valueOf(json.optString("type", "VIDEO"))
                } catch (e: Exception) {
                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                    FavoriteItemType.VIDEO
                }
            )
        }
    }
}
