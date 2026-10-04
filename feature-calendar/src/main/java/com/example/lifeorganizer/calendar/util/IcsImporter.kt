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
        val exDates = mutableListOf<String>()
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
                exDates.clear()
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
                        exDates = exDates.takeIf { it.isNotEmpty() }?.distinct()?.joinToString(","),
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
                trimmed.startsWith("EXDATE") -> trimmed.substringAfter(":").split(',').forEach { v ->
                    // Local date of the skipped occurrence
                    val millis = parseIcsDateTime(v, tzid(trimmed))
                    exDates += java.time.Instant.ofEpochMilli(millis).atZone(
                        if (v.trim().length == 8) java.time.ZoneOffset.UTC else java.time.ZoneId.systemDefault()
                    ).toLocalDate().toString()
                }
                trimmed.startsWith("DTSTART") -> {
                    val value = trimmed.substringAfter(":")
                    dateOnly = trimmed.contains("VALUE=DATE", ignoreCase = true) && !trimmed.contains("VALUE=DATE-TIME", ignoreCase = true) ||
                        value.trim().length == 8
                    startTimeMillis = parseIcsDateTime(value, tzid(trimmed))
                }
                trimmed.startsWith("DTEND") -> {
                    val value = trimmed.substringAfter(":")
                    endTimeMillis = parseIcsDateTime(value, tzid(trimmed))
                }
            }
        }

        // Old exports contain every occurrence of a series, each with the RRULE – keep one per series.
        return SeriesDedup.collapse(events)
    }

    /** TZID parameter of a DTSTART/DTEND line, e.g. "DTSTART;TZID=Europe/Berlin:20260708T120000". */
    private fun tzid(line: String): ZoneId? =
        Regex("TZID=([^;:]+)").find(line.substringBefore(":"))?.groupValues?.get(1)
            ?.let { runCatching { ZoneId.of(it.trim('"')) }.getOrNull() }

    private fun parseIcsDateTime(value: String, zone: ZoneId? = null): Long {
        val v = value.trim()
        // Local time ("floating" or with TZID): interpret in that zone, else the device zone
        if (v.length == 15 && v[8] == 'T') {
            runCatching {
                val ldt = LocalDateTime.parse(v, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                return ldt.atZone(zone ?: ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            val ldt = LocalDateTime.parse(v, formatter)
            ldt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            // fallback for DATE-only formats like DTSTART;VALUE=DATE:20260708
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                val ldt = java.time.LocalDate.parse(v, formatter).atStartOfDay()
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
