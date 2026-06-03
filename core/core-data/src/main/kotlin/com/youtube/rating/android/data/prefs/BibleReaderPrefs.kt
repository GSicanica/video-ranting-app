package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

object BibleReaderPrefs : BasePrefs() {
    private val KEY_BIBLE_READER_TEXT_SCALE = floatPreferencesKey("bible_reader_text_scale")
    private val KEY_BIBLE_READER_FONT_SP = intPreferencesKey("bible_reader_font_sp")
    private val KEY_BIBLE_HIGHLIGHTS = stringSetPreferencesKey("bible_highlights")
    private val KEY_BIBLE_HIGHLIGHT_TEXTS = stringPreferencesKey("bible_highlight_texts_json")
    private val KEY_BIBLE_PASSAGE_CACHE = stringPreferencesKey("bible_passage_cache")
    private val KEY_OFFLINE_BIBLE_BOOK = stringPreferencesKey("offline_bible_book")
    private val KEY_TRAINING_BIBLE_LANGUAGE = stringPreferencesKey("training_bible_language")

    @Volatile
    private var cachedBibleReaderTextScale: Float? = null

    fun bibleReaderTextScaleFlow(context: Context): Flow<Float> =
        prefsFlow(context, KEY_BIBLE_READER_TEXT_SCALE, 1.0f)

    suspend fun getBibleReaderTextScale(context: Context, defaultValue: Float = 1.0f): Float {
        return cachedBibleReaderTextScale
            ?: readPref(context, KEY_BIBLE_READER_TEXT_SCALE, defaultValue)
                .also { cachedBibleReaderTextScale = it }
    }

    suspend fun setBibleReaderTextScale(context: Context, value: Float) {
        editPref(context) { it[KEY_BIBLE_READER_TEXT_SCALE] = value }
        cachedBibleReaderTextScale = value
    }

    fun bibleReaderFontSpFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_BIBLE_READER_FONT_SP, 14).map { it.coerceIn(10, 30) }

    suspend fun setBibleReaderFontSp(context: Context, fontSp: Int) {
        editPref(context) { it[KEY_BIBLE_READER_FONT_SP] = fontSp.coerceIn(10, 30) }
    }

    fun bibleHighlightsFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_BIBLE_HIGHLIGHTS, emptySet())

    suspend fun setBibleHighlights(context: Context, set: Set<String>) {
        editPref(context) { prefs -> prefs[KEY_BIBLE_HIGHLIGHTS] = set }
    }

    fun bibleHighlightTextsFlow(context: Context): Flow<Map<String, String>> =
        dataStoreFlow(context = context).map { prefs ->
            val raw = prefs[KEY_BIBLE_HIGHLIGHT_TEXTS] ?: "{}"
            try {
                val obj = JSONObject(raw)
                buildMap {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        put(k, obj.optString(k, ""))
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                emptyMap()
            }
        }

    suspend fun putBibleHighlightText(context: Context, token: String, text: String) {
        editPref(context) { prefs ->
            val raw = prefs[KEY_BIBLE_HIGHLIGHT_TEXTS] ?: "{}"
            val obj = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            obj.put(token, text)
            prefs[KEY_BIBLE_HIGHLIGHT_TEXTS] = obj.toString()
        }
    }

    suspend fun removeBibleHighlightText(context: Context, token: String) {
        editPref(context) { prefs ->
            val raw = prefs[KEY_BIBLE_HIGHLIGHT_TEXTS] ?: "{}"
            val obj = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            obj.remove(token)
            prefs[KEY_BIBLE_HIGHLIGHT_TEXTS] = obj.toString()
        }
    }

    suspend fun clearBibleHighlightTexts(context: Context) {
        editPref(context) { prefs -> prefs[KEY_BIBLE_HIGHLIGHT_TEXTS] = "{}" }
    }

    fun biblePassageCacheFlow(context: Context): Flow<Map<String, String>> =
        dataStoreFlow(context = context).map { prefs ->
            val raw = prefs[KEY_BIBLE_PASSAGE_CACHE] ?: "{}"
            try {
                val obj = JSONObject(raw)
                buildMap {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = obj.optString(k, "")
                        if (v.isNotBlank()) put(k, v)
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                emptyMap()
            }
        }

    suspend fun putBiblePassageCache(
        context: Context,
        key: String,
        content: String,
        maxEntries: Int = 50
    ) {
        if (key.isBlank() || content.isBlank()) return
        editPref(context) { prefs ->
            val raw = prefs[KEY_BIBLE_PASSAGE_CACHE] ?: "{}"
            val obj = try {
                JSONObject(raw)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                JSONObject()
            }
            obj.put(key, content)
            while (obj.length() > maxEntries) {
                val keys = obj.keys()
                if (!keys.hasNext()) break
                obj.remove(keys.next())
            }
            prefs[KEY_BIBLE_PASSAGE_CACHE] = obj.toString()
        }
    }

    fun offlineBibleBookFlow(context: Context): Flow<String?> =
        dataStoreFlow(context = context).map { it[KEY_OFFLINE_BIBLE_BOOK] }

    suspend fun getOfflineBibleBook(context: Context): String? =
        readPrefNullable(context, KEY_OFFLINE_BIBLE_BOOK)

    suspend fun setOfflineBibleBook(context: Context, bookId: String?) {
        editPref(context) { prefs ->
            if (bookId != null) prefs[KEY_OFFLINE_BIBLE_BOOK] = bookId
            else prefs.remove(KEY_OFFLINE_BIBLE_BOOK)
        }
    }

    fun trainingBibleLanguageFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_TRAINING_BIBLE_LANGUAGE, "hr")

    suspend fun setTrainingBibleLanguage(context: Context, code: String) {
        editPref(context) { prefs -> prefs[KEY_TRAINING_BIBLE_LANGUAGE] = code }
    }
}