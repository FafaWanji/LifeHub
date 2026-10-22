package com.example.lifeorganizer.calendar.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class DefaultReminderRulesTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, day, h, m).toInstant(zone).toEpochMilli()
    private fun occ(id: Long, start: Long, place: String? = null, own: Boolean = false, allDay: Boolean = false, birthday: Boolean = false) =
        Occurrence(id, "E$id", start, place, allDay, birthday, own)
    private fun ids(list: List<Pair<Occurrence, Long>>) = list.map { it.first.eventId }

    @Test
    fun twoHoursBeforeEveryEventWithoutOwnReminder() {
        val due = DefaultReminderRules.due(listOf(occ(1, at(22, 14)), occ(2, at(22, 9), own = true)), 120, zone)
        assertEquals(listOf(1L), ids(due))
        assertEquals(at(22, 12), due.single().second)
    }

    @Test
    fun placeGroupOnlyTheFirst() {
        // Three appointments in the same clinic (spelled a bit differently) and one elsewhere
        val due = DefaultReminderRules.due(
            listOf(
                occ(1, at(22, 10), "Praxis Dr. Meier, Hauptstraße 1"),
                occ(2, at(22, 8), "praxis dr meier hauptstrasse 1"),
                occ(3, at(22, 12), "Praxis Dr. Meier, Hauptstraße 1"),
                occ(4, at(22, 11), "Bahnhof")
            ), 120, zone
        )
        assertEquals(listOf(2L, 4L), ids(due))
    }

    @Test
    fun firstOfGroupWithOwnReminderSilencesTheRest() {
        val due = DefaultReminderRules.due(listOf(occ(1, at(22, 8), "Uni", own = true), occ(2, at(22, 10), "Uni")), 120, zone)
        assertTrue(due.isEmpty())
    }

    @Test
    fun samePlaceOnAnotherDayIsItsOwnGroup() {
        val due = DefaultReminderRules.due(listOf(occ(1, at(22, 8), "Uni"), occ(2, at(23, 8), "Uni")), 120, zone)
        assertEquals(listOf(1L, 2L), ids(due))
    }

    @Test
    fun notForAllDayEventsBirthdaysOrWhenOff() {
        val list = listOf(occ(1, at(22, 0), allDay = true), occ(2, at(22, 0), birthday = true), occ(3, at(22, 15)))
        assertEquals(listOf(3L), ids(DefaultReminderRules.due(list, 120, zone)))
        assertTrue(DefaultReminderRules.due(list, -1, zone).isEmpty())
    }
}
