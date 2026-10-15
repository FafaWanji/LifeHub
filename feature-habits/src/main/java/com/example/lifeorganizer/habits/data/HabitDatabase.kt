package com.example.lifeorganizer.habits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.ZoneId

@Database(
    entities = [Habit::class, HabitCheck::class, TaskItem::class],
    version = 2,
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
                    .addMigrations(MIGRATION_1_2)
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
    }
}
