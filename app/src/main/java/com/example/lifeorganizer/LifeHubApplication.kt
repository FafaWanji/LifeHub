package com.example.lifeorganizer

import com.example.lifeorganizer.calendar.debug.DebugLogger
import com.example.lifeorganizer.dev.CrashLog
import com.example.lifeorganizer.dev.DevMode
import com.example.lifeorganizer.waypoints.WaypointsApplication

class LifeHubApplication : WaypointsApplication() {
    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
        DevMode.load(this)
        CrashLog.install(this)
    }
}
