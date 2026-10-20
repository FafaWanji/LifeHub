package com.example.lifeorganizer.habits.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TaskQuickParseTest {
    // Tuesday, 20 Oct 2026, 10:00
    private val now = LocalDateTime.of(2026, 10, 20, 10, 0)
    private val today = now.toLocalDate()

    private fun parse(text: String) = TaskQuickParse.parse(text, now)
    private fun task(title: String, date: LocalDate, h: Int, m: Int = 0) = QuickTask(title, date.toEpochDay(), h * 60 + m)

    @Test
    fun noDateMeansTodayEndOfDay() {
        assertEquals(task("Aufgabe 5", today, 23, 59), parse("Aufgabe 5"))
        assertEquals(task("Exercise 5", today, 23, 59), parse("  Exercise   5 "))
        assertNull(parse("   "))
    }

    @Test
    fun timeAtTheEnd() {
        assertEquals(task("Report", today, 14), parse("Report 14:00"))
        assertEquals(task("Zahnarzt anrufen", today, 14), parse("Zahnarzt anrufen 14 Uhr"))
        assertEquals(task("Zahnarzt anrufen", today, 14), parse("Zahnarzt anrufen um 14 Uhr"))
        assertEquals(task("Zahnarzt anrufen", today, 14, 30), parse("Zahnarzt anrufen um 14:30 Uhr"))
        assertEquals(task("Zahnarzt anrufen", today, 14, 30), parse("Zahnarzt anrufen 14.30 Uhr"))
        assertEquals(task("Call dentist", today, 14), parse("Call dentist at 2pm"))
        assertEquals(task("Call dentist", today, 23, 0), parse("Call dentist 11 pm"))
    }

    @Test
    fun passedTimeWithoutDateMeansTomorrow() {
        assertEquals(task("Abgabe", today.plusDays(1), 9), parse("Abgabe 9 Uhr"))
        assertEquals(task("Abgabe", today.plusDays(1), 10), parse("Abgabe 10:00"))
    }

    @Test
    fun dateWords() {
        assertEquals(task("Aufgabe 5", today.plusDays(1), 23, 59), parse("Aufgabe 5 morgen"))
        assertEquals(task("Aufgabe 5", today.plusDays(1), 14), parse("Aufgabe 5 morgen 14 Uhr"))
        assertEquals(task("Aufgabe 5", today.plusDays(1), 14), parse("Aufgabe 5 bis morgen um 14 Uhr"))
        assertEquals(task("Exercise 5", today.plusDays(1), 14), parse("Exercise 5 14:00 tomorrow"))
        assertEquals(task("Essay", today.plusDays(2), 23, 59), parse("Essay übermorgen"))
        // Explicit today keeps a passed time
        assertEquals(task("Abgabe", today, 9), parse("Abgabe heute 9 Uhr"))
    }

    @Test
    fun weekdaysAndDates() {
        assertEquals(task("Report", LocalDate.of(2026, 10, 23), 9, 30), parse("Report fri 9:30"))
        assertEquals(task("Report", LocalDate.of(2026, 10, 23), 23, 59), parse("Report am Freitag"))
        // Today's weekday means today
        assertEquals(task("Report", today, 23, 59), parse("Report Dienstag"))
        assertEquals(task("Report", LocalDate.of(2026, 10, 24), 9, 30), parse("Report 24.10. 9:30"))
        assertEquals(task("Report", LocalDate.of(2027, 1, 5), 23, 59), parse("Report 5.1."))
        assertEquals(task("Report", LocalDate.of(2026, 12, 1), 23, 59), parse("Report 1.12.2026"))
    }

    @Test
    fun titleStaysWhenNothingMatches() {
        // Numbers and everyday words in the title are no deadline
        assertEquals(task("Mach das so", today, 23, 59), parse("Mach das so"))
        assertEquals(task("Seite 25 lesen", today, 23, 59), parse("Seite 25 lesen"))
        assertEquals(task("Kapitel 3.2", today, 23, 59), parse("Kapitel 3.2"))
        // A line that is only a date stays the title
        assertEquals(task("morgen", today, 23, 59), parse("morgen"))
    }
}
