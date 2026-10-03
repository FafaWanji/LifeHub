package com.example.lifeorganizer.calendar.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsImporterTest {

    @Test
    fun `event without DTEND keeps its time and is not all-day`() {
        val ics = "BEGIN:VCALENDAR\nBEGIN:VEVENT\nDTSTART:20261015T170000Z\nSUMMARY:Yoga\nEND:VEVENT\nEND:VCALENDAR"
        assertFalse(IcsImporter.parseIcs(ics).single().event.isAllDay)
    }

    @Test
    fun `date-only DTSTART is all-day`() {
        val ics = "BEGIN:VCALENDAR\nBEGIN:VEVENT\nDTSTART;VALUE=DATE:20261015\nSUMMARY:Urlaub\nEND:VEVENT\nEND:VCALENDAR"
        assertTrue(IcsImporter.parseIcs(ics).single().event.isAllDay)
    }
}
