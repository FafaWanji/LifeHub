package com.example.lifeorganizer.waypoints.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.lifeorganizer.waypoints.data.WaypointWithLabels
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import android.graphics.Color

@Composable
fun MapContent(
    waypoints: List<WaypointWithLabels>,
    modifier: Modifier = Modifier
) {
    // Center map on the first waypoint, or a default location if empty
    val defaultLocation = remember(waypoints) {
        val firstWithCoords = waypoints.firstOrNull { it.waypoint.lat != null && it.waypoint.lng != null }
        if (firstWithCoords != null) {
            LatLng(firstWithCoords.waypoint.lat!!, firstWithCoords.waypoint.lng!!)
        } else {
            LatLng(0.0, 0.0) // Default center (Equator/Prime Meridian)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        waypoints.forEach { waypointWithLabels ->
            val waypoint = waypointWithLabels.waypoint
            if (waypoint.lat != null && waypoint.lng != null) {
                var markerHue = BitmapDescriptorFactory.HUE_RED
                val hexStr = waypointWithLabels.labels.firstOrNull()?.colorHex
                if (hexStr != null) {
                    try {
                        val colorInt = Color.parseColor(hexStr)
                        val hsv = FloatArray(3)
                        Color.colorToHSV(colorInt, hsv)
                        markerHue = hsv[0]
                    } catch (e: Exception) {
                        // fallback to red
                    }
                }

                Marker(
                    state = MarkerState(position = LatLng(waypoint.lat, waypoint.lng)),
                    title = waypoint.name,
                    snippet = waypoint.address,
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue)
                )
            }
        }
    }
}
