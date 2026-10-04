package com.example.lifeorganizer.calendar.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds a version-2 calendar database by hand, migrates it with the app's migrations and lets
 * Room validate the result against the current entities (it throws on any schema mismatch).
 */
@RunWith(AndroidJUnit4::class)
class CalendarMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test-calendar"

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test
    fun migrates_2_to_latest_and_keeps_events() {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `startTimeMillis` INTEGER NOT NULL, `isAllDay` INTEGER NOT NULL, `targetAddress` TEXT, `arrivalBufferMinutes` INTEGER NOT NULL, `alarmLeadMinutes` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, `reminderTimeMillis` INTEGER NOT NULL, `type` TEXT NOT NULL, FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_eventId` ON `reminders` (`eventId`)")
            db.execSQL("INSERT INTO events (title, description, startTimeMillis, isAllDay, targetAddress, arrivalBufferMinutes, alarmLeadMinutes) VALUES ('Vorlesung', '', 1760000000000, 0, 'HTW', 10, 15)")
            db.execSQL("INSERT INTO reminders (eventId, reminderTimeMillis, type) VALUES (1, 1759999100000, '15 min before')")
            db.version = 2
        }

        val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        try {
            val events = runBlocking { room.eventDao().getEventsWithRemindersSync() }
            assertEquals(1, events.size)
            assertEquals("Vorlesung", events[0].event.title)
            assertEquals(1, events[0].reminders.size)
            assertNull(events[0].event.linkedNoteId)
        } finally {
            room.close()
        }
    }
}
