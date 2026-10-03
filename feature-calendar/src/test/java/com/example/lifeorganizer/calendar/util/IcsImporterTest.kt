package com.example.lifeorganizer.calendar.util

import org.junit.Assert.assertEquals
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

    private fun vevent(start: String, title: String = "Datenbanktech PCÜ A143", rule: String? = "FREQ=WEEKLY") =
        "BEGIN:VEVENT\nUID:privatecal-7@privatecalendar.app\nDTSTART:$start\nSUMMARY:$title\nLOCATION:HTW Treskowallee\n" +
            (rule?.let { "RRULE:$it\n" } ?: "") + "END:VEVENT\n"

    @Test
    fun `old export with every occurrence of a series imports one series`() {
        // Weekly 17:15 Berlin time, across the switch to winter time (25 Oct 2026)
        val ics = "BEGIN:VCALENDAR\n" +
            vevent("20261013T151500Z") + vevent("20261020T151500Z") +
            vevent("20261027T161500Z") + vevent("20261103T161500Z") +
            vevent("20261014T100000Z", title = "Unternehmens Management") +
            "END:VCALENDAR"
        val zone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Berlin"))
        try {
            val events = IcsImporter.parseIcs(ics).map { it.event }
            assertEquals(2, events.size)
            assertEquals(1, events.count { it.title.startsWith("Datenbanktech") })
        } finally {
            java.util.TimeZone.setDefault(zone)
        }
    }

    @Test
    fun `single events with the same title are kept`() {
        val ics = "BEGIN:VCALENDAR\n" + vevent("20261013T151500Z", rule = null) + vevent("20261020T151500Z", rule = null) + "END:VCALENDAR"
        assertEquals(2, IcsImporter.parseIcs(ics).size)
    }

    @Test
    fun `TZID start is read in its zone`() {
        val ics = "BEGIN:VCALENDAR\nBEGIN:VEVENT\nDTSTART;TZID=Europe/Berlin:20261015T171500\nSUMMARY:X\nEND:VEVENT\nEND:VCALENDAR"
        val expected = java.time.ZonedDateTime.of(2026, 10, 15, 17, 15, 0, 0, java.time.ZoneId.of("Europe/Berlin")).toInstant().toEpochMilli()
        assertEquals(expected, IcsImporter.parseIcs(ics).single().event.startTimeMillis)
    }
}
