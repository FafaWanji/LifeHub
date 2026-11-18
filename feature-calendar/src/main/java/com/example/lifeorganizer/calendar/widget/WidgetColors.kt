package com.example.lifeorganizer.calendar.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.lerp
import androidx.glance.color.ColorProvider
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.core.theme.PastelBackground
import com.example.lifeorganizer.core.theme.PastelBackgrounds

/**
 * Widget colours from the background chosen in the app (Pastel → Hintergrund), each with a light and a dark tone.
 * Accent and error stay the widget_* resources. Used by the calendar widget and the app's widgets.
 */
class WidgetColors private constructor(bg: PastelBackground) {
    val surface = ColorProvider(day = bg.light.surface, night = bg.dark.surface)
    val onSurface = ColorProvider(day = bg.light.text, night = bg.dark.text)
    val muted = ColorProvider(day = lerp(bg.light.text, bg.light.background, 0.35f), night = lerp(bg.dark.text, bg.dark.background, 0.35f))
    val chip = ColorProvider(day = bg.light.variant, night = bg.dark.variant)

    companion object {
        // shortcut: reads the Pastel background even when another design is active (the design is in DataStore, which
        // a widget cannot read synchronously); only matters for someone who picked a tint and then switched design
        fun of(context: Context): WidgetColors = forId(MicroSettings.get(context, MicroSettings.PASTEL_BACKGROUND))

        fun forId(id: String): WidgetColors =
            WidgetColors(PastelBackgrounds.firstOrNull { it.id == id } ?: PastelBackgrounds.first { it.id == "blue" })
    }
}

/**
 * Widget colours that follow the setting while the widget is shown: an update request alone does not
 * redraw a widget whose content has not changed, so the colours have to be state.
 */
@androidx.compose.runtime.Composable
fun rememberWidgetColors(): WidgetColors {
    val context = androidx.glance.LocalContext.current
    val id by androidx.compose.runtime.remember { MicroSettings.flow(context, MicroSettings.PASTEL_BACKGROUND) }
        .collectAsState(MicroSettings.get(context, MicroSettings.PASTEL_BACKGROUND))
    return androidx.compose.runtime.remember(id) { WidgetColors.forId(id) }
}
