package com.example.lifeorganizer.waypoints

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.lifeorganizer.waypoints.data.WaypointEntity

object ShortcutHelper {
    fun updateShortcuts(context: Context, pinnedWaypoints: List<WaypointEntity>, transportMode: String) {
        val shortcuts = pinnedWaypoints.map { waypoint ->
            val travelModeStr = when (transportMode) {
                "car" -> "driving"
                "bike" -> "bicycling"
                "transit" -> "transit"
                else -> "driving"
            }
            
            val uriStr = "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(waypoint.address)}&travelmode=$travelModeStr"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                setPackage("com.google.android.apps.maps")
                action = Intent.ACTION_VIEW
            }

            ShortcutInfoCompat.Builder(context, "waypoint_${waypoint.id}")
                .setShortLabel(waypoint.name)
                .setLongLabel("Navigate to ${waypoint.name}")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
                .setIntent(intent)
                .build()
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
