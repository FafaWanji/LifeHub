package com.example.lifeorganizer.notes.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val colorLabel: Int? = null,
    val isPinned: Boolean = false,
    val isChecklist: Boolean = false,
    val pinnedToDate: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val templateId: Long? = null
)

@Entity(tableName = "note_labels")
data class NoteLabel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    /** Starting content for new notes created with this label (e.g. a "- [ ]" shopping list). */
    @ColumnInfo(defaultValue = "") val template: String = ""
)

@Entity(tableName = "note_label_cross_ref", primaryKeys = ["noteId", "labelId"])
data class NoteLabelCrossRef(
    val noteId: Long,
    val labelId: Long
)

data class NoteWithLabels(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteLabelCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "labelId"
        )
    )
    val labels: List<NoteLabel>
)

@Entity(tableName = "note_templates")
data class NoteTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val content: String,
    val isChecklist: Boolean = false,
    /** Label given to notes created from this template. */
    val labelId: Long? = null
)
