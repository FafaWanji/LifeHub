package com.example.lifeorganizer.calendar.alarm

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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DebugLogger.log("Boot completed — rescheduling alarms")
            CoroutineScope(Dispatchers.IO).launch {
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
            }
        }
    }
}
