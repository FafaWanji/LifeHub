package com.example.lifeorganizer.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyFormatsTest {
    @Test
    fun `formats are detected`() {
        assertEquals(ImportSource.ICS_CALENDAR, LegacyFormats.detect("BEGIN:VCALENDAR\nEND:VCALENDAR"))
        assertEquals(ImportSource.LIFEORGANIZER_BACKUP, LegacyFormats.detect("""{"format":"lifeorganizer-backup","version":1}"""))
        assertEquals(ImportSource.UNKNOWN, LegacyFormats.detect("""{"waypoints":[],"crossRefs":[]}"""))
        assertEquals(ImportSource.UNKNOWN, LegacyFormats.detect("kein json"))
    }
}
