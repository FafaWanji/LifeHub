package com.example.lifeorganizer.waypoints

import android.app.Application
import com.google.android.libraries.places.api.Places

class WaypointsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the SDK
        if (!Places.isInitialized()) {
            mapsApiKey()?.let { Places.initialize(applicationContext, it) }
        }
    }

    /** The key comes from the manifest placeholder (filled from local.properties at build time). */
    private fun mapsApiKey(): String? = runCatching {
        packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
            .metaData?.getString("com.google.android.geo.API_KEY")
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
