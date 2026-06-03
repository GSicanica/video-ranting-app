package com.youtube.rating.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.youtube.rating.android.data.models.VideoClip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object ClipsRepository {
    private val KEY_CLIPS = stringPreferencesKey("clips_json_v1")
    private val json = Json { ignoreUnknownKeys = true }

    fun clipsFlow(context: Context): Flow<List<VideoClip>> =
        PrefsDataStore.dataStore(context).data.map { prefs ->
            prefs[KEY_CLIPS]?.let { stored ->
                runCatching { json.decodeFromString<List<VideoClip>>(stored) }.getOrDefault(emptyList())
            } ?: emptyList()
        }

    suspend fun saveClip(context: Context, clip: VideoClip) {
        PrefsDataStore.dataStore(context).edit { prefs ->
            val current = prefs[KEY_CLIPS]?.let { stored ->
                runCatching { json.decodeFromString<List<VideoClip>>(stored) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = listOf(clip) + current.filterNot { it.id == clip.id }
            prefs[KEY_CLIPS] = json.encodeToString(updated)
        }
    }

    suspend fun createAndSave(
        context: Context,
        videoId: String,
        title: String,
        thumbnail: String,
        channel: String,
        startSeconds: Int,
        endSeconds: Int
    ): VideoClip {
        val clip = VideoClip(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            title = title,
            thumbnail = thumbnail,
            channelName = channel,
            startSeconds = startSeconds,
            endSeconds = endSeconds,
            createdAt = System.currentTimeMillis()
        )
        saveClip(context = context, clip = clip)
        return clip
    }

    suspend fun deleteClip(context: Context, clipId: String) {
        PrefsDataStore.dataStore(context).edit { prefs ->
            val current = prefs[KEY_CLIPS]?.let { stored ->
                runCatching { json.decodeFromString<List<VideoClip>>(stored) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[KEY_CLIPS] = json.encodeToString(current.filterNot { it.id == clipId })
        }
    }

    suspend fun reorderClips(context: Context, newOrder: List<VideoClip>) {
        PrefsDataStore.dataStore(context).edit { prefs ->
            val current = prefs[KEY_CLIPS]?.let { stored ->
                runCatching { json.decodeFromString<List<VideoClip>>(stored) }.getOrDefault(emptyList())
            } ?: emptyList()

            // Keep only known clips and avoid accidental duplicates
            val currentById = current.associateBy { it.id }
            val deduped = mutableListOf<VideoClip>()
            val seen = hashSetOf<String>()

            newOrder.forEach { clip ->
                val existing = currentById[clip.id]
                if (existing != null && seen.add(existing.id)) {
                    deduped += existing
                }
            }

            // Append any clips that might have been added concurrently but not present in the drag order
            current.forEach { clip ->
                if (seen.add(clip.id)) deduped += clip
            }

            prefs[KEY_CLIPS] = json.encodeToString(deduped)
        }
    }
}
