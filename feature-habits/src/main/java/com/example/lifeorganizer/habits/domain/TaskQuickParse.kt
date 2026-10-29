package com.example.lifeorganizer.habits.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** A task typed into the quick-add field, split into title and deadline. */
data class QuickTask(val title: String, val dueEpochDay: Long, val dueMinute: Int)

/**
 * Reads a deadline from the end of a quick-add line (German and English), e.g.
 * "Aufgabe 5 morgen", "Exercise 5 fri 14:00", "Zahnarzt anrufen um 14 Uhr", "Report 24.10. 9:30".
 * Only the end of the line is looked at, so numbers inside the title ("Aufgabe 5") stay.
 * Without a date the deadline is today (23:59, or at the given time; a time already past means tomorrow).
 */
object TaskQuickParse {
    internal val weekdays = mapOf(
        "montag" to DayOfWeek.MONDAY, "mo" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "dienstag" to DayOfWeek.TUESDAY, "di" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
        "mittwoch" to DayOfWeek.WEDNESDAY, "mi" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "donnerstag" to DayOfWeek.THURSDAY, "do" to DayOfWeek.THURSDAY, "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY,
        "freitag" to DayOfWeek.FRIDAY, "fr" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "samstag" to DayOfWeek.SATURDAY, "sa" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sonntag" to DayOfWeek.SUNDAY, "so" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY
    )
    private val relative = mapOf(
        "heute" to 0L, "today" to 0L, "morgen" to 1L, "tomorrow" to 1L, "übermorgen" to 2L, "uebermorgen" to 2L
    )

    // Words that may stand before a date or time: "bis morgen", "am Freitag", "um 14 Uhr", "by fri", "at 9:30"
    private const val LEAD = """(?:\s+(?:bis|am|um|an|on|by|at|due))?"""
    // "14:00", "9:30 Uhr", "14.30 Uhr", "14 Uhr", "2pm", "11 am"
    private val timeRx = Regex(
        """(?i)$LEAD\s+(?:(\d{1,2}):(\d{2})(?:\s*uhr)?|(\d{1,2})\.(\d{2})\s*uhr|(\d{1,2})\s*uhr|(\d{1,2})(?::(\d{2}))?\s*(am|pm))$"""
    )
    // "24.10.", "24.10.2026", "24.10.26"
    private val dateRx = Regex("""(?i)$LEAD\s+(\d{1,2})\.(\d{1,2})\.(\d{2,4})?$""")
    private val wordRx = Regex("""(?i)$LEAD\s+(?:nächsten\s+|naechsten\s+|next\s+)?(\p{L}+)(\.?)$""")
    // Also everyday words ("Mach das so"), so they only count as a weekday with a dot: "So.", "Do."
    internal val ambiguous = setOf("so", "do")

    /** A time at the end of [text] (which starts with a space): the text before it and the minute of the day. */
    internal fun timeAtEnd(text: String): Pair<String, Int>? {
        val m = timeRx.find(text) ?: return null
        val g = m.groupValues
        val (h, min) = when {
            g[1].isNotEmpty() -> g[1].toInt() to g[2].toInt()
            g[3].isNotEmpty() -> g[3].toInt() to g[4].toInt()
            g[5].isNotEmpty() -> g[5].toInt() to 0
            else -> {
                val h12 = g[6].toInt()
                if (h12 !in 1..12) return null
                val pm = g[8].equals("pm", true)
                (h12 % 12 + if (pm) 12 else 0) to (g[7].toIntOrNull() ?: 0)
            }
        }
        if (h !in 0..23 || min !in 0..59) return null
        return text.substring(0, m.range.first) to h * 60 + min
    }

    /** [defaultMinute] is the deadline time when only a day (or nothing) is given; 23:59 unless set otherwise. */
    fun parse(text: String, now: LocalDateTime = LocalDateTime.now(), defaultMinute: Int = 23 * 60 + 59): QuickTask? {
        val input = text.trim().replace(Regex("\\s+"), " ")
        if (input.isEmpty()) return null
        // A leading space lets every pattern demand whitespace before the date or time
        var rest = " $input"
        var date: LocalDate? = null
        var minute: Int? = null

        fun takeTime(): Boolean {
            val (before, m) = timeAtEnd(rest) ?: return false
            minute = m
            rest = before
            return true
        }

        fun takeDate(): Boolean {
            val today = now.toLocalDate()
            dateRx.find(rest)?.let { m ->
                val year = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it }
                val d = runCatching { LocalDate.of(year ?: today.year, m.groupValues[2].toInt(), m.groupValues[1].toInt()) }.getOrNull()
                    ?: return false
                // Without a year, a date already past means next year
                date = if (year == null && d.isBefore(today)) d.plusYears(1) else d
                rest = rest.substring(0, m.range.first)
                return true
            }
            val m = wordRx.find(rest) ?: return false
            val word = m.groupValues[1].lowercase()
            if (word in ambiguous && m.groupValues[2].isEmpty()) return false
            date = relative[word]?.let { today.plusDays(it) }
                ?: weekdays[word]?.let { today.with(TemporalAdjusters.nextOrSame(it)) }
                ?: return false
            rest = rest.substring(0, m.range.first)
            return true
        }

        // Date and time in either order: "morgen 14 Uhr" or "14:00 tomorrow"
        if (takeTime()) takeDate() else if (takeDate()) takeTime()

        val title = rest.trim()
        // No date: today, or tomorrow if that time has already passed
        fun undated(minuteOfDay: Int): LocalDate = now.toLocalDate().let { today ->
            if (!today.atStartOfDay().plusMinutes(minuteOfDay.toLong()).isAfter(now)) today.plusDays(1) else today
        }
        // Nothing left as a title: the whole line is the title, e.g. "Freitag"
        if (title.isEmpty()) return QuickTask(input, undated(defaultMinute).toEpochDay(), defaultMinute)

        val time = minute ?: defaultMinute
        val day = date ?: undated(time)
        return QuickTask(title, day.toEpochDay(), time)
    }
}
