package com.example.lifeorganizer.calendar.util

import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.EventWithReminders
import com.example.lifeorganizer.calendar.data.Reminder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

object IcsImporter {

    fun parseIcs(content: String): List<EventWithReminders> {
        val events = mutableListOf<EventWithReminders>()
        val lines = content.replace("\r\n", "\n").split("\n")
        
        var inEvent = false
        var inAlarm = false
        
        var title = ""
        var description = ""
        var startTimeMillis: Long = 0
        var endTimeMillis: Long? = null
        var dateOnly = false
        var location: String? = null
        var rrule: String? = null
        var isBirthday = false
        var birthYear: Int? = null
        val reminders = mutableListOf<Reminder>()
        
        var alarmTriggerMinutes = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed == "BEGIN:VEVENT") {
                inEvent = true
                title = ""
                description = ""
                startTimeMillis = 0
                endTimeMillis = null
                dateOnly = false
                location = null
                rrule = null
                isBirthday = false
                birthYear = null
                reminders.clear()
                continue
            }

            if (trimmed == "END:VEVENT") {
                inEvent = false
                if (startTimeMillis > 0) {
                    val event = Event(
                        title = title,
                        description = description,
                        startTimeMillis = startTimeMillis,
                        endTimeMillis = endTimeMillis,
                        targetAddress = location,
                        recurrenceRule = rrule,
                        isBirthday = isBirthday,
                        birthYear = birthYear,
                        // All-day only when DTSTART is a plain date. A missing DTEND just means
                        // "no end time" (the old calendar omits it), not an all-day event.
                        isAllDay = isBirthday || dateOnly
                    )
                    events.add(EventWithReminders(event, reminders.toList(), null))
                }
                continue
            }

            if (!inEvent) continue

            if (trimmed == "BEGIN:VALARM") {
                inAlarm = true
                alarmTriggerMinutes = 0
                continue
            }
            if (trimmed == "END:VALARM") {
                inAlarm = false
                if (alarmTriggerMinutes > 0) {
                    val reminderMillis = startTimeMillis - (alarmTriggerMinutes * 60000L)
                    reminders.add(Reminder(
                        eventId = 0, // will be assigned on insert
                        reminderTimeMillis = reminderMillis,
                        type = "$alarmTriggerMinutes min before"
                    ))
                }
                continue
            }

            if (inAlarm) {
                if (trimmed.startsWith("TRIGGER:")) {
                    // e.g. TRIGGER:-PT15M
                    val trigger = trimmed.substringAfter("TRIGGER:")
                    val matcher = Pattern.compile("-PT(\\d+)M").matcher(trigger)
                    if (matcher.find()) {
                        alarmTriggerMinutes = matcher.group(1)?.toIntOrNull() ?: 0
                    }
                }
                continue
            }

            // Normal event properties
            when {
                trimmed.startsWith("SUMMARY:") -> title = unescapeIcsText(trimmed.substringAfter("SUMMARY:"))
                trimmed.startsWith("DESCRIPTION:") -> {
                    description = unescapeIcsText(trimmed.substringAfter("DESCRIPTION:"))
                    if (description.contains("Birthday")) isBirthday = true
                    val yearMatcher = Pattern.compile("Birth Year: (\\d{4})").matcher(description)
                    if (yearMatcher.find()) {
                        birthYear = yearMatcher.group(1)?.toIntOrNull()
                    }
                    description = description.replace("Birthday", "").replace(Regex("Birth Year: \\d{4}"), "").replace("\n\n", "\n").trim()
                }
                trimmed.startsWith("LOCATION:") -> location = unescapeIcsText(trimmed.substringAfter("LOCATION:"))
                trimmed.startsWith("CATEGORIES:") -> {
                    val categories = trimmed.substringAfter("CATEGORIES:")
                    if (categories.contains("Birthday", ignoreCase = true)) {
                        isBirthday = true
                    }
                }
                trimmed.startsWith("RRULE:") -> rrule = trimmed.substringAfter("RRULE:")
                trimmed.startsWith("DTSTART") -> {
                    val value = trimmed.substringAfter(":")
                    dateOnly = trimmed.contains("VALUE=DATE", ignoreCase = true) && !trimmed.contains("VALUE=DATE-TIME", ignoreCase = true) ||
                        value.trim().length == 8
                    startTimeMillis = parseIcsDateTime(value)
                }
                trimmed.startsWith("DTEND") -> {
                    val value = trimmed.substringAfter(":")
                    endTimeMillis = parseIcsDateTime(value)
                }
            }
        }

        return events
    }

    private fun parseIcsDateTime(value: String): Long {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            val ldt = LocalDateTime.parse(value, formatter)
            ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            // fallback for DATE-only formats like DTSTART;VALUE=DATE:20260708
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                val ldt = java.time.LocalDate.parse(value, formatter).atStartOfDay()
                ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
            } catch (ex: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun unescapeIcsText(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
}
