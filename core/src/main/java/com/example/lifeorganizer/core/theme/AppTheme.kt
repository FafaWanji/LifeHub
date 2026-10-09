package com.example.lifeorganizer.core.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
            "midnight" -> MidnightTheme { SystemBarIcons(); content() }
            "neon" -> NeonTheme(accentColor = accentColor) { SystemBarIcons(); content() }
            "white_light" -> WhiteLightTheme { SystemBarIcons(); content() }
            else -> PastelTheme(darkTheme = darkTheme, accentColor = accentColor) { SystemBarIcons(); content() }
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

/**
 * Clock, Wi-Fi and battery icons follow the app's own background (e.g. White Light on a phone in
 * dark mode), not the system setting – otherwise they are white on white.
 */
@Composable
private fun SystemBarIcons() {
    val view = LocalView.current
    val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    if (view.isInEditMode) return
    SideEffect {
        var ctx = view.context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        val window = (ctx as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }
    }
}
