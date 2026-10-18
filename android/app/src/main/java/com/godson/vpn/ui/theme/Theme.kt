package com.godson.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.godson.vpn.data.ThemeMode

/** Respecte la préférence « Réduire les animations » de l'application et du système. */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val DarkColors = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = SoftWhite,
    secondary = CyberCyan,
    onSecondary = MidnightNavy,
    tertiary = SuccessGreen,
    background = MidnightNavy,
    onBackground = SoftWhite,
    surface = DeepNavy,
    onSurface = SoftWhite,
    surfaceVariant = PanelNavy,
    onSurfaceVariant = SlateGray,
    outline = SlateGray.copy(alpha = 0.4f),
    outlineVariant = SlateGray.copy(alpha = 0.18f),
    error = ErrorRed,
    onError = SoftWhite
)

private val LightColors = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = SoftWhite,
    secondary = ElectricBlue,
    onSecondary = SoftWhite,
    tertiary = SuccessGreen,
    background = SoftWhite,
    onBackground = MidnightNavy,
    surface = LightSurface,
    onSurface = MidnightNavy,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = SlateGray,
    outline = SlateGray.copy(alpha = 0.6f),
    outlineVariant = SlateGray.copy(alpha = 0.3f),
    error = ErrorRed,
    onError = SoftWhite
)

@Composable
fun GodsonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    reduceAnimations: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalReducedMotion provides reduceAnimations) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = GodsonTypography,
            shapes = GodsonShapes,
            content = content
        )
    }
}
