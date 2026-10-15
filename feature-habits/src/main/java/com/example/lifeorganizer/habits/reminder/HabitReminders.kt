package com.example.lifeorganizer.habits.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.example.lifeorganizer.habits.R
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.HabitCheck
import com.example.lifeorganizer.habits.data.HabitDatabase
import com.example.lifeorganizer.habits.data.TaskItem
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
 * Habit reminders (daily at the chosen time on due days) and task reminders (morning of the deadline).
 * Only the next alarm of each item is set; the receiver sets the following one when it fires.
 */
object HabitReminders {
    const val ACTION_OPEN_HABITS = "com.example.lifeorganizer.ACTION_HABITS"
    private const val ACTION_HABIT = "com.example.lifeorganizer.habits.REMIND_HABIT"
    private const val ACTION_TASK = "com.example.lifeorganizer.habits.REMIND_TASK"
    private const val ACTION_HABIT_DONE = "com.example.lifeorganizer.habits.HABIT_DONE"
    private const val ACTION_TASK_DONE = "com.example.lifeorganizer.habits.TASK_DONE"
    private const val CHANNEL_ID = "habits"
    /** Task reminders come on the morning of the deadline. */
    private const val TASK_REMINDER_HOUR = 9

    private fun alarms(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // The item id goes into the data URI, so every habit/task has its own PendingIntent.
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
        nextHabitTrigger(habit, doneToday)?.let { setAlarm(context, it, op) }
    }

    fun cancelHabit(context: Context, habitId: Long) {
        alarms(context).cancel(pending(context, ACTION_HABIT, "habit", habitId))
        NotificationManagerCompatSafe.cancel(context, notificationId("habit", habitId))
    }

    fun scheduleTask(context: Context, task: TaskItem) {
        val op = pending(context, ACTION_TASK, "task", task.id)
        alarms(context).cancel(op)
        val due = task.dueEpochDay ?: return
        if (task.doneAt != null) return
        val at = LocalDate.ofEpochDay(due).atTime(TASK_REMINDER_HOUR, 0)
        if (at.isAfter(LocalDateTime.now())) setAlarm(context, at, op)
    }

    fun cancelTask(context: Context, taskId: Long) {
        alarms(context).cancel(pending(context, ACTION_TASK, "task", taskId))
        NotificationManagerCompatSafe.cancel(context, notificationId("task", taskId))
    }

    /** After a reboot, an app update or a backup import. */
    suspend fun scheduleAll(context: Context) {
        val dao = HabitDatabase.getDatabase(context).habitDao()
        dao.habitsSync().forEach { scheduleHabit(context, it) }
        dao.tasksSync().forEach { scheduleTask(context, it) }
    }

    private fun ensureChannel(context: Context, lang: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, HabitsStr.channel.of(lang), NotificationManager.IMPORTANCE_HIGH))
        }
    }

    private fun notify(context: Context, lang: String, kind: String, id: Long, title: String, text: String, doneAction: String) {
        ensureChannel(context, lang)
        val nid = notificationId(kind, id)
        val open = PendingIntent.getActivity(
            context, nid,
            Intent(context, Class.forName("com.example.lifeorganizer.MainActivity"))
                .setAction(ACTION_OPEN_HABITS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_habit_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(R.drawable.ic_habit_notification, HabitsStr.markDone.of(lang), pending(context, doneAction, kind, id))
            .build()
        NotificationManagerCompatSafe.notify(context, nid, notification)
    }

    internal suspend fun handle(context: Context, intent: Intent) {
        val app = context.applicationContext
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scheduleAll(app); return
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
                    notify(app, lang, "habit", id, habit.title, HabitsStr.habitReminderText.of(lang), ACTION_HABIT_DONE)
                }
                nextHabitTrigger(habit, doneToday = true)?.let { setAlarm(app, it, pending(app, ACTION_HABIT, "habit", id)) }
            }
            ACTION_TASK -> {
                val task = dao.taskById(id) ?: return
                if (moduleOn && task.doneAt == null) notify(app, lang, "task", id, task.title, HabitsStr.taskReminderText.of(lang), ACTION_TASK_DONE)
            }
            ACTION_HABIT_DONE -> {
                dao.insertCheck(HabitCheck(id, today.toEpochDay()))
                NotificationManagerCompatSafe.cancel(app, notificationId("habit", id))
            }
            ACTION_TASK_DONE -> {
                dao.taskById(id)?.let { dao.updateTask(it.copy(doneAt = System.currentTimeMillis())) }
                NotificationManagerCompatSafe.cancel(app, notificationId("task", id))
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
