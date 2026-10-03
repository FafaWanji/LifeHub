package com.example.lifeorganizer.calendar.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lifeorganizer.calendar.data.SettingsManager
import com.example.lifeorganizer.calendar.debug.DebugLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.Year
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val SNOOZE_REQUEST_OFFSET = 900_000
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_SNOOZE -> {
                val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)
                val title = intent.getStringExtra("EVENT_TITLE") ?: "Calendar Event"
                val message = intent.getStringExtra("REMINDER_TYPE") ?: "Reminder"
                val notificationHelper = NotificationHelper(context)
                notificationHelper.cancelNotification(notificationId)
                // Fire the same reminder again in 10 minutes
                val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L)
                val reminderIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("EVENT_TITLE", title)
                    putExtra("REMINDER_TYPE", message)
                }
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    SNOOZE_REQUEST_OFFSET + notificationId,
                    reminderIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                }
                DebugLogger.log("Snoozed notification: $title for 10 minutes")
                return
            }
            NotificationHelper.ACTION_DISMISS -> {
                val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)
                val notificationHelper = NotificationHelper(context)
                notificationHelper.cancelNotification(notificationId)
                DebugLogger.log("Dismissed notification ID: $notificationId")
                return
            }
            else -> {
                val title = intent.getStringExtra("EVENT_TITLE") ?: "Calendar Event"
                val message = intent.getStringExtra("REMINDER_TYPE") ?: "Reminder"
                val isSystemAlarmTrigger = intent.getBooleanExtra("IS_SYSTEM_ALARM_TRIGGER", false)
                val isBirthday = intent.getBooleanExtra("IS_BIRTHDAY", false)
                val birthYear = intent.getIntExtra("BIRTH_YEAR", -1)

                if (isSystemAlarmTrigger) {
                    val reminderTimeMillis = intent.getLongExtra("REMINDER_TIME", 0L)
                    if (reminderTimeMillis > 0) {
                        val reminderDateTime = Instant.ofEpochMilli(reminderTimeMillis)
                            .atZone(ZoneId.systemDefault())
                        SystemAlarmScheduler.createSystemAlarm(
                            context = context,
                            hour = reminderDateTime.hour,
                            minute = reminderDateTime.minute,
                            label = "$title ($message)"
                        )
                        DebugLogger.log("Deferred System Alarm set for $title")
                    }
                    return
                }

                DebugLogger.log("Alarm triggered: $title ($message)")

                val notificationHelper = NotificationHelper(context)
                val birthdayMessage = if (isBirthday && birthYear > 0) {
                    val currentYear = Year.now().value
                    val age = currentYear - birthYear
                    "🎂 $title turns $age tomorrow!"
                } else if (isBirthday) {
                    "🎂 $title's birthday is tomorrow!"
                } else null

                val soundUri = runBlocking {
                    SettingsManager(context.applicationContext).notificationSoundUri.first()
                }

                // A repeating event's reminder just fired: arm it for the next occurrence.
                val pending = goAsync()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try { RecurringAlarmSync.sync(context.applicationContext) } finally { pending.finish() }
                }

                if (isBirthday) {
                    notificationHelper.showBirthdayNotification(
                        title = "🎂 Birthday Tomorrow",
                        message = birthdayMessage ?: message
                    )
                } else {
                    notificationHelper.showNotification(
                        title = title,
                        message = birthdayMessage ?: message,
                        soundUri = soundUri,
                        linkedNoteId = intent.getLongExtra(NotificationHelper.EXTRA_NOTE_ID, -1L).takeIf { it > 0 }
                    )
                }
            }
        }
    }
}
