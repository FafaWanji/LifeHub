package com.example.lifeorganizer.habits.ui

import com.example.lifeorganizer.core.i18n.durationLabel
import com.example.lifeorganizer.core.i18n.localDateFormatter
import java.time.LocalDate
import java.time.LocalDateTime

/** "15 min", "1 h", "1 h 30 min", "1 day", "2 days", "1 week" for a reminder offset in minutes. */
fun offsetLabel(minutes: Int, lang: String): String = durationLabel(minutes, lang)

/** "Today 14:00", "Tomorrow 23:59" or "18 Oct 09:30". */
fun deadlineLabel(deadline: LocalDateTime, today: LocalDate, lang: String): String {
    val day = when (deadline.toLocalDate().toEpochDay() - today.toEpochDay()) {
        0L -> HabitsStr.today.of(lang)
        1L -> HabitsStr.tomorrow.of(lang)
        // Day and month in the order of the language (or the one chosen in the settings)
        else -> deadline.toLocalDate().format(localDateFormatter(lang, if (deadline.year != today.year) "yyyyMMMd" else "MMMd"))
    }
    return "$day ${formatTime(deadline.hour * 60 + deadline.minute)}"
}
