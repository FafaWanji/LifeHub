package com.example.lifeorganizer.calendar.util

import com.example.lifeorganizer.calendar.data.Event
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Which part of a repeating event an edit or delete applies to. */
enum class SeriesScope { THIS, FOLLOWING, ALL }

/** Exceptions (EXDATE) and ends (UNTIL) of repeating events. */
object SeriesRules {
    private val compact = DateTimeFormatter.BASIC_ISO_DATE

    fun zone(e: Event): ZoneId = runCatching { ZoneId.of(e.timezone) }.getOrDefault(ZoneId.systemDefault())

    fun localDate(e: Event, millis: Long = e.startTimeMillis): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone(e)).toLocalDate()

    fun exDates(e: Event): Set<LocalDate> =
        e.exDates.orEmpty().split(',').mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toSet()

    /** The series without the occurrence on [date]. */
    fun withoutDate(e: Event, date: LocalDate): Event =
        e.copy(exDates = (exDates(e) + date).sorted().joinToString(","))

    /** The series ending the day before [date] (UNTIL replaces COUNT and an older UNTIL). */
    fun endingBefore(e: Event, date: LocalDate): Event {
        val parts = e.recurrenceRule.orEmpty().split(';')
            .filter { it.isNotBlank() && !it.startsWith("UNTIL=") && !it.startsWith("COUNT=") }
        return e.copy(recurrenceRule = (parts + "UNTIL=${date.minusDays(1).format(compact)}").joinToString(";"))
    }

    /** UNTIL as a local date; accepts "20261031" and "20261031T235959Z". */
    fun until(rule: String?): LocalDate? {
        val v = rule.orEmpty().split(';').firstOrNull { it.startsWith("UNTIL=") }?.substringAfter('=') ?: return null
        return runCatching { LocalDate.parse(v.take(8), compact) }.getOrNull()
    }
}
