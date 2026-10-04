package com.example.lifeorganizer.core.smartadd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class OfflineParserTest {
    private val now = LocalDateTime.of(2026, 10, 5, 10, 0) // Monday
    private fun event(text: String, waypoints: List<Pair<String, String>> = emptyList()) =
        OfflineParser.parse(text, waypoints, now, ZoneOffset.UTC).single() as SmartResult.Event
    private fun at(millis: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)

    @Test
    fun `relative day and time`() {
        val e = event("morgen 15 Uhr Zahnarzt")
        assertEquals("Zahnarzt", e.title)
        assertEquals(LocalDateTime.of(2026, 10, 6, 15, 0), at(e.startTimeMillis))
        assertEquals(LocalDateTime.of(2026, 10, 6, 16, 0), at(e.endTimeMillis))
    }

    @Test
    fun `weekday, minutes and waypoint`() {
        val e = event("Fr um 18:30 Kino bei Oma", listOf("Oma" to "Gartenweg 3"))
        assertEquals(LocalDateTime.of(2026, 10, 9, 18, 30), at(e.startTimeMillis))
        assertEquals("Gartenweg 3", e.location)
        assertEquals("Kino bei Oma", e.title)
    }

    @Test
    fun `date with time range`() {
        val e = event("12.10. 9-11 Uhr Klausur Raum 12")
        assertEquals(LocalDateTime.of(2026, 10, 12, 9, 0), at(e.startTimeMillis))
        assertEquals(LocalDateTime.of(2026, 10, 12, 11, 0), at(e.endTimeMillis))
        assertEquals("Klausur Raum 12", e.title)
    }

    @Test
    fun `english pm`() {
        assertEquals(LocalDateTime.of(2026, 10, 6, 15, 0), at(event("tomorrow 3pm dentist").startTimeMillis))
    }

    @Test
    fun `text without date becomes a checklist note`() {
        val n = OfflineParser.parse("Einkauf\n- Milch\n- Brot", now = now).single() as SmartResult.Note
        assertEquals("Einkauf", n.title)
        assertEquals("- [ ] Milch\n- [ ] Brot", n.content)
        assertTrue(n.isChecklist)
    }
}
