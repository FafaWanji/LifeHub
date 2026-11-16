package com.example.lifeorganizer.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ApiKeyMoveTest {
    @get:Rule val tmp = TemporaryFolder()
    private val key = stringPreferencesKey("gemini_api_key")
    private val other = stringPreferencesKey("home_address")

    /** Runs [block] with stores that are closed afterwards, so the same files can be opened again. */
    private fun <T> withStore(file: String, settings: DataStore<Preferences>? = null, block: suspend (DataStore<Preferences>) -> T): T = runBlocking {
        val job = Job()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            migrations = listOfNotNull(settings?.let { moveApiKeyFrom(it) })
        ) { File(tmp.root, file) }
        try { block(store) } finally { job.cancelAndJoin() }
    }

    @Test fun keyMovesAndOtherSettingsStay() {
        withStore("settings.preferences_pb") { settings ->
            settings.edit { it[key] = "gsk_old"; it[other] = "Home 1" }
            withStore("secrets.preferences_pb", settings) { secrets ->
                assertEquals("gsk_old", secrets.data.first()[key])
            }
            val left = settings.data.first()
            assertNull(left[key])
            assertEquals("Home 1", left[other])
        }
    }

    @Test fun keyInNewStoreWins() {
        withStore("secrets.preferences_pb") { it.edit { p -> p[key] = "gsk_new" } }
        withStore("settings.preferences_pb") { settings ->
            settings.edit { it[key] = "gsk_restored_old" }
            withStore("secrets.preferences_pb", settings) { secrets ->
                assertEquals("gsk_new", secrets.data.first()[key])
            }
        }
    }
}
