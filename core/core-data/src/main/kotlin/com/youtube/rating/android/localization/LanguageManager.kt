package com.youtube.rating.android.localization

import android.content.Context
import java.util.Locale
import com.youtube.rating.android.data.prefs.GenericPrefs
import com.youtube.rating.android.data.prefs.LanguagePrefs

/**
 * Manages UI language preferences
 * ✅ FIXED: Uses applicationContext to prevent context leaks
 */
class LanguageManager(context: Context) {
    private val appContext = context.applicationContext
    
    companion object {
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_AUTO_DETECTED = "auto_detected_language"
        
        /**
         * Detektuje jezik sistema (mobitela) i mapira ga na podržane jezike
         */
        fun detectSystemLanguage(): Strings.Language {
            val systemLocale = Locale.getDefault()
            val languageCode = systemLocale.language.lowercase()
            
            return when (languageCode) {
                "de" -> Strings.Language.GERMAN
                "en" -> Strings.Language.ENGLISH
                "hr", "sr", "bs" -> Strings.Language.CROATIAN
                else -> Strings.Language.CROATIAN
            }
        }
    }
    
    suspend fun saveLanguage(language: Strings.Language) {
        LanguagePrefs.setAppLanguage(appContext, language.name)
        GenericPrefs.setBoolean(appContext, KEY_AUTO_DETECTED, false)
        Strings.currentLanguage = language
    }

    suspend fun loadLanguage(): Strings.Language {
        val autoDetected = GenericPrefs.getBoolean(appContext, KEY_AUTO_DETECTED, false)
        var savedLanguage = LanguagePrefs.getAppLanguage(appContext) ?: run {
            val legacy = appContext.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, null)
            if (legacy != null) LanguagePrefs.setAppLanguage(appContext, legacy)
            legacy
        }

        if (savedLanguage == null) {
            val detectedLanguage = detectSystemLanguage()
            LanguagePrefs.setAppLanguage(appContext, detectedLanguage.name)
            GenericPrefs.setBoolean(appContext, KEY_AUTO_DETECTED, true)
            Strings.currentLanguage = detectedLanguage
            return detectedLanguage
        }

        // If language was auto-detected before, keep it synced with current device language.
        if (autoDetected) {
            val detectedLanguage = detectSystemLanguage()
            if (savedLanguage != detectedLanguage.name) {
                LanguagePrefs.setAppLanguage(appContext, detectedLanguage.name)
                savedLanguage = detectedLanguage.name
            }
        }

        val language = runCatching { Strings.Language.valueOf(savedLanguage) }
            .getOrElse { e ->
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                if (autoDetected) detectSystemLanguage() else Strings.Language.ENGLISH
            }

        Strings.currentLanguage = language
        return language
    }

    /**
     * Provjerava da li je jezik automatski detektovan ili ručno postavljen
     */
    suspend fun isAutoDetected(): Boolean {
        return GenericPrefs.getBoolean(appContext, KEY_AUTO_DETECTED, false)
    }
}
