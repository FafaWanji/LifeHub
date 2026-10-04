package com.example.lifeorganizer.calendar.util

import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.EventWithReminders
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SeriesRulesTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val start = LocalDate.of(2026, 10, 5) // Monday
    private fun weekly(rule: String = "FREQ=WEEKLY", exDates: String? = null) = Event(
        title = "Yoga", description = "", timezone = zone.id, recurrenceRule = rule, exDates = exDates,
        startTimeMillis = start.atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
    )
    private fun dates(e: Event) = RRuleExpander.expand(EventWithReminders(e, emptyList()), start, start.plusWeeks(5))
        .map { SeriesRules.localDate(it.event) }

    @Test
    fun `skipped date is not expanded`() {
        val e = SeriesRules.withoutDate(weekly(), start.plusWeeks(1))
        assertEquals(listOf(0L, 2, 3, 4, 5).map { start.plusWeeks(it) }, dates(e))
    }

    @Test
    fun `series ends before the cut`() {
        val e = SeriesRules.endingBefore(weekly("FREQ=WEEKLY;COUNT=10"), start.plusWeeks(2))
        assertEquals("FREQ=WEEKLY;UNTIL=20261018", e.recurrenceRule)
        assertEquals(listOf(start, start.plusWeeks(1)), dates(e))
    }

    @Test
    fun `count limits occurrences from the series start`() {
        val e = weekly("FREQ=WEEKLY;COUNT=3")
        assertEquals(listOf(start, start.plusWeeks(1), start.plusWeeks(2)), dates(e))
        assertEquals(listOf(start.plusWeeks(2)), RRuleExpander.expand(EventWithReminders(e, emptyList()), start.plusWeeks(2), start.plusWeeks(9))
            .map { SeriesRules.localDate(it.event) })
    }
}
