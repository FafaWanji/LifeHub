package com.example.lifeorganizer.web

import android.content.Context
import com.example.lifeorganizer.calendar.alarm.AlarmScheduler
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.Reminder
import com.example.lifeorganizer.calendar.util.RRuleExpander
import com.example.lifeorganizer.core.i18n.reminderLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Calendar reads and writes for the browser: same tables and alarm handling as the app. Series are edited as a whole. */
class CalendarAccess(context: Context) {
    private val dao = AppDatabase.getDatabase(context).eventDao()
    private val alarms = AlarmScheduler(context)

    data class Input(
        val title: String,
        val description: String,
        val start: Long,
        val end: Long?,
        val allDay: Boolean,
        val categoryId: Long?,
        val recurrence: String?,
        val location: String?,
        val reminderMinutes: List<Int>
    )

    data class Occurrence(val event: Event, val start: Long, val end: Long?, val reminderMinutes: List<Int>)

    suspend fun categories() = dao.getCategoriesSync()

    /** All occurrences (series expanded) that touch [from]..[to]. */
    suspend fun occurrences(from: LocalDate, to: LocalDate): List<Occurrence> {
        val zone = ZoneId.systemDefault()
        val rangeStart = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.getEventsWithRemindersSync().flatMap { ewr ->
            val minutes = ewr.reminders.filter { it.type != "Birthday at 23:59" }
                .map { ((ewr.event.startTimeMillis - it.reminderTimeMillis) / 60_000).toInt() }
                .filter { it >= 0 }.distinct().sorted()
            RRuleExpander.expand(ewr, from, to).map { o ->
                Occurrence(ewr.event, o.event.startTimeMillis, o.event.endTimeMillis, minutes)
            }
        }.filter { o -> o.start < rangeEnd && (o.end ?: (o.start + 1)) > rangeStart }
            .sortedBy { it.start }
    }

    suspend fun create(input: Input, lang: String): Long {
        val event = apply(Event(title = "", description = "", startTimeMillis = input.start), input, null)
        val id = dao.insertEvent(event)
        saveReminders(event.copy(id = id), input.reminderMinutes, lang)
        return id
    }

    /** Returns false when the event no longer exists. */
    suspend fun update(id: Long, input: Input, lang: String): Boolean {
        val existing = dao.getEventById(id) ?: return false
        dao.getRemindersForEvent(id).forEach { alarms.cancelAll(it) }
        val updated = apply(existing, input, existing.recurrenceRule)
        dao.updateEventWithReminders(updated, emptyList())
        saveReminders(updated, input.reminderMinutes, lang)
        return true
    }

    suspend fun delete(id: Long): Boolean {
        dao.getEventById(id) ?: return false
        dao.getRemindersForEvent(id).forEach { alarms.cancelAll(it) }
        dao.deleteRemindersByEventId(id)
        dao.deleteEventById(id)
        return true
    }

    /** [currentRule]: a rule the app set (e.g. with BYDAY) survives a browser edit that sends it back unchanged. */
    private fun apply(e: Event, i: Input, currentRule: String?) = e.copy(
        title = i.title.trim().ifBlank { "—" },
        description = i.description,
        startTimeMillis = i.start,
        endTimeMillis = i.end?.takeIf { it >= i.start },
        isAllDay = i.allDay,
        categoryId = i.categoryId,
        recurrenceRule = i.recurrence?.takeIf { it in BROWSER_RULES || it == currentRule },
        targetAddress = i.location?.takeIf { it.isNotBlank() }
    )

    private suspend fun saveReminders(event: Event, minutes: List<Int>, lang: String) {
        val reminders = minutes.distinct().map { m ->
            val offset = m * 60_000L
            Reminder(eventId = event.id, reminderTimeMillis = event.startTimeMillis - offset, type = reminderLabel(offset, lang), timezone = event.timezone)
        }
        dao.insertReminders(reminders)
        dao.getRemindersForEvent(event.id).forEach { alarms.schedule(event, it) }
    }

    companion object {
        /** Repeat rules the browser offers. */
        val BROWSER_RULES = setOf("FREQ=DAILY", "FREQ=WEEKLY", "FREQ=MONTHLY", "FREQ=YEARLY")

        fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
