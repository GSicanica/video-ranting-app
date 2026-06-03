package com.youtube.rating.android.utils

import com.youtube.rating.android.storage.FastingReminderSettings
import com.youtube.rating.shared.data.FavoriteVideoModel
import com.youtube.rating.shared.data.NoteModel
import com.youtube.rating.shared.data.OfflineVideoModel
import com.youtube.rating.shared.backup.BackupJsonCodec as SharedBackupJsonCodec
import org.json.JSONArray
import org.json.JSONObject

/**
 * Centralized JSON mapping for backup/import payloads.
 * Keeps mapping logic in one place to reduce duplication and restore bugs.
 */
object BackupJsonCodec {

    fun notesToJsonArray(notes: List<NoteModel>): JSONArray =
        JSONArray(SharedBackupJsonCodec.encodeNotes(notes))

    fun notesFromJsonArray(jsonArray: JSONArray): List<NoteModel> =
        SharedBackupJsonCodec.decodeNotes(jsonArray.toString())

    fun favoriteFromJson(json: JSONObject): FavoriteVideoModel =
        SharedBackupJsonCodec.decodeFavorites("[$json]").first()

    fun favoritesToJsonArray(favorites: List<FavoriteVideoModel>): JSONArray =
        JSONArray(SharedBackupJsonCodec.encodeFavorites(favorites))

    fun favoritesFromJsonArray(jsonArray: JSONArray): List<FavoriteVideoModel> =
        SharedBackupJsonCodec.decodeFavorites(jsonArray.toString())

    fun offlineVideoFromJson(json: JSONObject): OfflineVideoModel =
        SharedBackupJsonCodec.decodeOfflineVideos("[$json]").first()

    fun offlineVideosToJsonArray(videos: List<OfflineVideoModel>): JSONArray =
        JSONArray(SharedBackupJsonCodec.encodeOfflineVideos(videos))

    fun offlineVideosFromJsonArray(jsonArray: JSONArray): List<OfflineVideoModel> =
        SharedBackupJsonCodec.decodeOfflineVideos(jsonArray.toString())

    fun fastingReminderToJson(reminder: FastingReminderSettings): JSONObject = JSONObject().apply {
        put("enabled", reminder.enabled)
        put("hour", reminder.hour)
        put("minute", reminder.minute)
    }

    fun fastingReminderFromJson(json: JSONObject): FastingReminderSettings = FastingReminderSettings(
        enabled = json.optBoolean("enabled", false),
        hour = json.optInt("hour", 6),
        minute = json.optInt("minute", 0)
    )
}
