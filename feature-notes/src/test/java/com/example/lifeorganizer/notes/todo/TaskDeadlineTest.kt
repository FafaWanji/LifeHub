package com.example.lifeorganizer.notes.todo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TaskDeadlineTest {
    private val day = LocalDate.of(2026, 10, 22)
    private val deadline = day.atTime(14, 0)
    private val defaults = TaskDeadline.parseOffsets("1440,60")

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
    fun missedFromTheMinuteAfter() {
        assertFalse(TaskDeadline.isMissed(deadline, deadline.plusSeconds(59)))
        assertTrue(TaskDeadline.isMissed(deadline, deadline.plusMinutes(1)))
        assertFalse(TaskDeadline.isMissed(null, LocalDateTime.MAX))
    }

    @Test
    fun quietHoursSkipRemindersButNotTheDeadline() {
        val quiet = TaskDeadline.QuietHours(22 * 60, 7 * 60)
        assertTrue(quiet.contains(day.atTime(23, 0)))
        assertTrue(quiet.contains(day.atTime(6, 59)))
        assertFalse(quiet.contains(day.atTime(7, 0)))
        assertFalse(TaskDeadline.QuietHours(600, 600).contains(day.atTime(10, 0)))
        // Deadline 23:30: the 1-hour reminder at 22:30 is quiet, 3 hours before (20:30) and the deadline push are not
        val late = day.atTime(23, 30)
        val offsets = listOf(180, 60)
        assertEquals(listOf(180, 0), TaskDeadline.reminders(late, offsets, quiet = quiet).map { it.offsetMinutes })
        assertEquals(0, TaskDeadline.nextReminder(late, offsets, late.minusMinutes(90), quiet = quiet)?.offsetMinutes)
    }

    @Test
    fun repeatAfterAMissedDeadline() {
        val all = TaskDeadline.reminders(deadline, defaults, repeatAfter = 30)
        assertEquals(listOf(1440, 60, 0, -30), all.map { it.offsetMinutes })
        assertEquals(deadline.plusMinutes(30), TaskDeadline.nextReminder(deadline, defaults, deadline, repeatAfter = 30)?.at)
        assertNull(TaskDeadline.nextReminder(deadline, defaults, deadline.plusMinutes(30), repeatAfter = 30))
    }
}
