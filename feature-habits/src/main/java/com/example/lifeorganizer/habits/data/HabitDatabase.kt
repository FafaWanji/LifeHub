package com.example.lifeorganizer.habits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Habit::class, HabitCheck::class],
    version = 4,
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
                    .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }

        /**
         * v4: tasks moved to the notes (labels "To Do" / "To Do X"); the old tasks and their points are dropped.
         * Versions 1–3 only differ in the task tables, so all of them go straight to 4.
         */
        private fun dropTasks(from: Int) = object : Migration(from, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS tasks")
                db.execSQL("DROP TABLE IF EXISTS task_points")
            }
        }

        val MIGRATION_1_4 = dropTasks(1)
        val MIGRATION_2_4 = dropTasks(2)
        val MIGRATION_3_4 = dropTasks(3)
    }
}
