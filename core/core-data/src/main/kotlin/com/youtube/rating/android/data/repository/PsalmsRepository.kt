package com.youtube.rating.android.data.repository

import android.content.Context
import com.youtube.rating.android.utils.BibleApiService
import com.youtube.rating.android.utils.coroutines.suspendLazy
import com.youtube.rating.shared.models.PsalmDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.mongodb.kbson.serialization.EJson.Default.ignoreUnknownKeys


class PsalmsRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val enCacheFileName = "psalms_en_cache.json"

    // Cache parsed list to avoid re-reading/decoding large JSON on each screen open
    @Volatile
    private var cachedHr: List<PsalmDto>? = null

    private val cachedEn = suspendLazy { loadEnglishPsalmsOnce() }

    fun loadPsalms(): List<PsalmDto> {
        cachedHr?.let { return it }
        val rawText = context.resources
            .openRawResource(com.youtube.rating.core.data.R.raw.psalmi) // psalms.json -> R.raw.psalms
            .bufferedReader()
            .use { it.readText() }

        val list = json.decodeFromString<List<PsalmDto>>(rawText)
        cachedHr = list
        return list
    }

    suspend fun loadPsalms(language: BibleApiService.BibleLanguage): List<PsalmDto> {
        return when (language) {
            BibleApiService.BibleLanguage.ENGLISH -> cachedEn()
            else -> loadPsalms()
        }
    }

    private suspend fun loadEnglishPsalmsOnce(): List<PsalmDto> {
        readEnglishCache()?.let { return it }
        val list = mutableListOf<PsalmDto>()
        for (i in 1..150) {
            val response = BibleApiService.getPassage(
                bookId = "psalmi",
                chapter = i,
                language = BibleApiService.BibleLanguage.ENGLISH
            )
            if (response.isFailure) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(
                    response.exceptionOrNull() ?: IllegalStateException("Failed to load Psalm $i")
                )
                // Return whatever we managed to load so far instead of throwing.
                break
            }
            val text = response.getOrNull()?.text.orEmpty()
            val url = "https://www.bible.com/bible/206/PSA.$i.KJV"
            list.add(PsalmDto(psalm = i, url = url, title = "Psalm $i", text = text))
        }
        if (list.isNotEmpty()) writeEnglishCache(list = list)
        return list
    }

    private fun readEnglishCache(): List<PsalmDto>? {
        return runCatching {
            val file = context.filesDir.resolve(enCacheFileName)
            if (!file.exists() || file.length() <= 0L) return null
            val raw = file.readText()
            json.decodeFromString<List<PsalmDto>>(raw)
        }.getOrNull()
    }

    private fun writeEnglishCache(list: List<PsalmDto>) {
        runCatching {
            val file = context.filesDir.resolve(enCacheFileName)
            file.writeText(json.encodeToString(ListSerializer(PsalmDto.serializer()), list))
        }
    }
}
