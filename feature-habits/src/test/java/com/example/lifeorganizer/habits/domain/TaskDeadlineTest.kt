package com.example.lifeorganizer.habits.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class TaskDeadlineTest {
    private val day = LocalDate.of(2026, 10, 22)
    private val deadline = day.atTime(14, 0)
    private val defaults = TaskDeadline.parseOffsets(TaskDeadline.DEFAULT_OFFSETS)

    @Test
    fun offsetsAreParsedAndCleanedUp() {
        assertEquals(listOf(1440, 60), defaults)
        assertEquals(listOf(10080, 60, 15), TaskDeadline.parseOffsets("15, 60,x,0,-5,60,10080"))
        assertEquals(emptyList<Int>(), TaskDeadline.parseOffsets(""))
        assertEquals(emptyList<Int>(), TaskDeadline.parseOffsets(null))
        assertEquals("1440,60,15", TaskDeadline.formatOffsets(listOf(15, 1440, 60, 15, 0)))
    }

    @Test
    fun deadlineIsDayPlusTime() {
        assertEquals(deadline, TaskDeadline.deadline(day.toEpochDay(), 14 * 60))
        assertEquals(day.atTime(23, 59), TaskDeadline.deadline(day.toEpochDay(), TaskDeadline.END_OF_DAY))
        assertNull(TaskDeadline.deadline(null, 14 * 60))
    }

    @Test
    fun remindersIncludeOneAtTheDeadline() {
        val all = TaskDeadline.reminders(deadline, defaults)
        assertEquals(
            listOf(deadline.minusDays(1) to 1440, deadline.minusHours(1) to 60, deadline to 0),
            all.map { it.at to it.offsetMinutes }
        )
        // Even without any chosen reminder
        assertEquals(listOf(TaskDeadline.Reminder(deadline, 0)), TaskDeadline.reminders(deadline, emptyList()))
    }

    @Test
    fun nextReminderIsTheFirstStillAhead() {
        assertEquals(1440, TaskDeadline.nextReminder(deadline, defaults, deadline.minusDays(2))?.offsetMinutes)
        assertEquals(60, TaskDeadline.nextReminder(deadline, defaults, deadline.minusDays(1))?.offsetMinutes)
        assertEquals(0, TaskDeadline.nextReminder(deadline, defaults, deadline.minusMinutes(30))?.offsetMinutes)
        assertNull(TaskDeadline.nextReminder(deadline, defaults, deadline))
    }

    @Test
    fun missedReminderIsTheLatestOneWhileOff() {
        // Phone off from two days before until 30 min before: the 1-hour reminder is caught up
        val missed = TaskDeadline.missedReminder(deadline, defaults, deadline.minusDays(2), deadline.minusMinutes(30))
        assertEquals(60, missed?.offsetMinutes)
        // Off across the deadline: the push at the deadline itself
        assertEquals(0, TaskDeadline.missedReminder(deadline, defaults, deadline.minusMinutes(30), deadline.plusHours(3))?.offsetMinutes)
        // Nothing fell into the gap
        assertNull(TaskDeadline.missedReminder(deadline, defaults, deadline.minusMinutes(50), deadline.minusMinutes(10)))
    }

    @Test
    fun tenPointsByTheDeadlineZeroWhenLate() {
        assertEquals(10, TaskDeadline.points(deadline, deadline.minusDays(3)))
        assertEquals(10, TaskDeadline.points(deadline, deadline))
        // The deadline minute itself still counts
        assertEquals(10, TaskDeadline.points(deadline, deadline.plusSeconds(59)))
        assertEquals(0, TaskDeadline.points(deadline, deadline.plusMinutes(1)))
        assertEquals(0, TaskDeadline.points(null, deadline))
    }

    @Test
    fun missedFromTheMinuteAfter() {
        assertFalse(TaskDeadline.isMissed(deadline, deadline.plusSeconds(59)))
        assertTrue(TaskDeadline.isMissed(deadline, deadline.plusMinutes(1)))
        assertFalse(TaskDeadline.isMissed(null, LocalDateTime.MAX))
    }

    @Test
    fun oldReminderTimeBecomesAnOffset() {
        val zone = ZoneOffset.UTC
        // Old default: 09:00 on the deadline day, deadline now 23:59 → 14 h 59 min before
        val nine = day.atTime(9, 0).toInstant(zone).toEpochMilli()
        assertEquals(14 * 60 + 59, TaskDeadline.legacyOffset(day.toEpochDay(), TaskDeadline.END_OF_DAY, nine, zone))
        val dayBefore = day.minusDays(1).atTime(23, 59).toInstant(zone).toEpochMilli()
        assertEquals(1440, TaskDeadline.legacyOffset(day.toEpochDay(), TaskDeadline.END_OF_DAY, dayBefore, zone))
        // After the deadline: dropped, the push at the deadline covers it
        val after = day.plusDays(1).atTime(8, 0).toInstant(zone).toEpochMilli()
        assertNull(TaskDeadline.legacyOffset(day.toEpochDay(), TaskDeadline.END_OF_DAY, after, zone))
    }
}
