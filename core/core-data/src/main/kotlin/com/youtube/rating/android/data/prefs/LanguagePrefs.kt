package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow

object LanguagePrefs : BasePrefs() {
    private val KEY_CONTENT_LANGUAGES = stringSetPreferencesKey("content_languages")
    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

    @Volatile
    private var cachedContentLanguages: Set<String>? = null
    @Volatile
    private var cachedAppLanguage: String? = null

    fun contentLanguagesFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_CONTENT_LANGUAGES, emptySet())

    suspend fun getContentLanguages(context: Context): Set<String> {
        return cachedContentLanguages
            ?: readPref(context, KEY_CONTENT_LANGUAGES, emptySet()).also { cachedContentLanguages = it.toSet() }
    }

    suspend fun setContentLanguages(context: Context, codes: Set<String>) {
        val normalized = codes.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        editPref(context) { it[KEY_CONTENT_LANGUAGES] = normalized }
        cachedContentLanguages = normalized
    }

    suspend fun getAppLanguage(context: Context): String? {
        return cachedAppLanguage
            ?: readPrefNullable(context, KEY_APP_LANGUAGE).also { cachedAppLanguage = it }
    }

    suspend fun setAppLanguage(context: Context, language: String) {
        editPref(context) { it[KEY_APP_LANGUAGE] = language }
        cachedAppLanguage = language
    }
}