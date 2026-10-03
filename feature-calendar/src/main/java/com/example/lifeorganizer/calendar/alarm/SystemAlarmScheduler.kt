package com.example.lifeorganizer.calendar.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.lifeorganizer.calendar.debug.DebugLogger

object SystemAlarmScheduler {
    fun createSystemAlarm(context: Context, hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
            DebugLogger.log("System Alarm Intent sent: $hour:$minute - $label")
        } catch (e: Exception) {
            DebugLogger.log("Error creating System Alarm: ${e.message}")
        }
    }
}
