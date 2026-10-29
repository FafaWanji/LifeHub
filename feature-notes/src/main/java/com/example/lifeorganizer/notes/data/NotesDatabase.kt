package com.example.lifeorganizer.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, NoteLabel::class, NoteLabelCrossRef::class, NoteTemplate::class],
    version = 7,
    exportSchema = true
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NotesDatabase? = null

        /**
         * v7: to-do labels ("To Do", "To Do X" with an appointment) and the appointment of a note. Existing labels
         * named like that get their kind; missing ones are created.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_labels ADD COLUMN kind INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN dueEpochDay INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN dueMinute INTEGER NOT NULL DEFAULT 1439")
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderOffsets TEXT NOT NULL DEFAULT '1440,60'")
                db.execSQL("ALTER TABLE notes ADD COLUMN lastReminderAt INTEGER")
                val found = mutableSetOf<Int>()
                db.query("SELECT id, name FROM note_labels").use { c ->
                    while (c.moveToNext()) {
                        val kind = todoKindOfName(c.getString(1)) ?: continue
                        db.execSQL("UPDATE note_labels SET kind = ? WHERE id = ?", arrayOf<Any>(kind, c.getLong(0)))
                        found += kind
                    }
                }
                if (TodoKind.TODO !in found) db.execSQL(
                    "INSERT INTO note_labels (name, color, template, kind) VALUES ('To Do', ?, '', ?)", arrayOf<Any>(0xFF90CAF9.toInt(), TodoKind.TODO)
                )
                if (TodoKind.DATED !in found) db.execSQL(
                    "INSERT INTO note_labels (name, color, template, kind) VALUES ('To Do X', ?, '', ?)", arrayOf<Any>(0xFFF48FB1.toInt(), TodoKind.DATED)
                )
            }
        }

        /** "To Do", "todo", "to-do" → [TodoKind.TODO]; the same with an "X" behind → [TodoKind.DATED]. */
        fun todoKindOfName(name: String): Int? = when (name.lowercase().filter { it.isLetter() }) {
            "todo" -> TodoKind.TODO
            "todox" -> TodoKind.DATED
            else -> null
        }

        /** v5: sub-notes under a main note. */
        /** v6: sub-notes can be ticked off as done. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isDone INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN parentId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_parentId` ON `notes` (`parentId`)")
            }
        }

        /** v4: notes can carry a Dokki document. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN documentId INTEGER")
            }
        }

        /** v3: templates can assign a label. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_templates ADD COLUMN labelId INTEGER")
            }
        }

        /** v2: labels carry a template for new notes. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_labels ADD COLUMN template TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "notes_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
