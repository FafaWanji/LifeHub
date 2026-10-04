package com.example.lifeorganizer.notes.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Version-1 notes database migrated to the current version; Room validates the schema on open. */
@RunWith(AndroidJUnit4::class)
class NotesMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test-notes"

    @After
    fun cleanUp() {
        context.deleteDatabase(name)
    }

    @Test
    fun migrates_1_to_latest_and_keeps_notes_and_labels() {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `colorLabel` INTEGER, `isPinned` INTEGER NOT NULL, `isChecklist` INTEGER NOT NULL, `pinnedToDate` INTEGER, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, `templateId` INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `note_labels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `note_label_cross_ref` (`noteId` INTEGER NOT NULL, `labelId` INTEGER NOT NULL, PRIMARY KEY(`noteId`, `labelId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `note_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `content` TEXT NOT NULL, `isChecklist` INTEGER NOT NULL)")
            db.execSQL("INSERT INTO notes (title, content, createdAt, updatedAt, isPinned, isChecklist, isDeleted) VALUES ('Einkauf', '- [ ] Milch', 1, 1, 0, 1, 0)")
            db.execSQL("INSERT INTO note_labels (name, color) VALUES ('Privat', 0)")
            db.execSQL("INSERT INTO note_label_cross_ref (noteId, labelId) VALUES (1, 1)")
            db.version = 1
        }

        val room = Room.databaseBuilder(context, NotesDatabase::class.java, name)
            .addMigrations(NotesDatabase.MIGRATION_1_2, NotesDatabase.MIGRATION_2_3)
            .build()
        try {
            val notes = runBlocking { room.noteDao().getAllNotesWithLabelsSync() }
            assertEquals(1, notes.size)
            assertEquals("Privat", notes[0].labels.single().name)
            assertEquals("", runBlocking { room.noteDao().getLabelsSync() }.single().template)
        } finally {
            room.close()
        }
    }
}
