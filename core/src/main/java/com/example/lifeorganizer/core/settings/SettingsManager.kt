package com.example.lifeorganizer.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifeorganizer_settings")

class SettingsManager(private val context: Context) {
    companion object {
        // Calendar settings
        private val HOME_ADDRESS = stringPreferencesKey("home_address")
        private val TRAVEL_MODE = stringPreferencesKey("travel_mode")
        private val USE_HOME_AS_ORIGIN = booleanPreferencesKey("use_home_as_origin")
        private val USE_BROWSER_FOR_MAPS = booleanPreferencesKey("use_browser_for_maps")
        private val NOTIFICATION_SOUND_URI = stringPreferencesKey("notification_sound_uri")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val ENABLE_SMART_ALARMS = booleanPreferencesKey("enable_smart_alarms")
        private val USE_SYSTEM_ALARM = booleanPreferencesKey("use_system_alarm")

        // App-wide settings
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
        private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val ENABLE_APP_LOCK = booleanPreferencesKey("enable_app_lock")
        private val ACCENT_COLOR = intPreferencesKey("accent_color")
        private val DESIGN_STYLE = stringPreferencesKey("design_style")
    }

    // Theme & Appearance
    val accentColor: Flow<Int?> = context.dataStore.data.map { it[ACCENT_COLOR] }
    val designStyle: Flow<String> = context.dataStore.data.map { it[DESIGN_STYLE] ?: "pastel" }
    val isDarkTheme: Flow<Boolean?> = context.dataStore.data.map { it[IS_DARK_THEME] }
    val languageCode: Flow<String> = context.dataStore.data.map { it[LANGUAGE_CODE] ?: defaultLanguage() }

    /** Falls back to the device language when it is one of the supported ones. */
    private fun defaultLanguage(): String {
        val system = java.util.Locale.getDefault().language
        return if (system in com.example.lifeorganizer.core.i18n.SupportedLanguages) system else "en"
    }

    // Security
    val enableAppLock: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_APP_LOCK] ?: false }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    // Calendar-specific
    val homeAddress: Flow<String?> = context.dataStore.data.map { it[HOME_ADDRESS] }
    val travelMode: Flow<String> = context.dataStore.data.map { it[TRAVEL_MODE] ?: "driving" }
    val useHomeAsOrigin: Flow<Boolean> = context.dataStore.data.map { it[USE_HOME_AS_ORIGIN] ?: false }
    val useBrowserForMaps: Flow<Boolean> = context.dataStore.data.map { it[USE_BROWSER_FOR_MAPS] ?: true }
    val notificationSoundUri: Flow<String?> = context.dataStore.data.map { it[NOTIFICATION_SOUND_URI] }
    val geminiApiKey: Flow<String?> = context.dataStore.data.map { it[GEMINI_API_KEY] }
    val enableSmartAlarms: Flow<Boolean> = context.dataStore.data.map { it[ENABLE_SMART_ALARMS] ?: false }
    val useSystemAlarmForReminders: Flow<Boolean> = context.dataStore.data.map { it[USE_SYSTEM_ALARM] ?: false }

    // Save methods
    suspend fun saveAccentColor(color: Int?) = context.dataStore.edit { if (color != null) it[ACCENT_COLOR] = color else it.remove(ACCENT_COLOR) }
    suspend fun saveDesignStyle(style: String) = context.dataStore.edit { it[DESIGN_STYLE] = style }
    suspend fun saveIsDarkTheme(isDark: Boolean?) = context.dataStore.edit { if (isDark == null) it.remove(IS_DARK_THEME) else it[IS_DARK_THEME] = isDark }
    suspend fun saveLanguageCode(code: String) = context.dataStore.edit { it[LANGUAGE_CODE] = code }
    suspend fun saveEnableAppLock(enable: Boolean) = context.dataStore.edit { it[ENABLE_APP_LOCK] = enable }
    suspend fun saveOnboardingCompleted(completed: Boolean) = context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    suspend fun saveHomeAddress(address: String) = context.dataStore.edit { it[HOME_ADDRESS] = address }
    suspend fun saveTravelMode(mode: String) = context.dataStore.edit { it[TRAVEL_MODE] = mode }
    suspend fun saveUseHomeAsOrigin(useHome: Boolean) = context.dataStore.edit { it[USE_HOME_AS_ORIGIN] = useHome }
    suspend fun saveUseBrowserForMaps(useBrowser: Boolean) = context.dataStore.edit { it[USE_BROWSER_FOR_MAPS] = useBrowser }
    suspend fun saveNotificationSoundUri(uri: String?) = context.dataStore.edit { if (uri == null) it.remove(NOTIFICATION_SOUND_URI) else it[NOTIFICATION_SOUND_URI] = uri }
    suspend fun saveGeminiApiKey(key: String?) = context.dataStore.edit { if (key == null) it.remove(GEMINI_API_KEY) else it[GEMINI_API_KEY] = key }
    suspend fun saveEnableSmartAlarms(enable: Boolean) = context.dataStore.edit { it[ENABLE_SMART_ALARMS] = enable }
    suspend fun saveUseSystemAlarmForReminders(enable: Boolean) = context.dataStore.edit { it[USE_SYSTEM_ALARM] = enable }
}
