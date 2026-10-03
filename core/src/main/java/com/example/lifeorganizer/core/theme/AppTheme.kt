package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalDesignStyle = compositionLocalOf { "pastel" }
val LocalThemeStyle = LocalDesignStyle

@Composable
fun AppTheme(
    designStyle: String = "pastel",
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Int? = null,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDesignStyle provides designStyle) {
        when (designStyle) {
            "midnight" -> MidnightTheme { content() }
            "neon" -> NeonTheme(accentColor = accentColor) { content() }
            "white_light" -> WhiteLightTheme { content() }
            "pastel" -> PastelTheme(darkTheme = darkTheme, accentColor = accentColor) { content() }
            else -> PastelTheme(darkTheme = darkTheme, accentColor = accentColor) { content() }
        }
    }
}

@Composable
fun AppTheme(
    style: String,
    mode: String,
    accent: String,
    content: @Composable () -> Unit
) {
    val dark = mode == "dark"
    AppTheme(
        designStyle = style,
        darkTheme = dark,
        accentColor = null,
        content = content
    )
}
