package com.example.lifeorganizer.calendar.util

import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.EventWithReminders
import java.time.Instant
import java.time.ZoneId

/**
 * Finds copies of a repeating event that are only later occurrences of another copy.
 *
 * The calendar's .ics export used to write every expanded occurrence of a series, each with the
 * series' RRULE. Importing such a file created one new series per week, so week N showed N copies.
 */
object SeriesDedup {

    /** Events that duplicate an earlier series (same title, rule, place, duration and time slot). */
    fun duplicates(events: List<Event>): List<Event> {
        val recurring = events.filter { !it.recurrenceRule.isNullOrBlank() }
        return recurring
            .groupBy { key(it) }
            .values
            .flatMap { group ->
                if (group.size < 2) return@flatMap emptyList()
                val sorted = group.sortedWith(compareBy({ it.startTimeMillis }, { it.id }))
                val kept = mutableListOf<Event>()
                val dropped = mutableListOf<Event>()
                for (e in sorted) {
                    if (kept.any { isOccurrenceOf(e, it) }) dropped += e else kept += e
                }
                dropped
            }
    }

    /** Keeps the first VEVENT of each series in an imported file. */
    fun collapse(imported: List<EventWithReminders>): List<EventWithReminders> {
        val drop = duplicates(imported.mapIndexed { i, e -> e.event.copy(id = -(i + 1L)) }).map { -it.id - 1 }.toSet()
        return imported.filterIndexed { i, _ -> i.toLong() !in drop }
    }

    private fun key(e: Event): List<Any?> {
        val zone = runCatching { ZoneId.of(e.timezone) }.getOrDefault(ZoneId.systemDefault())
        val start = Instant.ofEpochMilli(e.startTimeMillis).atZone(zone).toLocalTime()
        return listOf(
            e.title.trim().lowercase(),
            e.recurrenceRule?.trim()?.uppercase(),
            e.targetAddress?.trim()?.lowercase(),
            e.endTimeMillis?.minus(e.startTimeMillis),
            e.isAllDay,
            start
        )
    }

    private fun isOccurrenceOf(later: Event, series: Event): Boolean {
        val zone = runCatching { ZoneId.of(series.timezone) }.getOrDefault(ZoneId.systemDefault())
        val day = Instant.ofEpochMilli(later.startTimeMillis).atZone(zone).toLocalDate()
        return RRuleExpander.expand(EventWithReminders(series, emptyList()), day, day)
            .any { it.event.startTimeMillis == later.startTimeMillis }
    }
}
