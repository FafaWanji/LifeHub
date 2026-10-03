package com.example.lifeorganizer.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.lifeorganizer.MainActivity
import com.example.lifeorganizer.R

/**
 * Smart Entry widget: a search-bar-like pill with a text area, a microphone and a camera button.
 * Each part opens Smart Add in a different mode (type, voice, scan).
 */
class SmartAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SmartEntry(context) }
    }

    @Composable
    private fun SmartEntry(context: Context) {
        val surface = ColorProvider(R.color.widget_surface)
        val accent = ColorProvider(R.color.widget_accent)
        val muted = ColorProvider(R.color.widget_muted)

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .cornerRadius(28.dp)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(launch(context, MainActivity.ACTION_SMART_ADD)),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_sparkle),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(accent),
                    modifier = GlanceModifier.size(20.dp)
                )
                Spacer(GlanceModifier.width(10.dp))
                Text(
                    text = context.getString(R.string.smart_entry_hint),
                    maxLines = 1,
                    style = TextStyle(color = muted, fontSize = 15.sp)
                )
            }
            WidgetIconButton(
                icon = R.drawable.ic_widget_mic,
                description = "Voice",
                tint = accent,
                action = launch(context, MainActivity.ACTION_SMART_ADD_VOICE)
            )
            WidgetIconButton(
                icon = R.drawable.ic_widget_camera,
                description = "Camera",
                tint = accent,
                action = launch(context, MainActivity.ACTION_SMART_ADD_SCAN)
            )
        }
    }

    @Composable
    private fun WidgetIconButton(icon: Int, description: String, tint: androidx.glance.unit.ColorProvider, action: Action) {
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .cornerRadius(22.dp)
                .clickable(action),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = description,
                colorFilter = ColorFilter.tint(tint),
                modifier = GlanceModifier.size(22.dp)
            )
        }
    }

    // A distinct action per button keeps the PendingIntents separate.
    private fun launch(context: Context, action: String): Action =
        actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
}

class SmartAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartAddWidget()
}
