package com.example.lifeorganizer.calendar.device

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.EventWithReminders
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Read-only view of the phone's calendars (Google, Samsung, Exchange …) via CalendarContract.
 * Their events get negative ids so the app never tries to store, edit or delete them.
 */
object DeviceCalendar {
    private const val PREFS = "device_calendar"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    private const val UID_PREFIX = "lifeorganizer-"

    fun hasWritePermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Calendars the user may add events to (id to "name (account)"). */
    fun writableCalendars(context: Context): List<Pair<Long, String>> {
        if (!hasPermission(context) || !hasWritePermission(context)) return emptyList()
        val out = mutableListOf<Pair<Long, String>>()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME),
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR} AND ${CalendarContract.Calendars.VISIBLE} = 1",
                null, null
            )
        }.getOrNull()?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1).orEmpty()
                val account = c.getString(2).orEmpty()
                out += c.getLong(0) to if (account.isNotBlank() && account != name) "$name ($account)" else name
            }
        }
        return out
    }

    /**
     * Copies an app event into a phone calendar (one-way). Returns the new event id or null.
     * Repetition and skipped dates are carried over; reminders become a 15-minute alert if any existed.
     */
    fun copyToDevice(context: Context, calendarId: Long, e: EventWithReminders): Long? {
        val ev = e.event
        val values = android.content.ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, ev.title)
            put(CalendarContract.Events.DESCRIPTION, ev.description)
            ev.targetAddress?.takeIf { it.isNotBlank() }?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
            put(CalendarContract.Events.UID_2445, "$UID_PREFIX${ev.id}-${System.currentTimeMillis()}")
            val zone = runCatching { ZoneId.of(ev.timezone) }.getOrDefault(ZoneId.systemDefault())
            var start = ev.startTimeMillis
            var end = ev.endTimeMillis ?: (start + 3_600_000L)
            if (ev.isAllDay) {
                // All-day events are stored at UTC midnight
                val day = java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
                start = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            }
            put(CalendarContract.Events.DTSTART, start)
            val rule = ev.recurrenceRule?.takeIf { it.isNotBlank() }
            if (rule != null) {
                put(CalendarContract.Events.RRULE, rule)
                // Repeating events need a duration instead of an end
                put(CalendarContract.Events.DURATION, if (ev.isAllDay) "P1D" else "PT${(end - start) / 60000}M")
                val skipped = com.example.lifeorganizer.calendar.util.SeriesRules.exDates(ev)
                if (skipped.isNotEmpty()) {
                    val time = java.time.Instant.ofEpochMilli(ev.startTimeMillis).atZone(zone).toLocalTime()
                    put(CalendarContract.Events.EXDATE, skipped.sorted().joinToString(",") { d ->
                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                            .format(d.atTime(time).atZone(zone).withZoneSameInstant(ZoneOffset.UTC))
                    })
                }
            } else {
                put(CalendarContract.Events.DTEND, end)
            }
        }
        val uri = runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }.getOrNull() ?: return null
        val id = ContentUris.parseId(uri)
        if (e.reminders.isNotEmpty()) {
            runCatching {
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, android.content.ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, id)
                    put(CalendarContract.Reminders.MINUTES, ((ev.startTimeMillis - e.reminders.minOf { it.reminderTimeMillis }) / 60000).coerceAtLeast(0))
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                })
            }
        }
        return id
    }

    /** True for events that come from the phone's calendars. */
    fun isDeviceEvent(e: Event) = e.id < 0

    /** All instances (recurrences already expanded by Android) between [from] and [to]. */
    fun load(context: Context, from: LocalDate, to: LocalDate): List<EventWithReminders> {
        if (!isEnabled(context) || !hasPermission(context)) return emptyList()
        val zone = ZoneId.systemDefault()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from.atStartOfDay(zone).toInstant().toEpochMilli())
            ContentUris.appendId(it, to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.EVENT_TIMEZONE,
            CalendarContract.Instances.UID_2445
        )
        val result = mutableListOf<EventWithReminders>()
        runCatching {
            context.contentResolver.query(uri, projection, "${CalendarContract.Instances.VISIBLE} = 1", null, null)
        }.getOrNull()?.use { c ->
            while (c.moveToNext()) {
                // Copies made by this app are already shown as the app's own events
                if (c.getString(9)?.startsWith(UID_PREFIX) == true) continue
                val allDay = c.getInt(5) == 1
                var begin = c.getLong(3)
                var end = c.getLong(4)
                if (allDay) {
                    // All-day instances are stored at UTC midnight; move them to local midnight.
                    begin = java.time.Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                    end = java.time.Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                }
                result += EventWithReminders(
                    Event(
                        id = -c.getLong(0),
                        title = c.getString(1).orEmpty(),
                        description = c.getString(2).orEmpty(),
                        startTimeMillis = begin,
                        endTimeMillis = end.takeIf { it > begin },
                        isAllDay = allDay,
                        targetAddress = c.getString(6)?.takeIf { it.isNotBlank() },
                        color = c.getInt(7),
                        timezone = zone.id
                    ),
                    emptyList()
                )
            }
        }
        return result
    }
}
