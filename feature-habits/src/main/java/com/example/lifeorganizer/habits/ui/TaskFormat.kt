package com.example.lifeorganizer.habits.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "15 min", "1 h", "1 h 30 min", "1 day", "2 days", "1 week" for a reminder offset in minutes. */
fun offsetLabel(minutes: Int, lang: String): String = when {
    minutes >= 7 * 1440 && minutes % (7 * 1440) == 0 ->
        (minutes / (7 * 1440)).let { if (it == 1) HabitsStr.oneWeek.of(lang) else HabitsStr.weeks.of(lang).format(it) }
    minutes >= 1440 && minutes % 1440 == 0 ->
        (minutes / 1440).let { if (it == 1) HabitsStr.oneDay.of(lang) else HabitsStr.days.of(lang).format(it) }
    minutes >= 60 && minutes % 60 == 0 -> HabitsStr.hourShort.of(lang).format(minutes / 60)
    minutes > 60 -> HabitsStr.hourShort.of(lang).format(minutes / 60) + " " + HabitsStr.minShort.of(lang).format(minutes % 60)
    else -> HabitsStr.minShort.of(lang).format(minutes)
}

/** "Today 14:00", "Tomorrow 23:59" or "18 Oct 09:30". */
fun deadlineLabel(deadline: LocalDateTime, today: LocalDate, lang: String): String {
    val day = when (deadline.toLocalDate().toEpochDay() - today.toEpochDay()) {
        0L -> HabitsStr.today.of(lang)
        1L -> HabitsStr.tomorrow.of(lang)
        else -> deadline.toLocalDate().format(
            DateTimeFormatter.ofPattern(
                when {
                    deadline.year != today.year -> if (lang == "en") "d MMM yyyy" else "d. MMM yyyy"
                    lang == "en" -> "d MMM"
                    else -> "d. MMM"
                },
                Locale.forLanguageTag(lang)
            )
        )
    }
    return "$day ${formatTime(deadline.hour * 60 + deadline.minute)}"
}
