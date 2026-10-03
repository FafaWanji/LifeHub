package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// Lighter than the old #546E7A so it stays readable (≥ 7:1) on pure black.
val MutedSlate = Color(0xFF9DB2BF)

private val MidnightColorScheme = accentScheme(
    accent = MutedSlate,
    secondaryAccent = Color(0xFFB0A8C8),
    tertiaryAccent = Color(0xFF8FB8A8),
    dark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF0E0F11),
    surfaceVariant = Color(0xFF1A1C1F),
    onBackground = Color(0xFFE4E6EA)
)

private val MidnightShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val MidnightTypography = AppTypography.copy(
    bodySmall = AppTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelSmall = AppTypography.labelSmall.copy(fontFamily = FontFamily.Monospace),
    labelMedium = AppTypography.labelMedium.copy(fontFamily = FontFamily.Monospace)
)

@Composable
fun MidnightTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MidnightColorScheme,
        shapes = MidnightShapes,
        typography = MidnightTypography,
        content = content
    )
}
