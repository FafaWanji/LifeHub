package com.example.lifeorganizer.calendar.alarm

import android.content.Context
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.util.RRuleExpander
import java.time.LocalDate

/**
 * Only the first occurrence of a repeating event has stored reminders. This arms each reminder of a
 * series for its next occurrence instead. It reuses the reminder's id as request code, so the alarm
 * for the previous occurrence is simply replaced. Run at app start, after boot and after each alarm.
 */
object RecurringAlarmSync {

    suspend fun sync(context: Context) {
        val dao = AppDatabase.getDatabase(context).eventDao()
        val scheduler = AlarmScheduler(context)
        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        dao.getEventsWithRemindersSync()
            .filter { !it.event.recurrenceRule.isNullOrBlank() && it.reminders.isNotEmpty() }
            .forEach { series ->
                val occurrences = RRuleExpander.expand(series, today.minusDays(1), today.plusDays(400))
                    .map { it.event.startTimeMillis }
                    .sorted()
                series.reminders.forEach { reminder ->
                    val leadMillis = series.event.startTimeMillis - reminder.reminderTimeMillis
                    val nextStart = occurrences.firstOrNull { it - leadMillis > now } ?: return@forEach
                    scheduler.schedule(
                        series.event.copy(startTimeMillis = nextStart),
                        reminder.copy(reminderTimeMillis = nextStart - leadMillis)
                    )
                }
            }
    }
}
