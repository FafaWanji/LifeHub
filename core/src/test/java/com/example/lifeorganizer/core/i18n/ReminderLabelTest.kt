package com.example.lifeorganizer.core.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderLabelTest {
    @Test
    fun `labels follow the language and use singular forms`() {
        assertEquals("15 Minuten vorher", reminderLabel(15 * 60_000L, "de"))
        assertEquals("1 Stunde vorher", reminderLabel(60 * 60_000L, "de"))
        assertEquals("2 Tage vorher", reminderLabel(2 * 1440 * 60_000L, "de"))
        assertEquals("1 day before", reminderLabel(1440 * 60_000L, "en"))
        assertEquals("Zur Startzeit", reminderLabel(0L, "de"))
    }
}
