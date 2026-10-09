package com.example.lifeorganizer.core.smartadd

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Rule-based Smart Add without network or API key (German and English):
 * "morgen 15 Uhr Zahnarzt", "Fr 18:30 Kino bei Oma", "12.10. 9-11 Uhr Klausur", "tomorrow 3pm dentist".
 * Text with a date or time becomes an event, anything else a note.
 */
object OfflineParser {

    private val weekdays = mapOf(
        "montag" to DayOfWeek.MONDAY, "mo" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "dienstag" to DayOfWeek.TUESDAY, "di" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
        "mittwoch" to DayOfWeek.WEDNESDAY, "mi" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "donnerstag" to DayOfWeek.THURSDAY, "do" to DayOfWeek.THURSDAY, "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY,
        "freitag" to DayOfWeek.FRIDAY, "fr" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "samstag" to DayOfWeek.SATURDAY, "sa" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sonntag" to DayOfWeek.SUNDAY, "so" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY
    )
    private val relative = mapOf("heute" to 0L, "today" to 0L, "morgen" to 1L, "tomorrow" to 1L, "übermorgen" to 2L)

    private val dateRx = Regex("""(?<!\d)(\d{1,2})\.(\d{1,2})\.(\d{2,4})?(?!\d)""")
    private val isoRx = Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)""")
    // "15 Uhr", "15:30", "15.30 Uhr", "3pm", "9-11 Uhr", "15:00 bis 17:00"
    private val timeRx = Regex(
        """(?i)(?:um\s+|at\s+|ab\s+)?(\d{1,2})(?:[:.](\d{2}))?\s*(uhr|h|am|pm)?(?:\s*(?:-|–|bis|to|until)\s*(\d{1,2})(?:[:.](\d{2}))?\s*(uhr|h|am|pm)?)?(?![\d.])"""
    )
    private val filler = Regex("""(?i)(?<![\p{L}])(am|um|an|on|at|ab|den|dem|nächsten|naechsten|next)(?![\p{L}])""")

    fun parse(
        text: String,
        waypoints: List<Pair<String, String>> = emptyList(),
        now: LocalDateTime = LocalDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<SmartResult> {
        val input = text.trim()
        if (input.isEmpty()) return emptyList()
        // Single-line text with an amount is a transaction ("12,50 Döner gestern"); lists stay notes.
        if ('\n' !in input) MoneyTextParser.parse(input, now.toLocalDate())?.let { return listOf(it) }
        var rest = input
        var date: LocalDate? = null

        fun cut(range: IntRange) {
            rest = rest.removeRange(range).replace(Regex("\\s{2,}"), " ")
        }

        isoRx.find(rest)?.let { m ->
            date = runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull()
            if (date != null) cut(m.range)
        }
        if (date == null) dateRx.find(rest)?.let { m ->
            val year = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it }
            val d = runCatching { LocalDate.of(year ?: now.year, m.groupValues[2].toInt(), m.groupValues[1].toInt()) }.getOrNull()
            if (d != null) {
                // Without a year, a date already past means next year
                date = if (year == null && d.isBefore(now.toLocalDate())) d.plusYears(1) else d
                cut(m.range)
            }
        }
        if (date == null) {
            val words = Regex("""[\p{L}]+""").findAll(rest).toList()
            words.firstOrNull { it.value.lowercase() in relative }?.let { w ->
                date = now.toLocalDate().plusDays(relative.getValue(w.value.lowercase()))
                cut(w.range)
            } ?: words.firstOrNull { it.value.lowercase().trimEnd('.') in weekdays }?.let { w ->
                val day = weekdays.getValue(w.value.lowercase())
                date = now.toLocalDate().with(TemporalAdjusters.next(day))
                cut(w.range)
            }
        }

        var start: LocalTime? = null
        var end: LocalTime? = null
        timeRx.findAll(rest).firstOrNull { m ->
            // A bare number is only a time when it carries a unit, minutes or a range ("Raum 12" is not)
            m.groupValues[2].isNotEmpty() || m.groupValues[3].isNotEmpty() || m.groupValues[4].isNotEmpty()
        }?.let { m ->
            start = toTime(m.groupValues[1], m.groupValues[2], m.groupValues[3].ifEmpty { m.groupValues[6] })
            if (m.groupValues[4].isNotEmpty()) end = toTime(m.groupValues[4], m.groupValues[5], m.groupValues[6])
            if (start != null) cut(m.range)
        }

        if (date == null && start == null) return listOf(note(input))

        val day = date ?: now.toLocalDate().let { if (start!!.isBefore(now.toLocalTime())) it.plusDays(1) else it }
        val startTime = start ?: LocalTime.of(9, 0)
        val startAt = day.atTime(startTime)
        val endAt = end?.let { day.atTime(it).let { e -> if (e.isAfter(startAt)) e else e.plusDays(1) } } ?: startAt.plusHours(1)

        val title = rest.replace(filler, " ").replace(Regex("\\s{2,}"), " ").trim(' ', ',', '-', '.', ':')
            .replaceFirstChar { it.uppercase() }.ifBlank { input }
        val location = SmartAddEngine.resolveWaypointAddress(input, title, "", waypoints)
        return listOf(
            SmartResult.Event(
                title = title,
                description = "",
                location = location,
                startTimeMillis = startAt.atZone(zone).toInstant().toEpochMilli(),
                endTimeMillis = endAt.atZone(zone).toInstant().toEpochMilli()
            )
        )
    }

    private fun toTime(h: String, m: String, unit: String): LocalTime? {
        var hour = h.toIntOrNull() ?: return null
        val minute = m.toIntOrNull() ?: 0
        when (unit.lowercase()) {
            "pm" -> if (hour < 12) hour += 12
            "am" -> if (hour == 12) hour = 0
        }
        return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
    }

    private fun note(input: String): SmartResult.Note {
        val lines = input.lines()
        val body = lines.drop(1).joinToString("\n").trim()
        val isList = body.lines().count { it.trimStart().startsWith("-") || it.trimStart().startsWith("•") } >= 2
        val content = if (isList) body.lines().joinToString("\n") { l ->
            val t = l.trimStart().removePrefix("-").removePrefix("•").trim()
            if (t.isEmpty()) "" else "- [ ] $t"
        } else body
        return SmartResult.Note(title = lines.first().trim(), content = content, isChecklist = isList)
    }
}
