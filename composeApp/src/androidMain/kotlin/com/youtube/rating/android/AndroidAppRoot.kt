package com.youtube.rating.android

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.youtube.rating.android.data.prefs.ThemeColors
import com.youtube.rating.android.data.settings.SettingsRepository
import com.youtube.rating.android.sentry.SentryBreadcrumbs
import com.youtube.rating.core.designsystem.theme.ThemeMode
import com.youtube.rating.core.designsystem.theme.ThemeOverrides
import com.youtube.rating.core.designsystem.theme.AppBackground
import com.youtube.rating.core.designsystem.theme.YouTubeRatingTheme
import org.koin.compose.koinInject

@Composable
fun AndroidAppRoot(
    activity: ComponentActivity,
    initialIntent: Intent?
) {
    val brightnessManager: AppBrightnessManager = koinInject()
    val brightness by brightnessManager.brightness.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> brightnessManager.setInBackground(false)
                Lifecycle.Event.ON_PAUSE -> brightnessManager.setInBackground(true)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val settingsRepository: SettingsRepository = koinInject()
    val settingsState by settingsRepository.state.collectAsStateWithLifecycle()
    val themePrefs: ThemeColors = settingsState.themeColors
    val themeMode: ThemeMode = settingsState.themeMode

    val themeOverrides = ThemeOverrides(
        primary = themePrefs.primary?.let { Color(it) },
        secondary = themePrefs.secondary?.let { Color(it) },
        tertiary = themePrefs.tertiary?.let { Color(it) }
    )

    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme =
        com.youtube.rating.android.data.prefs.ThemePrefs.resolveUseDarkTheme(themeMode, isSystemDark)

    // 1.0 = normal, 0.0 = max dim (overlay strength capped)
    val dimAlpha = ((1f - brightness) * 0.75f).coerceIn(0f, 0.75f)

    val content: @Composable () -> Unit = {
        YouTubeRatingTheme(
            darkTheme = useDarkTheme,
            themeOverrides = themeOverrides
        ) {
            LaunchedEffect(Unit) {
                SentryBreadcrumbs.screen("MainActivity")
            }

            val bg = MaterialTheme.colorScheme.background
            SideEffect {
                val controller =
                    WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                val perceivedLuma = bg.luminance() * (1f - dimAlpha)
                val useLightIcons = perceivedLuma <= 0.5f
                controller.isAppearanceLightStatusBars = !useLightIcons
                controller.isAppearanceLightNavigationBars = !useLightIcons
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                AppBackground(modifier = Modifier.fillMaxSize()) {
                    RatingApp(initialIntent = initialIntent)

                    if (dimAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = dimAlpha))
                        )
                    }
                }
            }
        }
    }

    content()
}
