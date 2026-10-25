package com.example.lifeorganizer.core.theme

/**
 * Accent colours to pick from (setup and settings), ARGB. The first one is the design's own default:
 * picking it stores no accent at all.
 */
object AccentPalette {
    val pastel: List<Int> = listOf(
        0xFF90CAF9, // Mist Blue (default)
        0xFFF48FB1, // Blush Pink
        0xFFCE93D8, // Lavender
        0xFF80CBC4, // Soft Mint
        0xFFFFCC80, // Peach Cream
        0xFFFFAB91, // Coral
        0xFFEF9A9A, // Rose
        0xFFFFF59D, // Lemon
        0xFFC5E1A5, // Sage
        0xFF80DEEA, // Aqua
        0xFF9FA8DA, // Periwinkle
        0xFFB39DDB  // Lilac
    ).map { it.toInt() }

    val neon: List<Int> = listOf(
        0xFF00FFB2, // Electric Mint (default)
        0xFF00E5FF, // Neon Cyan
        0xFFD4FF00, // Acid Lime
        0xFFD500F9, // Plasma Purple
        0xFFFF007F, // Hot Magenta
        0xFFFF6D00, // Blaze Orange
        0xFFFFEA00, // Volt Yellow
        0xFF76FF03, // Toxic Green
        0xFF2979FF, // Laser Blue
        0xFF651FFF, // Ultra Violet
        0xFFFF1744  // Signal Red
    ).map { it.toInt() }

    /** Colours for a design, null when it has no accent choice. */
    fun forStyle(style: String): List<Int>? = when (style) {
        "pastel" -> pastel
        "neon" -> neon
        else -> null
    }
}
