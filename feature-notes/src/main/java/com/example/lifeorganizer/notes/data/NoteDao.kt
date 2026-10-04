package com.example.lifeorganizer.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Backup: everything, including the trash
    @Transaction
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesWithLabelsSync(): List<NoteWithLabels>

    @Query("SELECT * FROM note_labels")
    suspend fun getLabelsSync(): List<NoteLabel>

    @Query("SELECT * FROM note_templates")
    suspend fun getTemplatesSync(): List<NoteTemplate>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteWithLabels>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteWithLabels>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteWithLabels>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND pinnedToDate = :dateMillis ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByPinnedDate(dateMillis: Long): Flow<List<NoteWithLabels>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND pinnedToDate IS NOT NULL")
    fun getNotesPinnedToDates(): Flow<List<NoteWithLabels>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteWithLabels(noteId: Long): NoteWithLabels?

    @Query("SELECT * FROM note_templates WHERE id = :templateId")
    suspend fun getTemplate(templateId: Long): NoteTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = null WHERE id = :noteId")
    suspend fun restoreNote(noteId: Long)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun permanentlyDeleteNote(noteId: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: NoteLabel): Long

    @Delete
    suspend fun deleteLabel(label: NoteLabel)

    @Query("SELECT * FROM note_labels WHERE id = :labelId")
    suspend fun getLabel(labelId: Long): NoteLabel?

    @Query("SELECT * FROM note_labels ORDER BY name ASC")
    fun getLabels(): Flow<List<NoteLabel>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: NoteLabelCrossRef)

    @Query("DELETE FROM note_label_cross_ref WHERE noteId = :noteId AND labelId = :labelId")
    suspend fun deleteCrossRef(noteId: Long, labelId: Long)

    @Query("DELETE FROM note_label_cross_ref WHERE noteId = :noteId")
    suspend fun deleteCrossRefsForNote(noteId: Long)

    @Query("DELETE FROM note_label_cross_ref WHERE labelId = :labelId")
    suspend fun deleteCrossRefsForLabel(labelId: Long)

    @Update
    suspend fun updateLabel(label: NoteLabel)

    @Query("SELECT * FROM note_templates ORDER BY name ASC")
    fun getTemplates(): Flow<List<NoteTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: NoteTemplate): Long

    @Delete
    suspend fun deleteTemplate(template: NoteTemplate)

    @Update
    suspend fun updateTemplate(template: NoteTemplate)
}
