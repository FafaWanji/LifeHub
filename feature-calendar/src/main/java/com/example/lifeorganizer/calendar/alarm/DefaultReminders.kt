package com.example.lifeorganizer.calendar.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.debug.DebugLogger
import com.example.lifeorganizer.calendar.util.RRuleExpander
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.core.settings.SettingsManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Locale

/**
 * Plans the default reminder of events that have none of their own (see [DefaultReminderRules]). Nothing is stored
 * per event: one alarm wakes for the next due one, the receiver notifies and plans the following. So it applies to
 * old, new, imported and PC-made events alike, and an own reminder simply replaces it.
 */
object DefaultReminders {
    const val ACTION = "com.example.lifeorganizer.calendar.DEFAULT_REMINDER"
    private const val PREFS = "default_reminders"
    /** Default reminders due up to this time (epoch millis) are handled. */
    private const val KEY_HANDLED = "handled_until"
    private const val REQUEST_CODE = 777_001
    private const val LOOK_AHEAD_DAYS = 3L
    /** Nothing due soon: look again after this long (events added meanwhile re-plan anyway). */
    private const val RECHECK_MS = 12 * 60 * 60 * 1000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun operation(context: Context) = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, AlarmReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private suspend fun due(context: Context, offset: Int, now: Long): List<Pair<Occurrence, Long>> {
        val today = LocalDate.now()
        val occurrences = AppDatabase.getDatabase(context).eventDao().getEventsWithRemindersSync().flatMap { ewr ->
            RRuleExpander.expand(ewr, today.minusDays(1), today.plusDays(LOOK_AHEAD_DAYS)).map { occ ->
                Occurrence(
                    eventId = ewr.event.id, title = ewr.event.title, start = occ.event.startTimeMillis,
                    place = ewr.event.targetAddress, allDay = ewr.event.isAllDay, birthday = ewr.event.isBirthday,
                    hasOwnReminders = ewr.reminders.isNotEmpty()
                )
            }
        }.filter { it.start > now - 24 * 60 * 60 * 1000L }
        return DefaultReminderRules.due(occurrences, offset)
    }

    /** Arms the alarm for the next default reminder. After every change of events or of the setting, and at start. */
    suspend fun sync(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(operation(app))
        val offset = MicroSettings.get(app, MicroSettings.EVENT_REMINDER)
        if (offset < 0) return
        val now = System.currentTimeMillis()
        // The very first time, nothing from the past is caught up
        if (!prefs(app).contains(KEY_HANDLED)) prefs(app).edit().putLong(KEY_HANDLED, now).apply()
        val next = due(app, offset, now).map { it.second }.firstOrNull { it > now }
        if (next != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms())) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, operation(app))
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next ?: now + RECHECK_MS, operation(app))
        }
    }

    /** The alarm fired: remind of every default reminder due since the last time (if its event is still ahead). */
    suspend fun fire(context: Context) {
        val app = context.applicationContext
        val offset = MicroSettings.get(app, MicroSettings.EVENT_REMINDER)
        val now = System.currentTimeMillis()
        val handled = prefs(app).getLong(KEY_HANDLED, now)
        if (offset >= 0) {
            val soundUri = SettingsManager(app).notificationSoundUri.first()
            val label = com.example.lifeorganizer.core.i18n.reminderLabel(offset * 60_000L, Locale.getDefault().language)
            due(app, offset, now).filter { (occ, at) -> at in (handled + 1)..now && occ.start > now }.forEach { (occ, _) ->
                DebugLogger.log("Default reminder: ${occ.title}")
                NotificationHelper(app).showNotification(
                    title = occ.title, message = label, soundUri = soundUri,
                    // Stable per occurrence, so a repeated alarm replaces instead of piling up
                    notificationId = (1_700_000_000 + ((occ.eventId * 31 + occ.start / 60_000L) % 100_000_000L)).toInt()
                )
            }
        }
        prefs(app).edit().putLong(KEY_HANDLED, now).apply()
        sync(app)
    }
}
