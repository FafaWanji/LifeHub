package com.example.lifeorganizer.calendar.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.lifeorganizer.calendar.data.EventWithReminders
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object IcsExporter {

    fun exportEvents(context: Context, events: List<EventWithReminders>): Uri? {
        val icsContent = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//PrivateCalendar//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")

            events.forEach { eventWithReminders ->
                val event = eventWithReminders.event
                appendLine("BEGIN:VEVENT")
                appendLine("UID:privatecal-${event.id}@privatecalendar.app")
                appendLine("DTSTAMP:${formatUtcDateTime(System.currentTimeMillis())}")
                appendLine("DTSTART:${formatUtcDateTime(event.startTimeMillis)}")
                event.endTimeMillis?.let {
                    appendLine("DTEND:${formatUtcDateTime(it)}")
                }
                appendLine("SUMMARY:${escapeIcsText(event.title)}")

                val description = buildString {
                    if (event.description.isNotBlank()) {
                        append(event.description)
                    }
                    if (event.isBirthday) {
                        if (event.description.isNotBlank()) append("\\n")
                        append("Birthday")
                        if (event.birthYear != null) {
                            append("\\nBirth Year: ${event.birthYear}")
                        }
                    }
                }
                if (description.isNotBlank()) {
                    appendLine("DESCRIPTION:${escapeIcsText(description)}")
                }

                if (!event.targetAddress.isNullOrBlank()) {
                    appendLine("LOCATION:${escapeIcsText(event.targetAddress)}")
                }
                if (event.isBirthday) {
                    appendLine("CATEGORIES:Birthday")
                }
                event.recurrenceRule?.let {
                    appendLine("RRULE:$it")
                }
                eventWithReminders.reminders.forEach { reminder ->
                    val minutesBefore = (event.startTimeMillis - reminder.reminderTimeMillis) / 60000
                    appendLine("BEGIN:VALARM")
                    appendLine("ACTION:DISPLAY")
                    appendLine("DESCRIPTION:${escapeIcsText(reminder.type)}")
                    appendLine("TRIGGER:-PT${minutesBefore}M")
                    appendLine("END:VALARM")
                }
                appendLine("END:VEVENT")
            }

            appendLine("END:VCALENDAR")
        }

        val file = File(context.cacheDir, "private_calendar_export.ics")
        FileWriter(file).use { it.write(icsContent) }

        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun formatUtcDateTime(millis: Long): String {
        return DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneId.of("UTC"))
            .format(Instant.ofEpochMilli(millis))
    }

    private fun escapeIcsText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    fun shareIcs(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Calendar"))
    }
}
