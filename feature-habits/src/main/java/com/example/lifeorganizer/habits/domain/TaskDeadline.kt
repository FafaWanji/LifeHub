package com.example.lifeorganizer.habits.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Deadlines, reminders and points of tasks, free of Android so they can be unit-tested.
 * A deadline is a day plus a time; it counts as kept until the end of that minute.
 */
object TaskDeadline {
    /** 23:59, used when only a day is given. */
    const val END_OF_DAY = 23 * 60 + 59
    /** 1 day and 1 hour before the deadline. */
    const val DEFAULT_OFFSETS = "1440,60"
    /** Done by the deadline: 1 point; late is 0 – a deadline is a deadline. */
    const val POINTS_ON_TIME = 1
    /** Offered as chips in the task dialog: 15 min, 1 h, 3 h, 1 day, 2 days, 1 week. */
    val PRESET_OFFSETS = listOf(15, 60, 3 * 60, 24 * 60, 2 * 24 * 60, 7 * 24 * 60)

    /** One push: [offsetMinutes] before the deadline, 0 = at the deadline itself, negative = after a missed one. */
    data class Reminder(val at: LocalDateTime, val offsetMinutes: Int)

    /** No pushes from [startMinute] to [endMinute] (minutes after midnight, may run past midnight). */
    data class QuietHours(val startMinute: Int, val endMinute: Int) {
        fun contains(at: LocalDateTime): Boolean {
            val m = at.hour * 60 + at.minute
            return when {
                startMinute == endMinute -> false
                startMinute < endMinute -> m in startMinute until endMinute
                else -> m >= startMinute || m < endMinute
            }
        }
    }

    /** "1440,60" → [1440, 60]; junk, zero and duplicates are dropped, largest first. */
    fun parseOffsets(raw: String?): List<Int> =
        raw.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.distinct().sortedDescending()

    fun formatOffsets(offsets: Collection<Int>): String = offsets.filter { it > 0 }.distinct().sortedDescending().joinToString(",")

    fun deadline(dueEpochDay: Long?, dueMinute: Int): LocalDateTime? =
        dueEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay().plusMinutes(dueMinute.coerceIn(0, END_OF_DAY).toLong()) }

    /**
     * All pushes of a task in time order; the one at the deadline is always included. [repeatAfter] adds one more
     * that many minutes after the deadline (sent only while the task is still open). Pushes in [quiet] hours are
     * left out – except the one at the deadline itself.
     */
    fun reminders(deadline: LocalDateTime, offsets: List<Int>, repeatAfter: Int = 0, quiet: QuietHours? = null): List<Reminder> {
        val before = offsets.filter { it > 0 }.distinct().map { Reminder(deadline.minusMinutes(it.toLong()), it) }
        val after = if (repeatAfter > 0) listOf(Reminder(deadline.plusMinutes(repeatAfter.toLong()), -repeatAfter)) else emptyList()
        return (before + Reminder(deadline, 0) + after)
            .filter { it.offsetMinutes == 0 || quiet?.contains(it.at) != true }
            .sortedBy { it.at }
    }

    /** The push to arm next: the first one after [now], null once all have passed. */
    fun nextReminder(
        deadline: LocalDateTime, offsets: List<Int>, now: LocalDateTime, repeatAfter: Int = 0, quiet: QuietHours? = null
    ): Reminder? = reminders(deadline, offsets, repeatAfter, quiet).firstOrNull { it.at.isAfter(now) }

    /** The latest push that fell into (since, now], e.g. while the phone was off; null if none was missed. */
    fun missedReminder(
        deadline: LocalDateTime, offsets: List<Int>, since: LocalDateTime, now: LocalDateTime, repeatAfter: Int = 0, quiet: QuietHours? = null
    ): Reminder? = reminders(deadline, offsets, repeatAfter, quiet).lastOrNull { it.at.isAfter(since) && !it.at.isAfter(now) }

    /** True from the minute after the deadline on. */
    fun isMissed(deadline: LocalDateTime?, now: LocalDateTime): Boolean = deadline != null && !now.isBefore(deadline.plusMinutes(1))

    /** Points for ticking off at [doneAt]: [POINTS_ON_TIME] by the deadline, 0 when late or without a deadline. */
    fun points(deadline: LocalDateTime?, doneAt: LocalDateTime): Int =
        if (deadline != null && !isMissed(deadline, doneAt)) POINTS_ON_TIME else 0

    /**
     * "I did it on time but forgot to tick it off" is offered only for late tasks: open past the deadline
     * ([doneAt] null) or ticked off after it.
     */
    fun canClaimOnTime(deadline: LocalDateTime?, doneAt: LocalDateTime?, now: LocalDateTime): Boolean =
        deadline != null && points(deadline, doneAt ?: now) < POINTS_ON_TIME

    /** Old fixed reminder time as minutes before the deadline; null if it was not before it. */
    fun legacyOffset(dueEpochDay: Long, dueMinute: Int, reminderAt: Long, zone: ZoneId = ZoneId.systemDefault()): Int? {
        val due = deadline(dueEpochDay, dueMinute) ?: return null
        val at = Instant.ofEpochMilli(reminderAt).atZone(zone).toLocalDateTime()
        val minutes = java.time.Duration.between(at, due).toMinutes()
        return minutes.takeIf { it > 0 }?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
    }

    fun toLocal(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun toMillis(at: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long = at.atZone(zone).toInstant().toEpochMilli()
}
