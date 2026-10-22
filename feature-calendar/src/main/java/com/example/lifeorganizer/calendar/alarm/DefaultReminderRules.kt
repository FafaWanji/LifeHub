package com.example.lifeorganizer.calendar.alarm

import com.example.lifeorganizer.core.util.Places
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One event occurrence, as far as the default reminder is concerned. */
data class Occurrence(
    val eventId: Long,
    val title: String,
    /** Start of this occurrence, epoch millis. */
    val start: Long,
    val place: String?,
    val allDay: Boolean = false,
    val birthday: Boolean = false,
    /** The event has reminders of its own; then the default one is not added. */
    val hasOwnReminders: Boolean = false
)

/**
 * The default reminder (2 hours before unless set otherwise): every timed event without a reminder of its own gets
 * it – except the later events of a place group, i.e. several events at the same place on one day. Only the first
 * of those reminds; you are already there for the rest. Free of Android so it can be unit-tested.
 */
object DefaultReminderRules {

    /** Occurrences that get the default reminder, with the time it is due (epoch millis). [offsetMinutes] < 0 = off. */
    fun due(occurrences: List<Occurrence>, offsetMinutes: Int, zone: ZoneId = ZoneId.systemDefault()): List<Pair<Occurrence, Long>> {
        if (offsetMinutes < 0) return emptyList()
        val timed = occurrences.filter { !it.allDay && !it.birthday }
        // First event of each place on each day (by start, then id so equal starts are stable)
        val firstOfGroup = timed.filter { Places.key(it.place) != null }
            .groupBy { day(it.start, zone) to Places.key(it.place) }
            .values.map { group -> group.minWith(compareBy<Occurrence>({ it.start }, { it.eventId })) }
            .toSet()
        return timed
            .filter { !it.hasOwnReminders }
            .filter { Places.key(it.place) == null || it in firstOfGroup }
            .map { it to it.start - offsetMinutes * 60_000L }
            .sortedBy { it.second }
    }

    private fun day(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
