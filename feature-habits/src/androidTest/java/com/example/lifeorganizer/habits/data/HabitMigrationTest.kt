package com.example.lifeorganizer.habits.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifeorganizer.habits.domain.TaskDeadline
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

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
    fun migrates_2_to_latest_and_keeps_the_old_reminder_as_offset() {
        context.deleteDatabase(name)
        val day = LocalDate.of(2026, 10, 22)
        val nine = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `weekdays` INTEGER NOT NULL, `reminderMinutes` INTEGER, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `habit_checks` (`habitId` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `epochDay`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `dueEpochDay` INTEGER, `reminderAt` INTEGER, `doneAt` INTEGER, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueEpochDay` ON `tasks` (`dueEpochDay`)")
            db.execSQL("INSERT INTO tasks (title, note, dueEpochDay, reminderAt, createdAt) VALUES ('Aufgabe 5', '', ${day.toEpochDay()}, $nine, 1)")
            db.execSQL("INSERT INTO tasks (title, note, reminderAt, createdAt) VALUES ('Ohne Deadline', '', $nine, 2)")
            db.version = 2
        }

        val room = Room.databaseBuilder(context, HabitDatabase::class.java, name)
            .addMigrations(HabitDatabase.MIGRATION_1_2, HabitDatabase.MIGRATION_2_3)
            .build()
        try {
            val tasks = runBlocking { room.habitDao().tasksSync() }.sortedBy { it.createdAt }
            // 09:00 on the deadline day = 14 h 59 min before 23:59
            assertEquals(TaskDeadline.END_OF_DAY, tasks[0].dueMinute)
            assertEquals("899", tasks[0].reminderOffsets)
            assertNull(tasks[0].reminderAt)
            // Without a deadline the old fixed reminder stays
            assertEquals(nine, tasks[1].reminderAt)
            assertEquals(0, runBlocking { room.habitDao().pointsSync() }.size)
        } finally {
            room.close()
        }
    }
}
