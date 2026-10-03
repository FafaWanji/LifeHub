package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Pastel Accents
val BlushPink = Color(0xFFF48FB1)
val Lavender = Color(0xFFCE93D8)
val MistBlue = Color(0xFF90CAF9)
val SoftMint = Color(0xFF80CBC4)
val PeachCream = Color(0xFFFFCC80)

val PastelAccents = listOf(BlushPink, Lavender, MistBlue, SoftMint, PeachCream)

private val PastelShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val base = Typography()
internal val AppTypography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun PastelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Int?,
    content: @Composable () -> Unit
) {
    val accent = accentColor?.let { Color(it) } ?: MistBlue
    // Pick companion accents that differ from the chosen one so secondary/tertiary stay distinct.
    val others = PastelAccents.filter { it != accent }
    val colorScheme = if (darkTheme) {
        accentScheme(
            accent = accent,
            secondaryAccent = others[1],
            tertiaryAccent = others[2],
            dark = true,
            background = Color(0xFF17181D),
            surface = Color(0xFF21232A),
            surfaceVariant = Color(0xFF2C2F38),
            onBackground = Color(0xFFF1F2F6)
        )
    } else {
        accentScheme(
            accent = accent,
            secondaryAccent = others[1],
            tertiaryAccent = others[2],
            dark = false,
            background = Color(0xFFFAFAFC),
            surface = Color(0xFFF2F3F7),
            surfaceVariant = Color(0xFFE6E8EE),
            onBackground = Color(0xFF1B1D24)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = PastelShapes,
        typography = AppTypography,
        content = content
    )
}
