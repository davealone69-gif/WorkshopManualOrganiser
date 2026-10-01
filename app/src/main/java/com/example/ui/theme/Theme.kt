package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GothicColorScheme = darkColorScheme(
    primary = GothicRed,
    onPrimary = GothicWhite,
    primaryContainer = GothicDeepRed,
    onPrimaryContainer = GothicWhite,
    secondary = GothicGold,
    onSecondary = GothicBlack,
    secondaryContainer = GothicPurple,
    onSecondaryContainer = GothicWhite,
    tertiary = GothicSilver,
    onTertiary = GothicBlack,
    tertiaryContainer = GothicDarkGray,
    onTertiaryContainer = GothicSilver,
    background = GothicBlack,
    onBackground = GothicWhite,
    surface = GothicDarkGray,
    onSurface = GothicWhite,
    surfaceVariant = Color(0xFF25252D),
    onSurfaceVariant = GothicSilver,
    error = GothicRed,
    onError = GothicWhite
)

private val LightAppColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightText,
    secondary = LightSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightText,
    tertiary = LightTertiary,
    onTertiary = LightSurface,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightText,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = GothicRed,
    onError = LightSurface
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val appSettings = remember { AppSettings() }
    
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (appSettings.themeMode.value) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    
    val colorScheme = if (isDark) GothicColorScheme else LightAppColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppSettings provides appSettings) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getAppTypography(appSettings.fontFamily.value.family),
            content = content
        )
    }
}

