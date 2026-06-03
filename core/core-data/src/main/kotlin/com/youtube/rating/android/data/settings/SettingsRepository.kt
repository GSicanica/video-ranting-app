package com.youtube.rating.android.data.settings

import android.content.Context
import com.youtube.rating.android.data.prefs.ThemeColors
import com.youtube.rating.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Single source of truth for app settings.
 */
interface SettingsRepository {
    val state: StateFlow<SettingsState>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setThemeColors(colors: ThemeColors?)
    suspend fun setHomeScreenStyle(style: String)
    suspend fun setNewVideoNotificationsEnabled(enabled: Boolean)
    suspend fun setWatchHistoryEnabled(enabled: Boolean)
    suspend fun setResumePlaybackEnabled(enabled: Boolean)
    suspend fun setGridViewEnabled(enabled: Boolean)

    @Deprecated("Use setThemeMode(mode = ThemeMode) instead.")
    suspend fun setDarkMode(isDarkMode: Boolean?)
}

class SettingsRepositoryImpl(
    private val appContext: Context
) : SettingsRepository {
    private val scope = com.youtube.rating.android.utils.AppScope.get()

    override val state: StateFlow<SettingsState> = run {
        val themeFlow = kotlinx.coroutines.flow.combine(
            com.youtube.rating.android.data.prefs.ThemePrefs.themePrefsFlow(appContext),
            com.youtube.rating.android.data.prefs.ThemePrefs.themeModeFlow(appContext),
            com.youtube.rating.android.data.prefs.ThemePrefs.homeScreenStyleFlow(appContext),
            com.youtube.rating.android.data.prefs.HomePrefs.lockFeaturedHomeFlow(appContext),
            com.youtube.rating.android.data.prefs.ScrollEffectsPrefs.disableFlow(appContext)
        ) { themeColors, themeMode, homeScreenStyle, lockFeaturedHome, scrollEffectsDisabled ->
            ThemeBundle(
                colors = themeColors,
                mode = themeMode,
                homeScreenStyle = homeScreenStyle,
                lockFeaturedHome = lockFeaturedHome,
                scrollEffectsDisabled = scrollEffectsDisabled
            )
        }
        val videoFlow = kotlinx.coroutines.flow.combine(
            com.youtube.rating.android.data.prefs.VideoPrefs.newVideoNotificationsEnabledFlow(appContext),
            com.youtube.rating.android.data.prefs.VideoPrefs.watchHistoryEnabledFlow(appContext),
            com.youtube.rating.android.data.prefs.VideoPrefs.resumePlaybackEnabledFlow(appContext)
        ) { newVideoNotificationsEnabled, watchHistoryEnabled, resumePlaybackEnabled ->
            Triple(newVideoNotificationsEnabled, watchHistoryEnabled, resumePlaybackEnabled)
        }
        kotlinx.coroutines.flow.combine(
            themeFlow,
            videoFlow,
            com.youtube.rating.android.data.prefs.HomePrefs.isGridViewFlow(appContext)
        ) { theme, video, isGridView ->
            SettingsState(
                themeColors = theme.colors,
                themeMode = theme.mode,
                homeScreenStyle = theme.homeScreenStyle,
                lockFeaturedHome = theme.lockFeaturedHome,
                scrollEffectsDisabled = theme.scrollEffectsDisabled,
                newVideoNotificationsEnabled = video.first,
                watchHistoryEnabled = video.second,
                resumePlaybackEnabled = video.third,
                isGridView = isGridView
            )
        }.stateIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = SettingsState()
        )
    }

    private data class ThemeBundle(
        val colors: com.youtube.rating.android.data.prefs.ThemeColors,
        val mode: ThemeMode,
        val homeScreenStyle: String,
        val lockFeaturedHome: Boolean,
        val scrollEffectsDisabled: Boolean
    )

    override suspend fun setThemeMode(mode: ThemeMode) {
        com.youtube.rating.android.data.prefs.ThemePrefs.setThemeMode(appContext, mode)
    }

    @Deprecated("Use setThemeMode(mode = ThemeMode) instead.")
    override suspend fun setDarkMode(isDarkMode: Boolean?) {
        com.youtube.rating.android.data.prefs.ThemePrefs.setDarkMode(appContext, isDarkMode)
    }

    override suspend fun setThemeColors(colors: ThemeColors?) {
        if (colors == null) {
            com.youtube.rating.android.data.prefs.ThemePrefs.clearThemeColors(appContext)
            return
        }
        com.youtube.rating.android.data.prefs.ThemePrefs.setThemePrimary(appContext, colors.primary)
        com.youtube.rating.android.data.prefs.ThemePrefs.setThemeSecondary(appContext, colors.secondary)
        com.youtube.rating.android.data.prefs.ThemePrefs.setThemeTertiary(appContext, colors.tertiary)
    }

    override suspend fun setHomeScreenStyle(style: String) {
        com.youtube.rating.android.data.prefs.ThemePrefs.setHomeScreenStyle(appContext, style)
    }

    override suspend fun setNewVideoNotificationsEnabled(enabled: Boolean) {
        com.youtube.rating.android.data.prefs.VideoPrefs.setNewVideoNotificationsEnabled(appContext, enabled)
    }

    override suspend fun setWatchHistoryEnabled(enabled: Boolean) {
        com.youtube.rating.android.data.prefs.VideoPrefs.setWatchHistoryEnabled(appContext, enabled)
    }

    override suspend fun setResumePlaybackEnabled(enabled: Boolean) {
        com.youtube.rating.android.data.prefs.VideoPrefs.setResumePlaybackEnabled(appContext, enabled)
    }

    override suspend fun setGridViewEnabled(enabled: Boolean) {
        com.youtube.rating.android.data.prefs.HomePrefs.setGridView(appContext, enabled)
    }
}
