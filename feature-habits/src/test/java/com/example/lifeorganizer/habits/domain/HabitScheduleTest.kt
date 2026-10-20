package com.example.lifeorganizer.habits.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitScheduleTest {
    // 2026-10-15 is a Thursday
    private val today = LocalDate.of(2026, 10, 15)
    private fun days(vararg back: Long) = back.map { today.minusDays(it).toEpochDay() }.toSet()

    @Test
    fun dueOnSelectedWeekdaysOnly() {
        val mondays = HabitSchedule.bit(DayOfWeek.MONDAY)
        assertTrue(HabitSchedule.isDue(mondays, LocalDate.of(2026, 10, 12)))
        assertFalse(HabitSchedule.isDue(mondays, today))
    }

    @Test
    fun streakCountsTodayWhenDone() {
        assertEquals(3, HabitSchedule.streak(HabitSchedule.EVERY_DAY, days(0, 1, 2), today))
    }

    @Test
    fun openTodayDoesNotBreakStreak() {
        assertEquals(2, HabitSchedule.streak(HabitSchedule.EVERY_DAY, days(1, 2, 4), today))
    }

    @Test
    fun missedDayEndsStreak() {
        assertEquals(0, HabitSchedule.streak(HabitSchedule.EVERY_DAY, days(2, 3), today))
    }

    @Test
    fun daysNotDueAreSkipped() {
        // Mondays and Thursdays: done today (Thu) and Monday → 2, Wednesday/Tuesday don't matter
        val mask = HabitSchedule.bit(DayOfWeek.MONDAY) or HabitSchedule.bit(DayOfWeek.THURSDAY)
        assertEquals(2, HabitSchedule.streak(mask, days(0, 3), today))
    }

    @Test
    fun tasksAreGroupedByDeadline() {
        val t = today.toEpochDay()
        val noon = today.atTime(12, 0)
        assertEquals(TaskGroup.OVERDUE, taskGroup(t - 1, TaskDeadline.END_OF_DAY, noon))
        assertEquals(TaskGroup.TODAY, taskGroup(t, TaskDeadline.END_OF_DAY, noon))
        assertEquals(TaskGroup.SOON, taskGroup(t + 7, 0, noon))
        assertEquals(TaskGroup.LATER, taskGroup(t + 8, 0, noon))
        assertEquals(TaskGroup.NO_DEADLINE, taskGroup(null, TaskDeadline.END_OF_DAY, noon))
    }

    @Test
    fun taskIsOverdueRightAfterItsDeadlineTime() {
        val t = today.toEpochDay()
        // Due today 14:00: still today at 14:00, overdue from 14:01
        assertEquals(TaskGroup.TODAY, taskGroup(t, 14 * 60, today.atTime(13, 59)))
        assertEquals(TaskGroup.TODAY, taskGroup(t, 14 * 60, today.atTime(14, 0, 59)))
        assertEquals(TaskGroup.OVERDUE, taskGroup(t, 14 * 60, today.atTime(14, 1)))
    }
}
