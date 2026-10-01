package com.example.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppFontFamily(val family: FontFamily, val displayName: String) {
    DEFAULT(FontFamily.Default, "Default"),
    SERIF(FontFamily.Serif, "Serif"),
    MONOSPACE(FontFamily.Monospace, "Monospace"),
    SANS_SERIF(FontFamily.SansSerif, "Sans Serif")
}

class AppSettings {
    val themeMode = mutableStateOf(AppThemeMode.SYSTEM)
    val fontFamily = mutableStateOf(AppFontFamily.DEFAULT)
}

val LocalAppSettings = compositionLocalOf { AppSettings() }

