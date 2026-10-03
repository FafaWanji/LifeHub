package com.example.lifeorganizer.calendar.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lifeorganizer.calendar.api.DirectionsApiService
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.alarm.AlarmScheduler
import com.example.lifeorganizer.calendar.data.Reminder
import com.example.lifeorganizer.calendar.data.SettingsManager
import com.example.lifeorganizer.calendar.debug.DebugLogger
import kotlinx.coroutines.flow.first

/**
 * Calculates how long the trip from the saved home address to the event takes and stores the
 * resulting departure time. With [KEY_SCHEDULE_ALARM] it also sets a "leave now" alarm
 * (smart alarm), which runs again shortly before the event to use live traffic.
 */
class TravelTimeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_EVENT_ID = "EVENT_ID"
        const val KEY_SCHEDULE_ALARM = "SCHEDULE_ALARM"
        private const val MAX_ATTEMPTS = 4
    }

    override suspend fun doWork(): Result {
        val eventId = inputData.getLong(KEY_EVENT_ID, -1L)
        if (eventId == -1L) return Result.failure()
        val scheduleAlarm = inputData.getBoolean(KEY_SCHEDULE_ALARM, true)

        val db = AppDatabase.getDatabase(applicationContext)
        val eventWithReminders = db.eventDao().getEventsWithRemindersSync().find { it.event.id == eventId }
            ?: return Result.failure()

        val event = eventWithReminders.event
        val destination = event.targetAddress?.takeIf { it.isNotBlank() } ?: return Result.success()
        if (event.startTimeMillis < System.currentTimeMillis()) return Result.success()

        val settingsManager = SettingsManager(applicationContext)
        // The Directions API can't resolve "my location" server-side, so a saved home address is required.
        val origin = settingsManager.homeAddress.first()?.takeIf { it.isNotBlank() } ?: run {
            DebugLogger.log("No home address saved – travel time for '${event.title}' can't be calculated.")
            return Result.success()
        }
        val travelMode = settingsManager.travelMode.first()

        val apiKey = com.example.lifeorganizer.calendar.BuildConfig.GOOGLE_MAPS_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_MAPS_API_KEY") {
            DebugLogger.log("Google Maps API key not configured. Skipping travel time check.")
            return Result.failure()
        }

        try {
            val targetArrivalTime = event.startTimeMillis - (event.arrivalBufferMinutes * 60 * 1000L)
            val api = DirectionsApiService.create()
            val response = api.getDirections(
                origin, destination,
                mode = travelMode,
                departureTime = if (travelMode == "transit") null else "now",
                arrivalTime = if (travelMode == "transit") targetArrivalTime / 1000 else null,
                apiKey = apiKey
            )

            if (response.status == "OK" && response.routes.isNotEmpty()) {
                val leg = response.routes[0].legs[0]
                val durationSeconds = leg.duration_in_traffic?.value ?: leg.duration.value
                val durationMinutes = durationSeconds / 60

                DebugLogger.log("API: Travel ($travelMode) from $origin to $destination is $durationMinutes mins")

                val departureTime = targetArrivalTime - (durationSeconds * 1000L)
                TravelInfoStore.put(
                    applicationContext, event.id,
                    TravelInfo(departureTime, durationMinutes.toLong(), travelMode, event.startTimeMillis)
                )

                if (scheduleAlarm) {
                    val finalAlarmTime = departureTime - (event.alarmLeadMinutes * 60 * 1000L)
                    val smartReminder = Reminder(
                        id = 999000 + event.id,
                        eventId = event.id,
                        reminderTimeMillis = finalAlarmTime,
                        type = "Smart Alarm ($durationMinutes min travel)",
                        timezone = event.timezone
                    )
                    AlarmScheduler(applicationContext).schedule(event, smartReminder)
                    DebugLogger.log("Smart Alarm set for: ${java.time.Instant.ofEpochMilli(finalAlarmTime)}")
                }
                return Result.success()
            }
            DebugLogger.log("Directions API status: ${response.status}")
        } catch (e: Exception) {
            DebugLogger.log("API Error: ${e.message}")
        }

        return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }
}
