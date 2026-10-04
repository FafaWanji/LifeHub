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
            CalendarContract.Instances.EVENT_TIMEZONE
        )
        val result = mutableListOf<EventWithReminders>()
        runCatching {
            context.contentResolver.query(uri, projection, "${CalendarContract.Instances.VISIBLE} = 1", null, null)
        }.getOrNull()?.use { c ->
            while (c.moveToNext()) {
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
