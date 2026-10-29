package com.example.lifeorganizer.habits.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Version-2 habits database migrated to the current version; Room validates the schema on open. */
@RunWith(AndroidJUnit4::class)
class HabitMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test-habits"

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test
    fun migrates_2_to_latest_keeps_habits_and_drops_tasks() {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `weekdays` INTEGER NOT NULL, `reminderMinutes` INTEGER, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `habit_checks` (`habitId` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `epochDay`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `dueEpochDay` INTEGER, `reminderAt` INTEGER, `doneAt` INTEGER, `createdAt` INTEGER NOT NULL)")
            db.execSQL("INSERT INTO habits (title, weekdays, sortOrder, createdAt) VALUES ('Sport', 127, 0, 1)")
            db.execSQL("INSERT INTO tasks (title, note, createdAt) VALUES ('Aufgabe 5', '', 1)")
            db.version = 2
        }

        val room = Room.databaseBuilder(context, HabitDatabase::class.java, name)
            .addMigrations(HabitDatabase.MIGRATION_1_4, HabitDatabase.MIGRATION_2_4, HabitDatabase.MIGRATION_3_4)
            .build()
        try {
            assertEquals(listOf("Sport"), runBlocking { room.habitDao().habitsSync() }.map { it.title })
            room.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE name = 'tasks'").use { c ->
                assertFalse(c.moveToFirst())
            }
        } finally {
            room.close()
        }
    }
}
