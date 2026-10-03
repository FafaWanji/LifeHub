package com.example.lifeorganizer.calendar.worker

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Result of the last travel-time calculation for an event. */
data class TravelInfo(
    val departureMillis: Long,
    val durationMinutes: Long,
    val mode: String,
    val eventStartMillis: Long
)

/**
 * Small persistent cache of "when do I have to leave" per event, written by [TravelTimeWorker]
 * and shown on event cards. Exposed as a flow so cards update as soon as a calculation finishes.
 */
object TravelInfoStore {
    private const val PREFS = "travel_info"
    private val state = MutableStateFlow<Map<Long, TravelInfo>>(emptyMap())
    private var loaded = false

    fun observe(context: Context): StateFlow<Map<Long, TravelInfo>> {
        ensureLoaded(context)
        return state.asStateFlow()
    }

    fun put(context: Context, eventId: Long, info: TravelInfo) {
        ensureLoaded(context)
        prefs(context).edit()
            .putString(eventId.toString(), "${info.departureMillis};${info.durationMinutes};${info.mode};${info.eventStartMillis}")
            .apply()
        state.value = state.value + (eventId to info)
    }

    fun remove(context: Context, eventId: Long) {
        ensureLoaded(context)
        prefs(context).edit().remove(eventId.toString()).apply()
        state.value = state.value - eventId
    }

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        val now = System.currentTimeMillis()
        val editor = prefs(context).edit()
        state.value = prefs(context).all.mapNotNull { (key, value) ->
            val parts = (value as? String)?.split(";") ?: return@mapNotNull null
            val info = runCatching {
                TravelInfo(parts[0].toLong(), parts[1].toLong(), parts[2], parts[3].toLong())
            }.getOrNull()
            // Drop entries for events that are long over.
            if (info == null || info.eventStartMillis < now - 86_400_000L) {
                editor.remove(key)
                null
            } else key.toLong() to info
        }.toMap()
        editor.apply()
        loaded = true
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
