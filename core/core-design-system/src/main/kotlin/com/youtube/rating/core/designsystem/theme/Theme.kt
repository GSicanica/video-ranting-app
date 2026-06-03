package com.youtube.rating.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// Refined palette: deep teal primaries with coral accents and balanced neutrals.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),          // bright teal for CTAs
    onPrimary = Color(0xFF002023),
    primaryContainer = Color(0xFF124852),
    onPrimaryContainer = Color(0xFFBDF6FF),

    secondary = Color(0xFF8FA3B3),        // cool slate for chips/labels
    onSecondary = Color(0xFF0F1B26),
    secondaryContainer = Color(0xFF2C3A46),
    onSecondaryContainer = Color(0xFFD6E5F2),

    tertiary = Color(0xFFFF9E80),         // soft coral accent
    onTertiary = Color(0xFF3A0C00),
    tertiaryContainer = Color(0xFF5B1F0E),
    onTertiaryContainer = Color(0xFFFFD8CB),

    background = Color(0xFF0F1318),
    onBackground = Color(0xFFF5F8FB),
    surface = Color(0xFF141920),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF2C343D),
    onSurfaceVariant = Color(0xFFF0F5FA),
    outline = Color(0xFF74808B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F8696),          // deep teal primary
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6EFFF),
    onPrimaryContainer = Color(0xFF001F25),

    secondary = Color(0xFF5F7080),        // balanced slate
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDBE5F0),
    onSecondaryContainer = Color(0xFF111D27),

    tertiary = Color(0xFFE36A4D),         // coral accent for highlights
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9CC),
    onTertiaryContainer = Color(0xFF330A00),

    background = Color(0xFFFAFBFD),
    onBackground = Color(0xFF0B0E11),
    surface = Color(0xFFF5F7FA),
    onSurface = Color(0xFF0F1418),
    surfaceVariant = Color(0xFFE0E6ED),
    onSurfaceVariant = Color(0xFF0D1116),
    outline = Color(0xFF757E88)
)

@Composable
fun YouTubeRatingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeOverrides: ThemeOverrides = ThemeOverrides(),
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = applyOverrides(base = baseScheme, overrides = themeOverrides, isDark = darkTheme)
    CompositionLocalProvider(LocalSpacing provides AppSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

data class ThemeOverrides(
    val primary: Color? = null,
    val secondary: Color? = null,
    val tertiary: Color? = null
)

private fun applyOverrides(
    base: androidx.compose.material3.ColorScheme,
    overrides: ThemeOverrides,
    isDark: Boolean
): androidx.compose.material3.ColorScheme {
    val primary = overrides.primary ?: base.primary
    val secondary = overrides.secondary ?: base.secondary
    val tertiary = overrides.tertiary ?: base.tertiary

    return base.copy(
        primary = primary,
        onPrimary = contentColorFor(color = primary),
        primaryContainer = containerColorFor(color = primary, isDark = isDark),
        onPrimaryContainer = contentColorFor(color = containerColorFor(primary, isDark)),
        secondary = secondary,
        onSecondary = contentColorFor(color = secondary),
        secondaryContainer = containerColorFor(color = secondary, isDark = isDark),
        onSecondaryContainer = contentColorFor(color = containerColorFor(secondary, isDark)),
        tertiary = tertiary,
        onTertiary = contentColorFor(color = tertiary),
        tertiaryContainer = containerColorFor(color = tertiary, isDark = isDark),
        onTertiaryContainer = contentColorFor(color = containerColorFor(tertiary, isDark))
    )
}

private fun contentColorFor(color: Color): Color {
    return if (color.luminance() > 0.5f) Color.Black else Color.White
}

private fun containerColorFor(color: Color, isDark: Boolean): Color {
    return if (isDark) darken(color = color, amount = 0.45f) else lighten(color = color, amount = 0.70f)
}

private fun lighten(color: Color, amount: Float): Color = lerp(color, Color.White, amount.coerceIn(0f, 1f))

private fun darken(color: Color, amount: Float): Color = lerp(color, Color.Black, amount.coerceIn(0f, 1f))
