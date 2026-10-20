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
import com.example.lifeorganizer.habits.domain.TaskDeadline
import com.example.lifeorganizer.habits.ui.HabitsStr
import com.example.lifeorganizer.habits.ui.deadlineLabel
import com.example.lifeorganizer.habits.ui.offsetLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Habit reminders (daily at the chosen time on due days) and task reminders (the chosen times before the deadline
 * plus one at the deadline itself). Only the next alarm of each item is set; the receiver sets the following one
 * when it fires. Everything is re-armed after a reboot or an app update.
 */
object HabitReminders {
    const val ACTION_OPEN_HABITS = "com.example.lifeorganizer.ACTION_HABITS"
    private const val ACTION_HABIT = "com.example.lifeorganizer.habits.REMIND_HABIT"
    private const val ACTION_TASK = "com.example.lifeorganizer.habits.REMIND_TASK"
    private const val ACTION_HABIT_DONE = "com.example.lifeorganizer.habits.HABIT_DONE"
    private const val ACTION_TASK_DONE = "com.example.lifeorganizer.habits.TASK_DONE"
    private const val CHANNEL_ID = "habits"
    private const val EXTRA_OFFSET = "offset"

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
        // Ticked off in the app or widget: today's reminder is no longer needed
        if (doneToday) NotificationManagerCompatSafe.cancel(context, notificationId("habit", habit.id))
        nextHabitTrigger(habit, doneToday)?.let { setAlarm(context, it, op) }
    }

    fun cancelHabit(context: Context, habitId: Long) {
        alarms(context).cancel(pending(context, ACTION_HABIT, "habit", habitId))
        NotificationManagerCompatSafe.cancel(context, notificationId("habit", habitId))
    }

    /** Task alarm for the push [offset] minutes before the deadline (0 = at the deadline). */
    private fun taskPending(context: Context, id: Long, offset: Int) = PendingIntent.getBroadcast(
        context, 0, intent(context, ACTION_TASK, "task", id).putExtra(EXTRA_OFFSET, offset),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Arms the next push of an open task; done tasks lose their alarm and notification. */
    fun scheduleTask(context: Context, task: TaskItem, now: LocalDateTime = LocalDateTime.now()) {
        alarms(context).cancel(pending(context, ACTION_TASK, "task", task.id))
        if (task.doneAt != null) {
            NotificationManagerCompatSafe.cancel(context, notificationId("task", task.id))
            return
        }
        val deadline = TaskDeadline.deadline(task.dueEpochDay, task.dueMinute)
        if (deadline == null) {
            // Old task without a deadline: its fixed reminder time, if any
            val at = task.reminderAt?.let { TaskDeadline.toLocal(it) } ?: return
            if (at.isAfter(now)) setAlarm(context, at, taskPending(context, task.id, -1))
            return
        }
        val next = TaskDeadline.nextReminder(deadline, TaskDeadline.parseOffsets(task.reminderOffsets), now) ?: return
        setAlarm(context, next.at, taskPending(context, task.id, next.offsetMinutes))
    }

    /** Whether reminders can come on time: notifications allowed and exact alarms possible. */
    fun notificationsAllowed(context: Context): Boolean =
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms(context).canScheduleExactAlarms()

    fun cancelTask(context: Context, taskId: Long) {
        alarms(context).cancel(pending(context, ACTION_TASK, "task", taskId))
        NotificationManagerCompatSafe.cancel(context, notificationId("task", taskId))
    }

    /**
     * After a reboot, an app update, a backup import or when exact alarms were allowed. With [catchUp], a task
     * push that was due while the phone was off is sent now (only the latest one per task).
     */
    suspend fun scheduleAll(context: Context, catchUp: Boolean = false) {
        val dao = HabitDatabase.getDatabase(context).habitDao()
        dao.habitsSync().forEach { scheduleHabit(context, it) }
        val now = LocalDateTime.now()
        val settings = SettingsManager(context)
        val lang = settings.languageCode.first()
        val moduleOn = AppModule.HABITS in settings.enabledModules.first()
        dao.tasksSync().forEach { task ->
            val deadline = TaskDeadline.deadline(task.dueEpochDay, task.dueMinute)
            if (catchUp && moduleOn && task.doneAt == null && deadline != null) {
                val since = TaskDeadline.toLocal(task.lastReminderAt ?: task.createdAt)
                TaskDeadline.missedReminder(deadline, TaskDeadline.parseOffsets(task.reminderOffsets), since, now)?.let { missed ->
                    notify(context, lang, "task", task.id, task.title, taskReminderText(task, missed.offsetMinutes, now, lang), ACTION_TASK_DONE)
                    dao.setLastReminder(task.id, TaskDeadline.toMillis(now))
                }
            }
            scheduleTask(context, task, now)
        }
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

    /**
     * "Deadline in 1 h (today 14:00) – done in time = +10 points", "Deadline now (14:00)", "Deadline missed (…)"
     * or just "Reminder" for an old task without a deadline.
     */
    private fun taskReminderText(task: TaskItem, offset: Int, now: LocalDateTime, lang: String): String {
        val deadline = TaskDeadline.deadline(task.dueEpochDay, task.dueMinute) ?: return HabitsStr.reminder.of(lang)
        val at = deadlineLabel(deadline, now.toLocalDate(), lang)
        return when {
            TaskDeadline.isMissed(deadline, now) -> HabitsStr.deadlineMissedAt.of(lang).format(at)
            offset <= 0 -> HabitsStr.deadlineNow.of(lang).format(at)
            else -> HabitsStr.deadlineIn.of(lang).format(offsetLabel(offset, lang), at, TaskDeadline.POINTS_ON_TIME)
        }
    }

    internal suspend fun handle(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> { scheduleAll(app, catchUp = true); return }
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
                    notify(app, lang, "habit", id, habit.title, HabitsStr.habitReminderText.of(lang), ACTION_HABIT_DONE)
                }
                nextHabitTrigger(habit, doneToday = true)?.let { setAlarm(app, it, pending(app, ACTION_HABIT, "habit", id)) }
            }
            ACTION_TASK -> {
                val task = dao.taskById(id) ?: return
                val now = LocalDateTime.now()
                if (moduleOn && task.doneAt == null) {
                    notify(app, lang, "task", id, task.title, taskReminderText(task, intent.getIntExtra(EXTRA_OFFSET, 0), now, lang), ACTION_TASK_DONE)
                }
                dao.setLastReminder(id, TaskDeadline.toMillis(now))
                // Switched off it stays quiet, but the chain goes on up to the deadline
                scheduleTask(app, task, now)
            }
            ACTION_HABIT_DONE -> {
                dao.insertCheck(HabitCheck(id, today.toEpochDay()))
                NotificationManagerCompatSafe.cancel(app, notificationId("habit", id))
                com.example.lifeorganizer.habits.HabitsIntegration.changed(app)
            }
            ACTION_TASK_DONE -> {
                // Same rules as in the app: points if in time, alarm and notification cleared
                com.example.lifeorganizer.habits.TaskActions.setDone(app, id, done = true)
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
