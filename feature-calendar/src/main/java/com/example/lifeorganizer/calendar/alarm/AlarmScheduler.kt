package com.example.lifeorganizer.calendar.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.Reminder
import com.example.lifeorganizer.calendar.debug.DebugLogger

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun schedule(event: Event, reminder: Reminder, isSystemAlarmTrigger: Boolean = false) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EVENT_TITLE", event.title)
            putExtra("REMINDER_TYPE", reminder.type)
            putExtra("IS_SYSTEM_ALARM_TRIGGER", isSystemAlarmTrigger)
            putExtra("REMINDER_TIME", reminder.reminderTimeMillis)
            putExtra("IS_BIRTHDAY", event.isBirthday)
            putExtra("BIRTH_YEAR", event.birthYear ?: -1)
            event.linkedNoteId?.let { putExtra(NotificationHelper.EXTRA_NOTE_ID, it) }
        }

        // Use unique request code. If it's a system alarm trigger, offset the ID to avoid collision with notification alarm.
        val requestCode = if (isSystemAlarmTrigger) reminder.id.toInt() + 500000 else reminder.id.toInt()
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = if (isSystemAlarmTrigger) {
            // Trigger 20 hours before the actual reminder
            reminder.reminderTimeMillis - (20 * 60 * 60 * 1000L)
        } else {
            reminder.reminderTimeMillis
        }

        // Never schedule in the past: Android would fire it immediately (e.g. when editing an old
        // event). Repeating events get their next occurrence armed by RecurringAlarmSync.
        if (triggerTime <= System.currentTimeMillis()) return

        val timeString = dateFormat.format(Date(triggerTime))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                if (isSystemAlarmTrigger) {
                    // Deferred trigger doesn't need to show the alarm icon yet
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    DebugLogger.log("Deferred System Alarm trigger scheduled at $timeString")
                } else {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, null)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    DebugLogger.log("Scheduled: ${event.title} (${reminder.type}) at $timeString")
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancel(reminder: Reminder, isSystemAlarmTrigger: Boolean = false) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = if (isSystemAlarmTrigger) reminder.id.toInt() + 500000 else reminder.id.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        DebugLogger.log("Cancelled: Reminder ID:${reminder.id} (systemTrigger=$isSystemAlarmTrigger)")
    }

    fun cancelAll(reminder: Reminder) {
        cancel(reminder, isSystemAlarmTrigger = false)
        cancel(reminder, isSystemAlarmTrigger = true)
    }
}
