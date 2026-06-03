package com.youtube.rating.android.localization

import android.content.Context
import kotlinx.coroutines.launch
import com.youtube.rating.android.utils.AppScope
import com.youtube.rating.android.data.prefs.LanguagePrefs

/**
 * Manages content language preferences
 * ✅ FIXED: Uses applicationContext to prevent context leaks
 */
class ContentLanguageManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = AppScope.get()
    
    companion object {
        private const val KEY_CONTENT_LANGUAGES = "selected_content_languages"
    }
    
    fun saveContentLanguages(languages: Set<Strings.Language>) {
        val languageNames = languages.map { it.name }.toSet()
        // Save to DataStore
        scope.launch { LanguagePrefs.setContentLanguages(appContext, languageNames) }
        Strings.selectedContentLanguages = languages
    }
    
    suspend fun loadContentLanguages(): Set<Strings.Language> {
        val savedLanguages = LanguagePrefs.getContentLanguages(appContext).ifEmpty {
            // fallback legacy prefs (migration)
            val legacy = appContext.getSharedPreferences("content_language_prefs", Context.MODE_PRIVATE)
                .getStringSet(KEY_CONTENT_LANGUAGES, null) ?: emptySet()
            if (legacy.isNotEmpty()) {
                LanguagePrefs.setContentLanguages(appContext, legacy)
            }
            legacy
        }
        
        if (savedLanguages.isEmpty()) {
            // Automatski detektuj jezik sistema i postavi ga kao content jezik
            val systemLanguage = LanguageManager.detectSystemLanguage()
            
            val defaultLanguages = linkedSetOf(
                systemLanguage,
                Strings.Language.ENGLISH,
                Strings.Language.CROATIAN,
                Strings.Language.GERMAN
            )
            
            // Sačuvaj automatski detektovane jezike
            saveContentLanguages(languages = defaultLanguages)
            
            Strings.selectedContentLanguages = defaultLanguages
            return defaultLanguages
        }
        
        val languages = savedLanguages.mapNotNull { name ->
            try {
                Strings.Language.valueOf(name)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                null
            }
        }.toSet()
        
        Strings.selectedContentLanguages = languages
        return languages
    }
    
    suspend fun getSelectedContentLanguages(): Set<Strings.Language> {
        return loadContentLanguages()
    }
    
    fun getLanguageCode(language: Strings.Language): String {
        return when (language) {
            Strings.Language.ENGLISH -> "en"
            Strings.Language.CROATIAN -> "hr"
            Strings.Language.GERMAN -> "de"
        }
    }
    
    fun getLanguageName(language: Strings.Language): String {
        return when (language) {
            Strings.Language.ENGLISH -> "🇬🇧 English"
            Strings.Language.CROATIAN -> "🇭🇷 Hrvatski"
            Strings.Language.GERMAN -> "🇩🇪 Deutsch"
        }
    }
}
