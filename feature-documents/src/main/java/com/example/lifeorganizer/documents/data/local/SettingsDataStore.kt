package com.example.lifeorganizer.documents.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "document_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val ACCENT_COLOR_INDEX = stringPreferencesKey("accent_color_index")
        val GROUPS = stringSetPreferencesKey("groups")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val themeStyleFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_STYLE] ?: "pastel"
        }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_MODE] ?: false
        }

    val accentColorIndexFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ACCENT_COLOR_INDEX] ?: "0"
        }

    val groupsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[GROUPS] ?: setOf("Allgemein", "Arbeit", "Privat")
        }

    val appLanguageFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[APP_LANGUAGE] ?: "system"
        }

    suspend fun setThemeStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_STYLE] = style
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }

    suspend fun setAccentColorIndex(index: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_COLOR_INDEX] = index
        }
    }

    suspend fun addGroup(group: String) {
        context.dataStore.edit { preferences ->
            val currentGroups = preferences[GROUPS] ?: setOf("Allgemein", "Arbeit", "Privat")
            preferences[GROUPS] = currentGroups + group
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language
        }
    }
}
