package com.example.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    DARK("Dark"),
    LIGHT("Light"),
}

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
    val spinnerTint: Color,
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
    spinnerTint = Color(0xFF737373),
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
    spinnerTint = Color(0xFFA8A8A8),
    isDark = true,
)

val LocalInstagramColors = staticCompositionLocalOf { LightInstagramColors }
val LocalReducedMotion = staticCompositionLocalOf { false }

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

    val isReducedMotion: Boolean
        @Composable get() = LocalReducedMotion.current
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    },
    content: @Composable () -> Unit,
) {
    val igColors = if (darkTheme) DarkInstagramColors else LightInstagramColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
            scale == 0f
        }.getOrDefault(false)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            runCatching {
                val window = (view.context as? Activity)?.window ?: return@runCatching
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalInstagramColors provides igColors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
