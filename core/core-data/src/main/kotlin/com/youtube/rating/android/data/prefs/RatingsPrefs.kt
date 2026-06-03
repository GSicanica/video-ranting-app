package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.youtube.rating.shared.models.RatedVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

object RatingsPrefs : BasePrefs() {
    private val KEY_MY_RATINGS_JSON = stringPreferencesKey("my_ratings_json")

    private fun parseMyRatings(raw: String?): List<RatedVideo> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val videoId = obj.optString("videoId", "").trim()
                    if (videoId.isEmpty()) continue
                    add(
                        RatedVideo(
                            videoId = videoId,
                            videoTitle = obj.optString("videoTitle", ""),
                            channelName = obj.optString("channelName", ""),
                            thumbnail = obj.optString("thumbnail", ""),
                            category = obj.optString("category", "").trim().ifBlank { null },
                            myLove = obj.optInt("myLove", 0),
                            myFaith = obj.optInt("myFaith", 0),
                            myHope = obj.optInt("myHope", 0),
                            ratedAt = obj.optString("ratedAt", ""),
                            totalRatings = 0,
                            avgLove = null,
                            avgFaith = null,
                            avgHope = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            emptyList()
        }
    }

    private fun myRatingsToJson(videos: List<RatedVideo>): String {
        val arr = JSONArray()
        videos.forEach { v ->
            arr.put(
                JSONObject().apply {
                    put("videoId", v.videoId)
                    put("videoTitle", v.videoTitle)
                    put("channelName", v.channelName)
                    put("thumbnail", v.thumbnail)
                    if (!v.category.isNullOrBlank()) put("category", v.category)
                    put("myLove", v.myLove)
                    put("myFaith", v.myFaith)
                    put("myHope", v.myHope)
                    put("ratedAt", v.ratedAt)
                }
            )
        }
        return arr.toString()
    }

    fun myRatingsFlow(context: Context): Flow<List<RatedVideo>> =
        prefsFlow(context, KEY_MY_RATINGS_JSON, "[]").map(::parseMyRatings)

    suspend fun getMyRatings(context: Context): List<RatedVideo> =
        parseMyRatings(raw = readPref(context, KEY_MY_RATINGS_JSON, "[]"))

    suspend fun setMyRatings(context: Context, videos: List<RatedVideo>) {
        val normalized = videos.map { v ->
            v.copy(
                totalRatings = 0,
                avgLove = null,
                avgFaith = null,
                avgHope = null
            )
        }
        editPref(context) { prefs ->
            prefs[KEY_MY_RATINGS_JSON] = myRatingsToJson(videos = normalized)
        }
    }

    suspend fun upsertMyRating(
        context: Context,
        videoId: String,
        videoTitle: String,
        channelName: String,
        thumbnail: String,
        category: String?,
        myLove: Int,
        myFaith: Int,
        myHope: Int,
        ratedAt: String?
    ) {
        val normalizedId = videoId.trim()
        if (normalizedId.isEmpty()) return

        editPref(context) { prefs ->
            val current = parseMyRatings(raw = prefs[KEY_MY_RATINGS_JSON])
            val existing = current.firstOrNull { it.videoId == normalizedId }
            val effectiveRatedAt = ratedAt?.takeIf { it.isNotBlank() } ?: existing?.ratedAt.orEmpty()

            val updated = RatedVideo(
                videoId = normalizedId,
                videoTitle = videoTitle,
                channelName = channelName,
                thumbnail = thumbnail,
                category = category,
                myLove = myLove,
                myFaith = myFaith,
                myHope = myHope,
                ratedAt = effectiveRatedAt,
                totalRatings = 0,
                avgLove = null,
                avgFaith = null,
                avgHope = null
            )

            val without = current.filterNot { it.videoId == normalizedId }
            val next = listOf(updated) + without
            prefs[KEY_MY_RATINGS_JSON] = myRatingsToJson(videos = next)
        }
    }

    suspend fun removeMyRating(context: Context, videoId: String) {
        val normalizedId = videoId.trim()
        if (normalizedId.isEmpty()) return
        editPref(context) { prefs ->
            val current = parseMyRatings(raw = prefs[KEY_MY_RATINGS_JSON])
            val next = current.filterNot { it.videoId == normalizedId }
            prefs[KEY_MY_RATINGS_JSON] = myRatingsToJson(videos = next)
        }
    }
}
