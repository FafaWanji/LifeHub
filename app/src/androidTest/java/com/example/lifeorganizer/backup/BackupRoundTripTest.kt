package com.example.lifeorganizer.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifeorganizer.calendar.data.Category
import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.documents.data.local.Document
import com.example.lifeorganizer.notes.data.Note
import com.example.lifeorganizer.notes.data.NoteLabel
import com.example.lifeorganizer.notes.data.NoteLabelCrossRef
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.example.lifeorganizer.calendar.data.AppDatabase as CalendarDatabase
import com.example.lifeorganizer.documents.data.local.AppDatabase as DocumentsDatabase
import com.example.lifeorganizer.notes.data.NotesDatabase
import com.example.lifeorganizer.waypoints.data.AppDatabase as WaypointsDatabase

/**
 * Export → wipe everything → import must give back the same data, including links between
 * modules (event/note → document, event → category, note → label) and the view settings.
 * Runs on a device/emulator and wipes the app's databases there.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val calendar = CalendarDatabase.getDatabase(context)
    private val notes = NotesDatabase.getDatabase(context)
    private val documents = DocumentsDatabase.getDatabase(context)
    private val waypoints = WaypointsDatabase.getDatabase(context)

    private fun wipe() {
        calendar.clearAllTables(); notes.clearAllTables(); documents.clearAllTables(); waypoints.clearAllTables()
    }

    @Test
    fun exportThenImportRestoresEverything(): Unit = runBlocking {
        wipe()
        val docId = documents.documentDao().insertDocument(Document(title = "Arztbrief", uri = "content://test/arzt", category = "Allgemein"))
        val catId = calendar.eventDao().insertCategory(Category(name = "Uni", color = 0xFF2196F3.toInt()))
        calendar.eventDao().insertEvent(
            Event(
                title = "Vorlesung", description = "", startTimeMillis = 1_791_000_000_000L, timezone = "Europe/Berlin",
                recurrenceRule = "FREQ=WEEKLY", exDates = "2026-10-20", categoryId = catId, documentId = docId
            )
        )
        val labelId = notes.noteDao().insertLabel(NoteLabel(name = "Einkauf", color = 1, template = "- [ ] Milch"))
        val noteId = notes.noteDao().insertNote(
            Note(title = "Liste", content = "- [x] Brot", createdAt = 5, updatedAt = 6, documentId = docId)
        )
        notes.noteDao().insertCrossRef(NoteLabelCrossRef(noteId, labelId))
        context.getSharedPreferences("calendar_filter", Context.MODE_PRIVATE).edit()
            .putBoolean("hide_recurring", true).putStringSet("hidden_categories", setOf(catId.toString())).commit()

        val file = File(context.cacheDir, "roundtrip.json")
        BackupManager(context).export(Uri.fromFile(file))

        wipe()
        context.getSharedPreferences("calendar_filter", Context.MODE_PRIVATE).edit().clear().commit()
        BackupManager(context).import(Uri.fromFile(file))

        val event = calendar.eventDao().getEventsWithRemindersSync().single().event
        val doc = documents.documentDao().getAllDocumentsSync().single()
        val category = calendar.eventDao().getCategoriesSync().single { it.name == "Uni" }
        assertEquals("FREQ=WEEKLY", event.recurrenceRule)
        assertEquals("2026-10-20", event.exDates)
        assertEquals(category.id, event.categoryId)
        assertEquals(doc.id, event.documentId)

        val note = notes.noteDao().getAllNotesWithLabelsSync().single()
        assertEquals("- [x] Brot", note.note.content)
        assertEquals("Einkauf", note.labels.single().name)
        assertEquals("- [ ] Milch", note.labels.single().template)
        assertEquals(doc.id, note.note.documentId)

        val filter = context.getSharedPreferences("calendar_filter", Context.MODE_PRIVATE)
        assertEquals(true, filter.getBoolean("hide_recurring", false))
        assertEquals(setOf(category.id.toString()), filter.getStringSet("hidden_categories", null))

        wipe()
        filter.edit().clear().commit()
    }
}
