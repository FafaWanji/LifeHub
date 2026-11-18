package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Pastel Accents
val BlushPink = Color(0xFFF48FB1)
val Lavender = Color(0xFFCE93D8)
val MistBlue = Color(0xFF90CAF9)
val SoftMint = Color(0xFF80CBC4)
val PeachCream = Color(0xFFFFCC80)

val PastelAccents = listOf(BlushPink, Lavender, MistBlue, SoftMint, PeachCream)

// Controls 10, cards 14, sheets and panels 18 to 24: rounded, but not everything is a pill
private val PastelShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Hierarchy from size and colour, not weight: titles 500, body 400, nothing bolder than 600
private val base = Typography()
internal val AppTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Normal),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Normal),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium)
)

/** Graphite neutrals: grey surfaces, so the accent and the calendar's colours carry the colour. */
private val Graphite = Color(0xFFA1A1A6)

/** Darkens [c] until text in it reads on [on] (4.5:1), so light accents like Lemon stay legible. */
private fun readable(c: Color, on: Color): Color {
    fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }
    var out = c
    var step = 0
    while (contrast(out, on) < 4.5f && step < 20) { out = lerp(c, Color.Black, 0.05f * ++step) }
    return out
}

@Composable
fun PastelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Int?,
    content: @Composable () -> Unit
) {
    val accent = accentColor?.let { Color(it) } ?: MistBlue
    // Surfaces: blue-tinted (the earlier look) or neutral graphite, picked in the settings
    val blue = com.example.lifeorganizer.core.settings.rememberMicro(com.example.lifeorganizer.core.settings.MicroSettings.PASTEL_BACKGROUND) != "graphite"
    // The chrome stays neutral (graphite selections and chips); the accent marks actions, amber marks deadlines
    val colorScheme = if (darkTheme) {
        accentScheme(
            accent = accent,
            secondaryAccent = Graphite,
            tertiaryAccent = PeachCream,
            dark = true,
            background = if (blue) Color(0xFF17181D) else Color(0xFF141415),
            surface = if (blue) Color(0xFF21232A) else Color(0xFF1C1C1E),
            surfaceVariant = if (blue) Color(0xFF2C2F38) else Color(0xFF262628),
            onBackground = if (blue) Color(0xFFF1F2F6) else Color(0xFFEDEDED)
        )
    } else {
        accentScheme(
            accent = accent,
            secondaryAccent = Graphite,
            tertiaryAccent = PeachCream,
            dark = false,
            background = if (blue) Color(0xFFFAFAFC) else Color(0xFFFAFAFA),
            surface = if (blue) Color(0xFFF2F3F7) else Color(0xFFF4F4F5),
            surfaceVariant = if (blue) Color(0xFFE6E8EE) else Color(0xFFEAEAEC),
            onBackground = if (blue) Color(0xFF1B1D24) else Color(0xFF171717)
        ).let { it.copy(primary = readable(it.primary, Color.White), tertiary = readable(it.tertiary, Color.White)) }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = PastelShapes,
        typography = AppTypography,
        content = content
    )
}
