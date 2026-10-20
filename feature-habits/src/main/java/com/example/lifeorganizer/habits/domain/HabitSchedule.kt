package com.example.lifeorganizer.habits.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

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

/** Where an open task belongs on the "Today" list. */
enum class TaskGroup { OVERDUE, TODAY, SOON, LATER, NO_DEADLINE }

/** Grouped by the deadline time: a task due today at 14:00 is overdue from 14:01 on. */
fun taskGroup(dueEpochDay: Long?, dueMinute: Int, now: LocalDateTime, soonDays: Int = 7): TaskGroup {
    val deadline = TaskDeadline.deadline(dueEpochDay, dueMinute) ?: return TaskGroup.NO_DEADLINE
    val t = now.toLocalDate().toEpochDay()
    val day = deadline.toLocalDate().toEpochDay()
    return when {
        TaskDeadline.isMissed(deadline, now) -> TaskGroup.OVERDUE
        day == t -> TaskGroup.TODAY
        day <= t + soonDays -> TaskGroup.SOON
        else -> TaskGroup.LATER
    }
}
