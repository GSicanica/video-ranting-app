package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.SaintOfDayResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.youtube.rating.android.data.prefs.SaintsPrefs

class SaintOfDayManager(
    private val context: Context,
    private val apiClient: RatingApiClient
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun getSaintOfDayOncePerDay(): SaintOfDayResponse? = withContext(ioDispatcher) {
        val today = LocalDate.now().toString()
        val resp = runCatching { apiClient.getSaintOfDay(today) }.getOrNull()
        if (resp != null && resp.success) {
            val responseDate = when {
                !resp.date.isNullOrBlank() -> resp.date
                else -> runCatching {
                    Instant.parse(resp.fetchedAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                }.getOrDefault(today)
            }
            if (responseDate == today) {
                SaintsPrefs.setSaintOfDayDate(context, responseDate)
                SaintsPrefs.setSaintOfDayJson(context, json.encodeToString(resp))
                return@withContext resp
            }
            // If server responds with non-today date, don't show stale content.
            return@withContext null
        }

        val lastDate = SaintsPrefs.getSaintOfDayDate(context)
        val cachedJson = SaintsPrefs.getSaintOfDayJson(context)
        if (lastDate == today && !cachedJson.isNullOrBlank()) {
            return@withContext runCatching { json.decodeFromString(SaintOfDayResponse.serializer(), cachedJson) }
                .getOrNull()
        }
        null
    }

    suspend fun getSaintByDate(date: String): SaintOfDayResponse? = withContext(ioDispatcher) {
        val resp = runCatching { apiClient.getSaintOfDay(date) }.getOrNull() ?: return@withContext null
        if (resp.success) resp else null
    }
}