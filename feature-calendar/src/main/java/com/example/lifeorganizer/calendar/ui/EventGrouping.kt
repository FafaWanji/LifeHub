package com.example.lifeorganizer.calendar.ui

import com.example.lifeorganizer.calendar.data.EventWithReminders
import com.example.lifeorganizer.core.util.Places

/** One row of a day list: a single event or several events at the same place. */
sealed interface DayEntry {
    val firstStart: Long

    data class Single(val event: EventWithReminders) : DayEntry {
        override val firstStart get() = event.event.startTimeMillis
    }

    data class PlaceGroup(val place: String, val events: List<EventWithReminders>) : DayEntry {
        override val firstStart get() = events.first().event.startTimeMillis
    }
}

object EventGrouping {

    /**
     * Groups events of one day that happen at the same place (e.g. three appointments in the
     * same clinic) into one entry, placed at the time of the first of them.
     */
    fun byPlace(dayEvents: List<EventWithReminders>): List<DayEntry> {
        val sorted = dayEvents.sortedBy { it.event.startTimeMillis }
        val byKey = sorted.groupBy { Places.key(it.event.targetAddress) }
        val emitted = mutableSetOf<String>()
        return sorted.mapNotNull { e ->
            val key = Places.key(e.event.targetAddress)
            val group = key?.let { byKey[it] }
            when {
                key == null || group == null || group.size < 2 -> DayEntry.Single(e)
                emitted.add(key) -> DayEntry.PlaceGroup(Places.clean(e.event.targetAddress)!!, group)
                else -> null // already part of an emitted group
            }
        }
    }

    /**
     * A repeating event is shown once – at its next occurrence – instead of one entry per week
     * for years. One-time events are kept as they are.
     */
    fun collapseSeries(events: List<EventWithReminders>, now: Long): List<EventWithReminders> {
        val (series, single) = events.partition { !it.event.recurrenceRule.isNullOrBlank() }
        val next = series.groupBy { it.event.id }.values.mapNotNull { occurrences ->
            occurrences.sortedBy { it.event.startTimeMillis }
                .firstOrNull { (it.event.endTimeMillis ?: (it.event.startTimeMillis + 3_600_000)) > now }
        }
        return (single + next).sortedBy { it.event.startTimeMillis }
    }
}

/** "Weekly", "Every 2 weeks", … for an RRULE, or null for one-time events. */
fun recurrenceLabel(rule: String?, lang: String): String? {
    if (rule.isNullOrBlank()) return null
    val freq = Regex("FREQ=([A-Z]+)").find(rule)?.groupValues?.get(1)
    val interval = Regex("INTERVAL=(\\d+)").find(rule)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    return when {
        freq == "WEEKLY" && interval == 2 -> Translations.get(TransKey.BIWEEKLY, lang)
        freq == "DAILY" -> Translations.get(TransKey.DAILY, lang)
        freq == "WEEKLY" -> Translations.get(TransKey.WEEKLY, lang)
        freq == "MONTHLY" -> Translations.get(TransKey.MONTHLY, lang)
        freq == "YEARLY" -> Translations.get(TransKey.YEARLY, lang)
        else -> Translations.get(TransKey.RECURRENCE, lang)
    }
}
