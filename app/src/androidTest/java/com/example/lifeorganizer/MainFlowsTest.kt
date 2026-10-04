package com.example.lifeorganizer

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifeorganizer.core.settings.SettingsManager
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import com.example.lifeorganizer.calendar.data.AppDatabase as CalendarDatabase
import com.example.lifeorganizer.notes.data.NotesDatabase

/** Main user flows end to end: add an event, add a note, Smart Add offline. English UI, setup done. */
@RunWith(AndroidJUnit4::class)
class MainFlowsTest {
    private val prepare = object : TestWatcher() {
        override fun starting(description: Description) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            // No system permission dialog on top of the app during the test
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
            CalendarDatabase.getDatabase(context).clearAllTables()
            NotesDatabase.getDatabase(context).clearAllTables()
            runBlocking {
                SettingsManager(context).apply {
                    saveLanguageCode("en")
                    saveOnboardingCompleted(true)
                    saveEnableAppLock(false)
                    saveGeminiApiKey(null) // Smart Add offline
                }
            }
            context.getSharedPreferences("whats_new", Context.MODE_PRIVATE).edit().putString("last_seen", BuildConfig.VERSION_NAME).commit()
            context.getSharedPreferences("updater", Context.MODE_PRIVATE).edit().putLong("last_check", System.currentTimeMillis()).commit()
        }
    }
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(prepare)
        .around(compose)

    private fun waitFor(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun addEventSavesOnClose() {
        compose.onNodeWithContentDescription("New event").performClick()
        compose.onNode(hasSetTextAction() and hasText("Event Title")).performTextInput("UI Test Event")
        Espresso.pressBack() // keyboard
        Espresso.pressBack() // dialog closes and saves
        waitFor("UI Test Event")
    }

    @Test
    fun addNote() {
        compose.onNodeWithContentDescription("Switch to notes").performClick()
        compose.onNodeWithContentDescription("New note").performClick()
        compose.onNode(hasSetTextAction() and hasText("Title")).performTextInput("UI Test Note")
        Espresso.pressBack()
        Espresso.pressBack()
        waitFor("UI Test Note")
    }

    @Test
    fun smartAddOfflineCreatesEvent() {
        compose.onNodeWithContentDescription("Smart Add").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("tomorrow 3pm dentist")
        compose.onNodeWithText("Create").performClick()
        waitFor("Dentist")
    }
}
