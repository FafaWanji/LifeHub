package com.example.lifeorganizer.habits.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lifeorganizer.core.settings.AppModule
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.core.settings.enabledModules
import com.example.lifeorganizer.core.settings.NotificationChannels
import com.example.lifeorganizer.habits.R
import com.example.lifeorganizer.core.i18n.durationLabel
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.HabitCheck
import com.example.lifeorganizer.habits.data.HabitDatabase
import com.example.lifeorganizer.habits.domain.HabitSchedule
import com.example.lifeorganizer.habits.ui.HabitsStr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Habit reminders, daily at the chosen time on due days. Only the next alarm of each habit is set; the receiver
 * sets the following one when it fires. Everything is re-armed after a reboot or an app update.
 */
object HabitReminders {
    const val ACTION_OPEN_HABITS = "com.example.lifeorganizer.ACTION_HABITS"
    private const val ACTION_HABIT = "com.example.lifeorganizer.habits.REMIND_HABIT"
    private const val ACTION_HABIT_DONE = "com.example.lifeorganizer.habits.HABIT_DONE"
    private const val ACTION_HABIT_SNOOZE = "com.example.lifeorganizer.habits.HABIT_SNOOZE"

    private fun alarms(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // The habit id goes into the data URI, so every habit has its own PendingIntent.
    private fun intent(context: Context, action: String, kind: String, id: Long) =
        Intent(context, HabitReminderReceiver::class.java).setAction(action).setData(Uri.parse("lifehub-$kind://$id"))

    private fun pending(context: Context, action: String, kind: String, id: Long) = PendingIntent.getBroadcast(
        context, 0, intent(context, action, kind, id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun notificationId(kind: String, id: Long) = (if (kind == "habit") 1_500_000_000 else 1_600_000_000) + (id % 100_000_000).toInt()

    /** Next due day at the reminder time that is still ahead (today is skipped once it is done). */
    fun nextHabitTrigger(habit: Habit, doneToday: Boolean, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val minutes = habit.reminderMinutes ?: return null
        for (d in 0L..7L) {
            val date = now.toLocalDate().plusDays(d)
            if (!HabitSchedule.isDue(habit.weekdays, date) || (d == 0L && doneToday)) continue
            val at = date.atTime(minutes / 60, minutes % 60)
            if (at.isAfter(now)) return at
        }
        return null
    }

    private fun setAlarm(context: Context, at: LocalDateTime, operation: PendingIntent) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = alarms(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
    }

    suspend fun scheduleHabit(context: Context, habit: Habit) {
        val op = pending(context, ACTION_HABIT, "habit", habit.id)
        alarms(context).cancel(op)
        val dao = HabitDatabase.getDatabase(context).habitDao()
        val today = LocalDate.now().toEpochDay()
        val doneToday = dao.checksSync().any { it.habitId == habit.id && it.epochDay == today }
        // Ticked off in the app or widget: today's reminder is no longer needed
        if (doneToday) NotificationManagerCompatSafe.cancel(context, notificationId("habit", habit.id))
        nextHabitTrigger(habit, doneToday)?.let { setAlarm(context, it, op) }
    }

    fun cancelHabit(context: Context, habitId: Long) {
        alarms(context).cancel(pending(context, ACTION_HABIT, "habit", habitId))
        NotificationManagerCompatSafe.cancel(context, notificationId("habit", habitId))
    }

    /** Whether reminders can come on time: notifications allowed and exact alarms possible. */
    fun notificationsAllowed(context: Context): Boolean =
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms(context).canScheduleExactAlarms()

    /** After a reboot, an app update, a backup import or when exact alarms were allowed. */
    suspend fun scheduleAll(context: Context) {
        val dao = HabitDatabase.getDatabase(context).habitDao()
        dao.habitsSync().forEach { scheduleHabit(context, it) }
    }

    /** Habits have their own channel, so sound and vibration can be set apart (see the notification settings). */
    private fun notify(context: Context, lang: String, kind: String, id: Long, title: String, text: String) {
        NotificationChannels.ensureHabitsAndTasks(context, lang)
        val nid = notificationId(kind, id)
        val snoozeMinutes = MicroSettings.get(context, MicroSettings.SNOOZE_MINUTES).coerceAtLeast(1)
        val open = PendingIntent.getActivity(
            context, nid,
            Intent(context, Class.forName("com.example.lifeorganizer.MainActivity"))
                .setAction(ACTION_OPEN_HABITS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.HABITS)
            .setSmallIcon(R.drawable.ic_habit_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(R.drawable.ic_habit_notification, HabitsStr.markDone.of(lang), pending(context, ACTION_HABIT_DONE, kind, id))
            .addAction(
                R.drawable.ic_habit_notification, HabitsStr.snoozeFor.of(lang).format(durationLabel(snoozeMinutes, lang)),
                pending(context, ACTION_HABIT_SNOOZE, kind, id)
            )
            .build()
        NotificationManagerCompatSafe.notify(context, nid, notification)
    }

    internal suspend fun handle(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> { scheduleAll(app); return }
            // Exact alarms were just allowed: re-arm so pushes come on the minute
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> { scheduleAll(app); return }
        }
        val id = intent.data?.host?.toLongOrNull() ?: return
        val dao = HabitDatabase.getDatabase(app).habitDao()
        val settings = SettingsManager(app)
        val lang = settings.languageCode.first()
        val moduleOn = AppModule.HABITS in settings.enabledModules.first()
        val today = LocalDate.now()
        when (intent.action) {
            ACTION_HABIT -> {
                val habit = dao.habitById(id) ?: return
                val done = dao.checksSync().any { it.habitId == id && it.epochDay == today.toEpochDay() }
                // Switched off: stay quiet, but keep the chain so reminders resume when it is on again
                if (moduleOn && !done && HabitSchedule.isDue(habit.weekdays, today)) {
                    notify(app, lang, "habit", id, habit.title, HabitsStr.habitReminderText.of(lang))
                }
                nextHabitTrigger(habit, doneToday = true)?.let { setAlarm(app, it, pending(app, ACTION_HABIT, "habit", id)) }
            }
            ACTION_HABIT_DONE -> {
                dao.insertCheck(HabitCheck(id, today.toEpochDay()))
                NotificationManagerCompatSafe.cancel(app, notificationId("habit", id))
                com.example.lifeorganizer.habits.HabitsIntegration.changed(app)
            }
            ACTION_HABIT_SNOOZE -> {
                NotificationManagerCompatSafe.cancel(app, notificationId("habit", id))
                // The habit's alarm comes back after the snooze time; when it fires it plans the next day again
                setAlarm(app, LocalDateTime.now().plusMinutes(MicroSettings.get(app, MicroSettings.SNOOZE_MINUTES).coerceAtLeast(1).toLong()), pending(app, ACTION_HABIT, "habit", id))
            }
        }
    }
}

/** Posting needs the notification permission on Android 13+; without it there is simply nothing to show. */
private object NotificationManagerCompatSafe {
    fun notify(context: Context, id: Int, notification: android.app.Notification) {
        runCatching { androidx.core.app.NotificationManagerCompat.from(context).notify(id, notification) }
    }

    fun cancel(context: Context, id: Int) = androidx.core.app.NotificationManagerCompat.from(context).cancel(id)
}

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HabitReminders.handle(context, intent)
            } finally {
                result.finish()
            }
        }
    }
}
