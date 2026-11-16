package com.example.lifeorganizer.calendar.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.debug.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // After a reboot alarms are gone; after the exact-alarm permission is granted the inexact ones are re-armed exactly
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        DebugLogger.log("${intent.action} — rescheduling alarms")
        // Keeps the process alive until every alarm is armed again
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val events = db.eventDao().getEventsWithRemindersSync()
                val alarmScheduler = AlarmScheduler(context)
                val currentTime = System.currentTimeMillis()

                events.forEach { eventWithReminders ->
                    eventWithReminders.reminders.forEach { reminder ->
                        if (reminder.reminderTimeMillis > currentTime) {
                            alarmScheduler.schedule(eventWithReminders.event, reminder)
                            if (reminder.type == "Birthday at 23:59") {
                                DebugLogger.log("Rescheduled (silent): ${eventWithReminders.event.title} (${reminder.type})")
                            } else {
                                DebugLogger.log("Rescheduled: ${eventWithReminders.event.title} (${reminder.type})")
                            }
                        }
                    }
                }
                RecurringAlarmSync.sync(context)
                DefaultReminders.sync(context)
            } finally {
                pending.finish()
            }
        }
    }
}
