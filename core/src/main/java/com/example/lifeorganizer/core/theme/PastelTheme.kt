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

/** Surface tones of one background: page, cards, raised parts, text. */
class Tones(val background: Color, val surface: Color, val variant: Color, val text: Color)

/** A Pastel background. Tints stay low so the accent and the calendar's colours still lead. */
class PastelBackground(val id: String, val name: com.example.lifeorganizer.core.i18n.Txt, val dark: Tones, val light: Tones) {
    fun tones(dark: Boolean) = if (dark) this.dark else light
}

private fun tones(bg: Long, surface: Long, variant: Long, text: Long) = Tones(Color(bg), Color(surface), Color(variant), Color(text))
private fun txt(de: String) = com.example.lifeorganizer.core.i18n.Txt(de)

/** The choices in the settings; the id is what is stored ([com.example.lifeorganizer.core.settings.MicroSettings.PASTEL_BACKGROUND]). */
val PastelBackgrounds = listOf(
    PastelBackground("graphite", txt("Graphit"), tones(0xFF141415, 0xFF1C1C1E, 0xFF262628, 0xFFEDEDED), tones(0xFFFAFAFA, 0xFFF4F4F5, 0xFFEAEAEC, 0xFF171717)),
    PastelBackground("blue", txt("Blau"), tones(0xFF17181D, 0xFF21232A, 0xFF2C2F38, 0xFFF1F2F6), tones(0xFFFAFAFC, 0xFFF2F3F7, 0xFFE6E8EE, 0xFF1B1D24)),
    PastelBackground("warm", txt("Warm"), tones(0xFF1A1714, 0xFF25211C, 0xFF302A24, 0xFFF2EEE8), tones(0xFFFBFAF7, 0xFFF4F1EB, 0xFFE9E4DA, 0xFF1F1B16)),
    PastelBackground("green", txt("Grün"), tones(0xFF141A17, 0xFF1D2520, 0xFF27312B, 0xFFEEF3EF), tones(0xFFF9FBFA, 0xFFF1F5F2, 0xFFE3EBE5, 0xFF18201B)),
    PastelBackground("violet", txt("Lila"), tones(0xFF1C1624, 0xFF282032, 0xFF352A42, 0xFFF3EFF8), tones(0xFFFBF9FE, 0xFFF4F0FA, 0xFFE9E2F4, 0xFF1E1A26)),
    PastelBackground("rose", txt("Rosa"), tones(0xFF1C1619, 0xFF272024, 0xFF332A2F, 0xFFF5EFF1), tones(0xFFFCFAFB, 0xFFF6F1F3, 0xFFECE4E8, 0xFF221B1E))
)

@Composable
fun PastelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Int?,
    content: @Composable () -> Unit
) {
    val accent = accentColor?.let { Color(it) } ?: MistBlue
    // Surfaces picked in the settings; an unknown id (e.g. from a newer backup) falls back to blue
    val chosen = com.example.lifeorganizer.core.settings.rememberMicro(com.example.lifeorganizer.core.settings.MicroSettings.PASTEL_BACKGROUND)
    val t = (PastelBackgrounds.firstOrNull { it.id == chosen } ?: PastelBackgrounds.first { it.id == "blue" }).tones(darkTheme)
    // The chrome stays neutral (graphite selections and chips); the accent marks actions, amber marks deadlines
    val colorScheme = if (darkTheme) {
        accentScheme(
            accent = accent,
            secondaryAccent = Graphite,
            tertiaryAccent = PeachCream,
            dark = true,
            background = t.background,
            surface = t.surface,
            surfaceVariant = t.variant,
            onBackground = t.text
        )
    } else {
        accentScheme(
            accent = accent,
            secondaryAccent = Graphite,
            tertiaryAccent = PeachCream,
            dark = false,
            background = t.background,
            surface = t.surface,
            surfaceVariant = t.variant,
            onBackground = t.text
        ).let { it.copy(primary = readable(it.primary, Color.White), tertiary = readable(it.tertiary, Color.White)) }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = PastelShapes,
        typography = AppTypography,
        content = content
    )
}
