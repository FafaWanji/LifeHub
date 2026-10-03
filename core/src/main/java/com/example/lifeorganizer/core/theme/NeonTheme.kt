package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Neon Accents
val ElectricMint = Color(0xFF00FFB2)
val NeonCyan = Color(0xFF00E5FF)
val AcidLime = Color(0xFFD4FF00)
val PlasmaPurple = Color(0xFFD500F9)
val HotMagenta = Color(0xFFFF007F)

val NeonAccents = listOf(ElectricMint, NeonCyan, AcidLime, PlasmaPurple, HotMagenta)

private val NeonShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private fun wider(style: androidx.compose.ui.text.TextStyle) =
    style.copy(letterSpacing = (style.letterSpacing.value + 0.5f).sp)

private val NeonTypography = AppTypography.copy(
    labelSmall = wider(AppTypography.labelSmall),
    labelMedium = wider(AppTypography.labelMedium),
    titleSmall = wider(AppTypography.titleSmall),
    titleMedium = wider(AppTypography.titleMedium)
)

@Composable
fun NeonTheme(
    accentColor: Int?,
    content: @Composable () -> Unit
) {
    val accent = accentColor?.let { Color(it) } ?: ElectricMint
    val others = NeonAccents.filter { it != accent }

    val colorScheme = accentScheme(
        accent = accent,
        secondaryAccent = others[0],
        tertiaryAccent = others[2],
        dark = true,
        background = Color(0xFF0B0F12),
        surface = Color(0xFF141A1E),
        surfaceVariant = Color(0xFF1D252B),
        onBackground = Color(0xFFE3ECEF)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = NeonShapes,
        typography = NeonTypography,
        content = content
    )
}
