package com.example.lifeorganizer.habits.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** Weekday bit masks and streaks, free of Android so they can be unit-tested. */
object HabitSchedule {
    const val EVERY_DAY = 0b1111111

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun isDue(weekdays: Int, date: LocalDate): Boolean = weekdays and bit(date.dayOfWeek) != 0

    /**
     * Due days done in a row up to [today]. Today only counts once it is done; an open today does not
     * break the streak yet. Days the habit is not due are skipped.
     */
    fun streak(weekdays: Int, doneDays: Set<Long>, today: LocalDate): Int {
        if (weekdays and EVERY_DAY == 0) return 0
        var day = today
        if (day.toEpochDay() !in doneDays) day = day.minusDays(1)
        var count = 0
        // A year back is plenty and keeps a corrupt mask from looping forever
        repeat(366) {
            if (isDue(weekdays, day)) {
                if (day.toEpochDay() in doneDays) count++ else return count
            }
            day = day.minusDays(1)
        }
        return count
    }
}
