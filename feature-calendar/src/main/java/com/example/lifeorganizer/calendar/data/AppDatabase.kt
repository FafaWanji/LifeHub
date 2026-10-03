package com.example.lifeorganizer.calendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Event::class, Reminder::class, Category::class, EventTemplate::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN endTimeMillis INTEGER")
                db.execSQL("ALTER TABLE events ADD COLUMN color INTEGER")
                db.execSQL("ALTER TABLE events ADD COLUMN recurrenceRule TEXT")
                db.execSQL("ALTER TABLE events ADD COLUMN timezone TEXT NOT NULL DEFAULT '${java.time.ZoneId.systemDefault().id}'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN timezone TEXT NOT NULL DEFAULT '${java.time.ZoneId.systemDefault().id}'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN isBirthday INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE events ADD COLUMN birthYear INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, `isDefault` INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE events ADD COLUMN categoryId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_categoryId` ON `events` (`categoryId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `event_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `isAllDay` INTEGER NOT NULL, `targetAddress` TEXT, `arrivalBufferMinutes` INTEGER NOT NULL, `alarmLeadMinutes` INTEGER NOT NULL, `categoryId` INTEGER, `recurrenceRule` TEXT)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN linkedNoteId INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendar_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
