package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF1E1E22)

private val WhiteLightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E9EE),
    onPrimaryContainer = Ink,
    secondary = Color(0xFF5B5B66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEFF3),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF3D6B8C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE3EDF5),
    onTertiaryContainer = Color(0xFF12324A),
    background = Color(0xFFF7F7F9),
    onBackground = Ink,
    surface = Color(0xFFF7F7F9),
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDEDF1),
    onSurfaceVariant = Color(0xFF6B6B75),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F1F4),
    surfaceContainerHighest = Color(0xFFEAEAEE),
    outline = Color(0xFFB4B4BC),
    outlineVariant = Color(0xFFE2E2E7)
)

private val WhiteLightShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun airy(style: androidx.compose.ui.text.TextStyle) =
    style.copy(lineHeight = (style.lineHeight.value + 2f).sp)

private val WhiteLightTypography = AppTypography.copy(
    bodySmall = airy(AppTypography.bodySmall),
    bodyMedium = airy(AppTypography.bodyMedium),
    bodyLarge = airy(AppTypography.bodyLarge),
    labelSmall = airy(AppTypography.labelSmall),
    labelMedium = airy(AppTypography.labelMedium),
    labelLarge = airy(AppTypography.labelLarge),
    titleSmall = airy(AppTypography.titleSmall),
    titleMedium = airy(AppTypography.titleMedium),
    titleLarge = airy(AppTypography.titleLarge)
)

@Composable
fun WhiteLightTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WhiteLightColorScheme,
        shapes = WhiteLightShapes,
        typography = WhiteLightTypography,
        content = content
    )
}
