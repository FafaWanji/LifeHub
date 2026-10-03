package com.example.lifeorganizer.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Derives a complete, contrast-safe Material 3 color scheme from a single accent color.
 *
 * Pastel / neon accents are too light to be used as text on light backgrounds, so in light mode
 * the accent is deepened for `primary` and softened for the container roles.
 */
internal fun accentScheme(
    accent: Color,
    secondaryAccent: Color,
    tertiaryAccent: Color,
    dark: Boolean,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    onBackground: Color
): ColorScheme {
    val muted = lerp(onBackground, background, 0.38f)
    return if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = lerp(accent, Color.Black, 0.8f),
            primaryContainer = lerp(accent, background, 0.72f),
            onPrimaryContainer = lerp(accent, Color.White, 0.55f),
            secondary = secondaryAccent,
            onSecondary = lerp(secondaryAccent, Color.Black, 0.8f),
            secondaryContainer = lerp(secondaryAccent, background, 0.75f),
            onSecondaryContainer = lerp(secondaryAccent, Color.White, 0.6f),
            tertiary = tertiaryAccent,
            onTertiary = lerp(tertiaryAccent, Color.Black, 0.8f),
            tertiaryContainer = lerp(tertiaryAccent, background, 0.75f),
            onTertiaryContainer = lerp(tertiaryAccent, Color.White, 0.6f),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            surfaceContainerLowest = lerp(background, Color.Black, 0.3f),
            surfaceContainerLow = lerp(background, surface, 0.5f),
            surfaceContainer = surface,
            surfaceContainerHigh = lerp(surface, surfaceVariant, 0.5f),
            surfaceContainerHighest = surfaceVariant,
            outline = lerp(onBackground, background, 0.6f),
            outlineVariant = lerp(onBackground, background, 0.82f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = lerp(accent, Color.Black, 0.42f),
            onPrimary = Color.White,
            primaryContainer = lerp(accent, Color.White, 0.55f),
            onPrimaryContainer = lerp(accent, Color.Black, 0.72f),
            secondary = lerp(secondaryAccent, Color.Black, 0.45f),
            onSecondary = Color.White,
            secondaryContainer = lerp(secondaryAccent, Color.White, 0.6f),
            onSecondaryContainer = lerp(secondaryAccent, Color.Black, 0.72f),
            tertiary = lerp(tertiaryAccent, Color.Black, 0.45f),
            onTertiary = Color.White,
            tertiaryContainer = lerp(tertiaryAccent, Color.White, 0.6f),
            onTertiaryContainer = lerp(tertiaryAccent, Color.Black, 0.72f),
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = muted,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = lerp(background, surface, 0.5f),
            surfaceContainer = surface,
            surfaceContainerHigh = lerp(surface, surfaceVariant, 0.5f),
            surfaceContainerHighest = surfaceVariant,
            outline = lerp(onBackground, background, 0.55f),
            outlineVariant = lerp(onBackground, background, 0.85f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}
