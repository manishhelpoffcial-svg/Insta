package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class InstagramColors(
    val background: Color,
    val surface: Color,
    val pillBackground: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val hairline: Color,
    val trackBackground: Color,
    val isDark: Boolean,
)

val LightInstagramColors = InstagramColors(
    background = IgLightBackground,
    surface = IgLightSurface,
    pillBackground = IgLightPill,
    cardBackground = IgLightCard,
    textPrimary = IgLightTextPrimary,
    textSecondary = IgLightTextSecondary,
    hairline = IgLightHairline,
    trackBackground = IgLightTrack,
    isDark = false,
)

val DarkInstagramColors = InstagramColors(
    background = IgDarkBackground,
    surface = IgDarkSurface,
    pillBackground = IgDarkPill,
    cardBackground = IgDarkCard,
    textPrimary = IgDarkTextPrimary,
    textSecondary = IgDarkTextSecondary,
    hairline = IgDarkHairline,
    trackBackground = IgDarkTrack,
    isDark = true,
)

val LocalInstagramColors = staticCompositionLocalOf { LightInstagramColors }

private val DarkColorScheme = darkColorScheme(
    primary = IgBoostBlue,
    background = IgDarkBackground,
    surface = IgDarkSurface,
    surfaceVariant = IgDarkCard,
    onPrimary = Color.White,
    onBackground = IgDarkTextPrimary,
    onSurface = IgDarkTextPrimary,
    onSurfaceVariant = IgDarkTextSecondary,
    outline = IgDarkHairline,
)

private val LightColorScheme = lightColorScheme(
    primary = IgBoostBlue,
    background = IgLightBackground,
    surface = IgLightSurface,
    surfaceVariant = IgLightCard,
    onPrimary = Color.White,
    onBackground = IgLightTextPrimary,
    onSurface = IgLightTextPrimary,
    onSurfaceVariant = IgLightTextSecondary,
    outline = IgLightHairline,
)

object InstagramTheme {
    val colors: InstagramColors
        @Composable get() = LocalInstagramColors.current
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val igColors = if (darkTheme) DarkInstagramColors else LightInstagramColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalInstagramColors provides igColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
