package com.youtube.rating.android.ui.arch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.youtube.rating.core.designsystem.theme.ThemeMode

/**
 * EtchDroid-inspired small contract for screens/viewmodels that carry theme settings in their state.
 */
interface ThemeState {
    val themeMode: ThemeMode
}

/**
 * Computes whether dark theme should be used given a [ThemeMode].
 */
@Composable
fun rememberUseDarkTheme(mode: ThemeMode): State<Boolean> {
    val systemDark = isSystemInDarkTheme()
    return remember(mode, systemDark) {
        derivedStateOf {
            when (mode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
        }
    }
}

