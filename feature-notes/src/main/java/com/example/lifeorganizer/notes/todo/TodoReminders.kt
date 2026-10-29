package com.example.lifeorganizer.notes.todo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.lifeorganizer.core.i18n.durationLabel
import com.example.lifeorganizer.core.i18n.localDateFormatter
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.core.settings.NotificationChannels
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.notes.R
import com.example.lifeorganizer.notes.data.Note
import com.example.lifeorganizer.notes.data.NoteWithLabels
import com.example.lifeorganizer.notes.data.NotesDatabase
import com.example.lifeorganizer.notes.data.TodoKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** "Heute 14:00", "Morgen 23:59" or "18. Okt. 09:30". */
fun appointmentLabel(at: LocalDateTime, today: LocalDate, lang: String): String {
    val day = when (at.toLocalDate().toEpochDay() - today.toEpochDay()) {
        0L -> TodoStr.today.of(lang)
        1L -> TodoStr.tomorrow.of(lang)
        else -> at.toLocalDate().format(localDateFormatter(lang, if (at.year != today.year) "yyyyMMMd" else "MMMd"))
    }
    return "$day %02d:%02d".format(at.hour, at.minute)
}

/** The appointment of a "To Do X" note, null when the note is no such to-do or has none set. */
fun NoteWithLabels.appointment(): LocalDateTime? =
    if (todoKind == TodoKind.DATED) TaskDeadline.deadline(note.dueEpochDay, note.dueMinute) else null

/** Reminder settings of to-dos, read together. */
private class TodoSettings(context: Context) {
    val repeatAfter = MicroSettings.get(context, MicroSettings.DEADLINE_REPEAT).coerceAtLeast(0)
    val snoozeMinutes = MicroSettings.get(context, MicroSettings.SNOOZE_MINUTES).coerceAtLeast(1)
    val quiet = if (MicroSettings.get(context, MicroSettings.QUIET_HOURS)) {
        TaskDeadline.QuietHours(MicroSettings.get(context, MicroSettings.QUIET_START), MicroSettings.get(context, MicroSettings.QUIET_END))
    } else null
}

/**
 * Reminders of "To Do X" notes: the chosen times before the appointment plus one at the appointment itself.
 * Only the next alarm of each note is set; the receiver sets the following one when it fires. Everything is
 * re-armed after a reboot or an app update.
 */
object TodoReminders {
    /** Same as the calendar's "open note" action, so MainActivity opens the note. */
    private const val ACTION_OPEN_NOTE = "com.example.lifeorganizer.ACTION_OPEN_NOTE"
    private const val EXTRA_NOTE_ID = "NOTE_ID"
    private const val ACTION_REMIND = "com.example.lifeorganizer.notes.REMIND_TODO"
    private const val ACTION_DONE = "com.example.lifeorganizer.notes.TODO_DONE"
    private const val ACTION_SNOOZE = "com.example.lifeorganizer.notes.TODO_SNOOZE"
    private const val EXTRA_OFFSET = "offset"
    /** Offset of a snoozed push: its text counts down to the appointment instead. */
    private const val SNOOZED = Int.MIN_VALUE

    /** Called after a to-do changed (e.g. to refresh widgets); set by the app. */
    @Volatile
    var onChanged: (suspend (Context) -> Unit)? = null

    private fun alarms(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // The note id goes into the data URI, so every note has its own PendingIntent.
    private fun intent(context: Context, action: String, id: Long) =
        Intent(context, TodoReminderReceiver::class.java).setAction(action).setData(Uri.parse("lifehub-todo://$id"))

    private fun pending(context: Context, action: String, id: Long, offset: Int? = null) = PendingIntent.getBroadcast(
        context, 0, intent(context, action, id).apply { offset?.let { putExtra(EXTRA_OFFSET, it) } },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun notificationId(id: Long) = 1_700_000_000 + (id % 100_000_000).toInt()

    private fun setAlarm(context: Context, at: LocalDateTime, operation: PendingIntent) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = alarms(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
    }

    private fun cancelNotification(context: Context, id: Long) = NotificationManagerCompat.from(context).cancel(notificationId(id))

    private fun active(n: NoteWithLabels) = !n.note.isDeleted && !n.note.isDone

    /** Arms the next push of an open "To Do X" note; anything else loses its alarm and notification. */
    fun schedule(context: Context, n: NoteWithLabels, now: LocalDateTime = LocalDateTime.now()) {
        alarms(context).cancel(pending(context, ACTION_REMIND, n.note.id))
        val at = n.appointment()
        if (at == null || !active(n)) {
            cancelNotification(context, n.note.id)
            return
        }
        val next = next(n.note, at, now, TodoSettings(context)) ?: return
        setAlarm(context, next.at, pending(context, ACTION_REMIND, n.note.id, next.offsetMinutes))
    }

    /** After a note was saved, ticked off, trashed or restored. */
    suspend fun schedule(context: Context, noteId: Long) {
        val n = NotesDatabase.getDatabase(context).noteDao().getNoteWithLabels(noteId)
        if (n == null) cancel(context, noteId) else schedule(context, n)
        runCatching { onChanged?.invoke(context.applicationContext) }
    }

    fun cancel(context: Context, noteId: Long) {
        alarms(context).cancel(pending(context, ACTION_REMIND, noteId))
        cancelNotification(context, noteId)
    }

    private fun next(note: Note, at: LocalDateTime, now: LocalDateTime, settings: TodoSettings) =
        TaskDeadline.nextReminder(at, TaskDeadline.parseOffsets(note.reminderOffsets), now, settings.repeatAfter, settings.quiet)

    /** Whether reminders can come on time: notifications allowed and exact alarms possible. */
    fun notificationsAllowed(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms(context).canScheduleExactAlarms()

    /**
     * After a reboot, an app update, a backup import, a label change or when exact alarms were allowed. With
     * [catchUp], a push that was due while the phone was off is sent now (only the latest one per note).
     */
    suspend fun scheduleAll(context: Context, catchUp: Boolean = false) {
        val dao = NotesDatabase.getDatabase(context).noteDao()
        val now = LocalDateTime.now()
        val lang = SettingsManager(context).languageCode.first()
        val settings = TodoSettings(context)
        dao.getAllNotesWithLabelsSync().forEach { n ->
            val at = n.appointment()
            if (catchUp && at != null && active(n)) {
                val since = TaskDeadline.toLocal(n.note.lastReminderAt ?: n.note.updatedAt)
                TaskDeadline.missedReminder(at, TaskDeadline.parseOffsets(n.note.reminderOffsets), since, now, settings.repeatAfter, settings.quiet)
                    ?.let { missed ->
                        notify(context, lang, n.note, text(at, missed.offsetMinutes, now, lang), settings)
                        dao.setLastReminder(n.note.id, TaskDeadline.toMillis(now))
                    }
            }
            schedule(context, n, now)
        }
    }

    /** "Termin in 1 h (Heute 14:00)", "Termin jetzt (…)" or "Termin verpasst (…)"; a snoozed push counts down from now. */
    private fun text(at: LocalDateTime, offset: Int, now: LocalDateTime, lang: String): String {
        val label = appointmentLabel(at, now.toLocalDate(), lang)
        // Rounded up, so "in 1 min" until the minute itself
        val before = if (offset == SNOOZED) ((java.time.Duration.between(now, at).seconds + 59) / 60).toInt() else offset
        return when {
            TaskDeadline.isMissed(at, now) -> TodoStr.appointmentMissed.of(lang).format(label)
            before <= 0 -> TodoStr.appointmentNow.of(lang).format(label)
            else -> TodoStr.appointmentIn.of(lang).format(durationLabel(before, lang), label)
        }
    }

    private fun notify(context: Context, lang: String, note: Note, text: String, settings: TodoSettings) {
        NotificationChannels.ensureHabitsAndTasks(context, lang)
        val nid = notificationId(note.id)
        val open = PendingIntent.getActivity(
            context, nid,
            Intent(context, Class.forName("com.example.lifeorganizer.MainActivity"))
                .setAction(ACTION_OPEN_NOTE).putExtra(EXTRA_NOTE_ID, note.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.TASKS)
            .setSmallIcon(R.drawable.ic_todo_notification)
            .setContentTitle(note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty().take(60) })
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(R.drawable.ic_todo_notification, TodoStr.done.of(lang), pending(context, ACTION_DONE, note.id))
            .addAction(
                R.drawable.ic_todo_notification, TodoStr.snoozeFor.of(lang).format(durationLabel(settings.snoozeMinutes, lang)),
                pending(context, ACTION_SNOOZE, note.id)
            )
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(nid, notification) }
    }

    internal suspend fun handle(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> { scheduleAll(app, catchUp = true); return }
            // Exact alarms were just allowed: re-arm so pushes come on the minute
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> { scheduleAll(app); return }
        }
        val id = intent.data?.host?.toLongOrNull() ?: return
        val dao = NotesDatabase.getDatabase(app).noteDao()
        val n = dao.getNoteWithLabels(id)
        val at = n?.appointment()
        if (n == null || at == null) { cancel(app, id); return }
        val now = LocalDateTime.now()
        val settings = TodoSettings(app)
        when (intent.action) {
            ACTION_REMIND -> {
                if (active(n)) {
                    val lang = SettingsManager(app).languageCode.first()
                    notify(app, lang, n.note, text(at, intent.getIntExtra(EXTRA_OFFSET, 0), now, lang), settings)
                }
                dao.setLastReminder(id, TaskDeadline.toMillis(now))
                schedule(app, n, now)
            }
            ACTION_SNOOZE -> {
                cancelNotification(app, id)
                if (!active(n)) return
                // Again after the snooze time, but never later than the next planned push
                var next = now.plusMinutes(settings.snoozeMinutes.toLong())
                var offset = SNOOZED
                next(n.note, at, now, settings)?.takeIf { !it.at.isAfter(next) }?.let { next = it.at; offset = it.offsetMinutes }
                alarms(app).cancel(pending(app, ACTION_REMIND, id))
                setAlarm(app, next, pending(app, ACTION_REMIND, id, offset))
            }
            ACTION_DONE -> {
                dao.setDone(id, true)
                schedule(app, id)
            }
        }
    }
}

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TodoReminders.handle(context, intent)
            } finally {
                result.finish()
            }
        }
    }
}
