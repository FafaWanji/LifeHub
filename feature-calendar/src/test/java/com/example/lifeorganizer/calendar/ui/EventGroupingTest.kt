package com.example.lifeorganizer.calendar.ui

import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.EventWithReminders
import com.example.lifeorganizer.core.util.Places
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventGroupingTest {

    private fun ev(id: Long, start: Long, address: String? = null, rule: String? = null) = EventWithReminders(
        event = Event(id = id, title = "E$id", description = "", startTimeMillis = start, targetAddress = address, recurrenceRule = rule, timezone = "UTC"),
        reminders = emptyList()
    )

    @Test
    fun `events at the same place are grouped, others stay single`() {
        val entries = EventGrouping.byPlace(
            listOf(
                ev(1, 1000, "Hauptstraße 1, Berlin"),
                ev(2, 2000, null),
                ev(3, 3000, "hauptstrasse 1,  Berlin "),
                ev(4, 4000, "Gartenweg 3")
            )
        )
        assertEquals(3, entries.size)
        val group = entries[0] as DayEntry.PlaceGroup
        assertEquals(listOf(1L, 3L), group.events.map { it.event.id })
        assertTrue(entries[1] is DayEntry.Single)
        assertEquals(4L, (entries[2] as DayEntry.Single).event.event.id)
    }

    @Test
    fun `a series is shown once at its next occurrence`() {
        val now = 10_000_000L
        val weekly = listOf(0L, 7, 14, 21).map { ev(9, now - 20 * 86_400_000L + it * 86_400_000L, rule = "FREQ=WEEKLY") }
        val single = ev(1, now + 1000)
        val result = EventGrouping.collapseSeries(weekly + single, now)
        assertEquals(listOf(1L, 9L).sorted(), result.map { it.event.id }.sorted())
        assertTrue(result.first { it.event.id == 9L }.event.startTimeMillis > now)
    }

    @Test
    fun `placeholder locations count as no place`() {
        assertNull(Places.clean("N/A"))
        assertNull(Places.clean("  Nicht angegeben "))
        assertNull(Places.clean(""))
        assertEquals("Hauptstraße 1", Places.clean(" Hauptstraße  1 "))
        assertTrue(Places.samePlace("Hauptstraße 1, Berlin", "hauptstrasse 1 berlin"))
    }
}
