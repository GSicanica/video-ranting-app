package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.youtube.rating.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Theme/color and visual style preferences.
 */
data class ThemeColors(
    val primary: Int? = null,
    val secondary: Int? = null,
    val tertiary: Int? = null
)

object ThemePrefs : BasePrefs() {
    private val KEY_THEME_PRIMARY = intPreferencesKey("theme_primary_color")
    private val KEY_THEME_SECONDARY = intPreferencesKey("theme_secondary_color")
    private val KEY_THEME_TERTIARY = intPreferencesKey("theme_tertiary_color")
    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    private val KEY_HOME_SCREEN_STYLE = stringPreferencesKey("home_screen_style")

    @Volatile
    private var cachedThemePrimary: Int? = null
    @Volatile
    private var cachedThemeSecondary: Int? = null
    @Volatile
    private var cachedThemeTertiary: Int? = null

    fun themePrefsFlow(context: Context): Flow<ThemeColors> =
        dataStoreFlow(context = context).map {
            ThemeColors(
                primary = it[KEY_THEME_PRIMARY],
                secondary = it[KEY_THEME_SECONDARY],
                tertiary = it[KEY_THEME_TERTIARY]
            )
        }

    suspend fun setThemePrimary(context: Context, color: Int?) {
        editPref(context) {
            if (color == null) it.remove(KEY_THEME_PRIMARY) else it[KEY_THEME_PRIMARY] = color
        }
        cachedThemePrimary = color
    }

    suspend fun setThemeSecondary(context: Context, color: Int?) {
        editPref(context) {
            if (color == null) it.remove(KEY_THEME_SECONDARY) else it[KEY_THEME_SECONDARY] = color
        }
        cachedThemeSecondary = color
    }

    suspend fun setThemeTertiary(context: Context, color: Int?) {
        editPref(context) {
            if (color == null) it.remove(KEY_THEME_TERTIARY) else it[KEY_THEME_TERTIARY] = color
        }
        cachedThemeTertiary = color
    }

    suspend fun clearThemeColors(context: Context) {
        editPref(context) {
            it.remove(KEY_THEME_PRIMARY)
            it.remove(KEY_THEME_SECONDARY)
            it.remove(KEY_THEME_TERTIARY)
        }
        cachedThemePrimary = null
        cachedThemeSecondary = null
        cachedThemeTertiary = null
    }

    fun darkModeFlow(context: Context): Flow<Boolean?> =
        dataStoreFlow(context = context).map { it[KEY_DARK_MODE] }

    suspend fun setDarkMode(context: Context, isDarkMode: Boolean?) {
        editPref(context) {
            if (isDarkMode == null) it.remove(KEY_DARK_MODE) else it[KEY_DARK_MODE] = isDarkMode
        }
    }

    /**
     * Compatibility mapping for the legacy nullable-boolean preference:
     * - null -> SYSTEM
     * - true -> DARK
     * - false -> LIGHT
     */
    fun themeModeFlow(context: Context): Flow<ThemeMode> =
        darkModeFlow(context = context).map { pref ->
            when (pref) {
                null -> ThemeMode.SYSTEM
                true -> ThemeMode.DARK
                false -> ThemeMode.LIGHT
            }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        setDarkMode(
            context,
            when (mode) {
                ThemeMode.SYSTEM -> null
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
        )
    }

    fun resolveUseDarkTheme(mode: ThemeMode, isSystemDark: Boolean): Boolean {
        return when (mode) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    }

    fun homeScreenStyleFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_HOME_SCREEN_STYLE, "default")

    suspend fun setHomeScreenStyle(context: Context, style: String) {
        editPref(context) { it[KEY_HOME_SCREEN_STYLE] = style }
    }
}
