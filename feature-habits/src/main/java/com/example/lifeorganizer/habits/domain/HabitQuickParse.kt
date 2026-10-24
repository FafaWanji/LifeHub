package com.example.lifeorganizer.habits.domain

import java.time.DayOfWeek

/** A habit typed into the quick-add field: title, weekday mask and optional reminder time. */
data class QuickHabit(val title: String, val weekdays: Int, val reminderMinutes: Int?)

/**
 * Reads a repeat from the end of a quick-add line (German and English), e.g. "Sport täglich 7 Uhr",
 * "Lesen jeden Tag", "Gym Mo Mi Fr 18:00", "Run every mon and thu", "Joggen montags", "Vokabeln Mo-Fr",
 * "Putzen am Wochenende". A single day without "jeden"/"every" stays a task deadline ("Bericht Fr").
 */
object HabitQuickParse {
    private val MON_FRI = (1..5).fold(0) { m, d -> m or HabitSchedule.bit(DayOfWeek.of(d)) }
    private val WEEKEND = HabitSchedule.bit(DayOfWeek.SATURDAY) or HabitSchedule.bit(DayOfWeek.SUNDAY)

    // "montags", "mondays": always a repeat, even on its own
    private val adverbs = TaskQuickParse.weekdays.filterKeys { it.length > 3 }.mapKeys { (k, _) -> k + "s" }
    private val days = TaskQuickParse.weekdays + adverbs
    private val day = days.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
    private val dayRx = Regex("""(?i)(?<!\p{L})($day)(?!\p{L})""")
    private const val SEP = """(?:\s*[,/&+]\s*|\s+(?:und|and|u\.)\s+|\s+)"""
    private const val REPEAT = """jeden|jede|jedes|immer|alle|every|each"""

    private val simpleRx = Regex(
        """(?i)\s+(?:(täglich|taeglich|jeden\s+tag|alle\s+tage|daily|every\s*day|each\s+day)""" +
            """|(werktags|wochentags|an\s+werktagen|unter\s+der\s+woche|(?:on\s+|every\s+)?weekdays|every\s+weekday)""" +
            """|(am\s+wochenende|wochenends|jedes\s+wochenende|(?:on\s+|every\s+)?weekends|every\s+weekend))$"""
    )
    private val rangeRx = Regex("""(?i)\s+(?:(?:$REPEAT)\s+)?($day)\.?\s*(?:-|–|bis|to|through)\s*($day)\.?$""")
    private val listRx = Regex("""(?i)\s+(?:($REPEAT|am|on)\s+)?((?:$day)\.?(?:$SEP(?:$day)\.?)*)$""")

    /**
     * Null when the line has no repeat, so it is a task. With [repeatNeeded] false (the habits tab) a
     * line without one is a daily habit.
     */
    fun parse(text: String, repeatNeeded: Boolean = true): QuickHabit? {
        val input = text.trim().replace(Regex("\\s+"), " ")
        if (input.isEmpty()) return null
        var rest = " $input"
        var minute: Int? = null
        fun takeTime() = TaskQuickParse.timeAtEnd(rest)?.let { (before, m) -> rest = before; minute = m }

        // Time and repeat in either order: "täglich 7 Uhr" or "um 7 Uhr jeden Tag"
        takeTime()
        val repeat = repeatAtEnd(rest)
        if (repeat != null) {
            rest = repeat.first
            if (minute == null) takeTime()
        } else if (repeatNeeded) return null

        val title = rest.trim()
        // "täglich" on its own: rather a task called that than a habit without a name
        if (title.isEmpty()) return if (repeatNeeded) null else QuickHabit(input, HabitSchedule.EVERY_DAY, null)
        return QuickHabit(title, repeat?.second ?: HabitSchedule.EVERY_DAY, minute)
    }

    /** The text before the repeat and its weekday mask. */
    private fun repeatAtEnd(text: String): Pair<String, Int>? {
        simpleRx.find(text)?.let { m ->
            val mask = when {
                m.groupValues[1].isNotEmpty() -> HabitSchedule.EVERY_DAY
                m.groupValues[2].isNotEmpty() -> MON_FRI
                else -> WEEKEND
            }
            return text.substring(0, m.range.first) to mask
        }
        rangeRx.find(text)?.let { m ->
            val from = days.getValue(m.groupValues[1].lowercase()).value
            val to = days.getValue(m.groupValues[2].lowercase()).value
            // "Fr-Mo" goes round the weekend
            var mask = 0
            var d = from
            while (true) {
                mask = mask or HabitSchedule.bit(DayOfWeek.of(d))
                if (d == to) break
                d = d % 7 + 1
            }
            return text.substring(0, m.range.first) to mask
        }
        val m = listRx.find(text) ?: return null
        val list = m.groupValues[2]
        val found = dayRx.findAll(list).map { it.value.lowercase() }.toList()
        // "Mach das immer so": "so"/"do" alone only count as a day with a dot
        if (found.size == 1 && found[0] in TaskQuickParse.ambiguous && !list.endsWith(".")) return null
        val repeatWord = m.groupValues[1].isNotEmpty() && !m.groupValues[1].lowercase().let { it == "am" || it == "on" }
        // One plain day ("Bericht Fr", "am Freitag") is a deadline; a list, "jeden"/"every" or "montags" is a repeat
        if (found.size < 2 && !repeatWord && found.none { it in adverbs }) return null
        return text.substring(0, m.range.first) to found.fold(0) { mask, w -> mask or HabitSchedule.bit(days.getValue(w)) }
    }
}
