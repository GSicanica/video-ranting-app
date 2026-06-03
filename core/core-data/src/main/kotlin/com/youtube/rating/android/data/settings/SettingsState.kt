package com.youtube.rating.android.data.settings

import com.youtube.rating.android.data.prefs.ThemeColors
import com.youtube.rating.core.designsystem.theme.ThemeMode

/**
 * Aggregated settings state for UI.
 */
data class SettingsState(
    val themeColors: ThemeColors = ThemeColors(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val homeScreenStyle: String = "default",
    val lockFeaturedHome: Boolean = true,
    val scrollEffectsDisabled: Boolean = true,
    val newVideoNotificationsEnabled: Boolean = false,
    val watchHistoryEnabled: Boolean = true,
    val resumePlaybackEnabled: Boolean = false,
    val isGridView: Boolean = true
)
