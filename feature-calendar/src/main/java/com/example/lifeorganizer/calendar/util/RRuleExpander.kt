package com.example.lifeorganizer.calendar.util

import com.example.lifeorganizer.calendar.data.EventWithReminders
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

object RRuleExpander {
    /**
     * Expands a recurring event within a specific date range.
     * Generates virtual EventWithReminders for each occurrence.
     */
    fun expand(
        eventWithReminders: EventWithReminders,
        rangeStart: LocalDate,
        rangeEnd: LocalDate
    ): List<EventWithReminders> {
        val rule = eventWithReminders.event.recurrenceRule ?: return listOf(eventWithReminders)
        if (rule.isBlank()) return listOf(eventWithReminders)

        val zoneId = ZoneId.of(eventWithReminders.event.timezone)
        val originalStartInstant = Instant.ofEpochMilli(eventWithReminders.event.startTimeMillis)
        val originalStartDateTime = LocalDateTime.ofInstant(originalStartInstant, zoneId)
        val originalStartDate = originalStartDateTime.toLocalDate()
        val originalStartTime = originalStartDateTime.toLocalTime()

        // If the range ends before the event starts, return empty
        if (rangeEnd.isBefore(originalStartDate)) return emptyList()

        val params = parseRRule(rule)
        val freq = params["FREQ"] ?: return listOf(eventWithReminders)
        val interval = params["INTERVAL"]?.toIntOrNull() ?: 1
        
        // We do not support complex UNTIL/COUNT in this basic implementation, 
        // but we limit instances to the query range.
        var currentDate = originalStartDate
        
        // Find the first date on or after rangeStart that matches the rule
        val instances = mutableListOf<EventWithReminders>()
        var count = 0
        val maxInstances = 500 // Safety limit

        when (freq) {
            "DAILY" -> {
                while (currentDate.isBefore(rangeStart) && currentDate.isBefore(rangeEnd)) {
                    currentDate = currentDate.plusDays(interval.toLong())
                }
                while (!currentDate.isAfter(rangeEnd) && count < maxInstances) {
                    if (!currentDate.isBefore(rangeStart)) {
                        instances.add(createVirtualEvent(eventWithReminders, currentDate, originalStartTime, zoneId))
                    }
                    currentDate = currentDate.plusDays(interval.toLong())
                    count++
                }
            }
            "WEEKLY" -> {
                val byDayStr = params["BYDAY"]
                val targetDays = if (byDayStr != null) {
                    parseByDay(byDayStr)
                } else {
                    listOf(originalStartDate.dayOfWeek)
                }

                // Advance to start of the week of rangeStart
                while (currentDate.plusWeeks(interval.toLong()).isBefore(rangeStart)) {
                    currentDate = currentDate.plusWeeks(interval.toLong())
                }

                while (!currentDate.isAfter(rangeEnd) && count < maxInstances) {
                    // For the current week, find matching days
                    for (day in targetDays) {
                        val instanceDate = currentDate.with(TemporalAdjusters.nextOrSame(day))
                        // We must ensure that instanceDate belongs to the exact week interval from originalStartDate
                        // This logic is simplified for WEEKLY without BYDAY. 
                        // For complex BYDAY, we just yield all matching days in the current week.
                        if (!instanceDate.isBefore(originalStartDate) && !instanceDate.isBefore(rangeStart) && !instanceDate.isAfter(rangeEnd)) {
                            instances.add(createVirtualEvent(eventWithReminders, instanceDate, originalStartTime, zoneId))
                            count++
                        }
                    }
                    currentDate = currentDate.plusWeeks(interval.toLong())
                }
            }
            "MONTHLY" -> {
                while (currentDate.plusMonths(interval.toLong()).isBefore(rangeStart)) {
                    currentDate = currentDate.plusMonths(interval.toLong())
                }
                while (!currentDate.isAfter(rangeEnd) && count < maxInstances) {
                    if (!currentDate.isBefore(rangeStart)) {
                        instances.add(createVirtualEvent(eventWithReminders, currentDate, originalStartTime, zoneId))
                    }
                    currentDate = currentDate.plusMonths(interval.toLong())
                    count++
                }
            }
            "YEARLY" -> {
                while (currentDate.plusYears(interval.toLong()).isBefore(rangeStart)) {
                    currentDate = currentDate.plusYears(interval.toLong())
                }
                while (!currentDate.isAfter(rangeEnd) && count < maxInstances) {
                    if (!currentDate.isBefore(rangeStart)) {
                        instances.add(createVirtualEvent(eventWithReminders, currentDate, originalStartTime, zoneId))
                    }
                    currentDate = currentDate.plusYears(interval.toLong())
                    count++
                }
            }
        }

        return instances
    }

    private fun parseRRule(rule: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parts = rule.removePrefix("RRULE:").split(";")
        for (part in parts) {
            val kv = part.split("=")
            if (kv.size == 2) {
                map[kv[0].uppercase()] = kv[1].uppercase()
            }
        }
        return map
    }

    private fun parseByDay(byDay: String): List<DayOfWeek> {
        val days = mutableListOf<DayOfWeek>()
        val parts = byDay.split(",")
        for (part in parts) {
            val dayStr = part.replace(Regex("[^A-Z]"), "")
            when (dayStr) {
                "MO" -> days.add(DayOfWeek.MONDAY)
                "TU" -> days.add(DayOfWeek.TUESDAY)
                "WE" -> days.add(DayOfWeek.WEDNESDAY)
                "TH" -> days.add(DayOfWeek.THURSDAY)
                "FR" -> days.add(DayOfWeek.FRIDAY)
                "SA" -> days.add(DayOfWeek.SATURDAY)
                "SU" -> days.add(DayOfWeek.SUNDAY)
            }
        }
        return days
    }

    private fun createVirtualEvent(
        original: EventWithReminders,
        newDate: LocalDate,
        newTime: java.time.LocalTime,
        zoneId: ZoneId
    ): EventWithReminders {
        val newStartMillis = LocalDateTime.of(newDate, newTime).atZone(zoneId).toInstant().toEpochMilli()
        val offset = newStartMillis - original.event.startTimeMillis
        val newEndMillis = original.event.endTimeMillis?.let { it + offset }
        
        // Update reminders offset as well
        val newReminders = original.reminders.map {
            it.copy(
                id = 0, // Virtual reminders don't have DB IDs
                reminderTimeMillis = it.reminderTimeMillis + offset
            )
        }
        
        return original.copy(
            event = original.event.copy(
                id = original.event.id, // Keep the same ID so we know it's the same event
                startTimeMillis = newStartMillis,
                endTimeMillis = newEndMillis
            ),
            reminders = newReminders
        )
    }
}
