package com.example.lifeorganizer.waypoints.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_ACCENT = stringPreferencesKey("theme_accent")
        val CARD_SCALE = floatPreferencesKey("card_scale")
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val USE_HOME_AS_ORIGIN = booleanPreferencesKey("use_home_as_origin")
        val TRANSPORT_MODE = stringPreferencesKey("transport_mode")
        val HIDE_ADDRESSES = booleanPreferencesKey("hide_addresses")
    }

    val themeStyle: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_STYLE] ?: "pastel"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "light"
    }

    val themeAccent: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_ACCENT] ?: "blush_pink"
    }

    val cardScale: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[CARD_SCALE] ?: 1.0f
    }
    
    // Home address, "start from home" and travel mode are shared with the calendar
    // (core SettingsManager), so both modules always use the same values.
    private val shared = com.example.lifeorganizer.core.settings.SettingsManager(context)

    val homeAddress: Flow<String> = shared.homeAddress.map { it.orEmpty() }

    val useHomeAsOrigin: Flow<Boolean> = shared.useHomeAsOrigin

    /** Waypoints speaks "car" / "bike" / "transit", the calendar "driving" / "bicycling" / "transit". */
    val transportMode: Flow<String> = shared.travelMode.map { calendarToWaypointMode(it) }

    val hideAddresses: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_ADDRESSES] ?: false
    }

    suspend fun saveThemeStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_STYLE] = style
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun saveThemeAccent(accent: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_ACCENT] = accent
        }
    }

    suspend fun saveCardScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[CARD_SCALE] = scale
        }
    }
    
    suspend fun saveHomeAddress(address: String) {
        shared.saveHomeAddress(address.trim())
    }

    suspend fun saveUseHomeAsOrigin(useHome: Boolean) {
        shared.saveUseHomeAsOrigin(useHome)
    }

    suspend fun saveTransportMode(mode: String) {
        shared.saveTravelMode(waypointToCalendarMode(mode))
    }

    /**
     * One-time move of values saved by the old standalone Waypoints app: they win only
     * where the calendar has nothing yet, then the legacy keys are removed.
     */
    suspend fun migrateLegacyNavigationSettings() {
        val legacy = context.dataStore.data.first()
        val legacyHome = legacy[HOME_ADDRESS]
        if (!legacyHome.isNullOrBlank() && shared.homeAddress.first().isNullOrBlank()) {
            shared.saveHomeAddress(legacyHome.trim())
        }
        legacy[USE_HOME_AS_ORIGIN]?.let { if (it) shared.saveUseHomeAsOrigin(true) }
        legacy[TRANSPORT_MODE]?.let { mode ->
            if (mode != "d") shared.saveTravelMode(waypointToCalendarMode(mode))
        }
        context.dataStore.edit {
            it.remove(HOME_ADDRESS)
            it.remove(USE_HOME_AS_ORIGIN)
            it.remove(TRANSPORT_MODE)
        }
    }
    
    suspend fun saveHideAddresses(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_ADDRESSES] = hide
        }
    }
}

internal fun calendarToWaypointMode(mode: String) = when (mode) {
    "bicycling" -> "bike"
    "transit" -> "transit"
    else -> "car"
}

internal fun waypointToCalendarMode(mode: String) = when (mode) {
    "bike", "bicycling" -> "bicycling"
    "transit" -> "transit"
    else -> "driving"
}
