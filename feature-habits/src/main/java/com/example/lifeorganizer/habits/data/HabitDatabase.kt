package com.example.lifeorganizer.habits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lifeorganizer.habits.domain.TaskDeadline
import java.time.LocalDate
import java.time.ZoneId

@Database(
    entities = [Habit::class, HabitCheck::class, TaskItem::class, TaskPoints::class],
    version = 3,
    exportSchema = true
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getDatabase(context: Context): HabitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, HabitDatabase::class.java, "habits_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }

        /** Tasks get their own reminder time; open tasks keep the old one (09:00 on the deadline). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderAt INTEGER")
                db.query("SELECT id, dueEpochDay FROM tasks WHERE dueEpochDay IS NOT NULL AND doneAt IS NULL").use { c ->
                    while (c.moveToNext()) {
                        val at = LocalDate.ofEpochDay(c.getLong(1)).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        db.execSQL("UPDATE tasks SET reminderAt = ? WHERE id = ?", arrayOf<Any>(at, c.getLong(0)))
                    }
                }
            }
        }

        /**
         * Deadlines get a time (23:59 for existing ones) and reminders become offsets before it; an old reminder
         * time of a task with a deadline is kept as such an offset. Points get their own table.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN dueMinute INTEGER NOT NULL DEFAULT ${TaskDeadline.END_OF_DAY}")
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderOffsets TEXT NOT NULL DEFAULT '${TaskDeadline.DEFAULT_OFFSETS}'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN lastReminderAt INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER, `title` TEXT NOT NULL, `points` INTEGER NOT NULL, `earnedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_points_taskId` ON `task_points` (`taskId`)")
                // Reminders that passed before the update are not caught up
                db.execSQL("UPDATE tasks SET lastReminderAt = ?", arrayOf<Any>(System.currentTimeMillis()))
                db.query("SELECT id, dueEpochDay, reminderAt FROM tasks WHERE dueEpochDay IS NOT NULL AND reminderAt IS NOT NULL").use { c ->
                    while (c.moveToNext()) {
                        val offsets = TaskDeadline.legacyOffset(c.getLong(1), TaskDeadline.END_OF_DAY, c.getLong(2))?.toString() ?: ""
                        db.execSQL("UPDATE tasks SET reminderOffsets = ?, reminderAt = NULL WHERE id = ?", arrayOf<Any>(offsets, c.getLong(0)))
                    }
                }
            }
        }
    }
}
