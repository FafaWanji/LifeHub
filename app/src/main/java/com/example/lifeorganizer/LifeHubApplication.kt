package com.example.lifeorganizer

import com.example.lifeorganizer.waypoints.WaypointsApplication

class LifeHubApplication : WaypointsApplication() {
    override fun onCreate() {
        super.onCreate()
        // Ticking things off in the app or a notification updates the home-screen widget
        com.example.lifeorganizer.habits.HabitsIntegration.onChanged = { com.example.lifeorganizer.widgets.HabitsWidget.refresh(it) }
    }
}
