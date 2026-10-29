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
}
