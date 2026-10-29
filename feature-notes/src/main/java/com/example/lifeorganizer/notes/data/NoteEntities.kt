package com.example.lifeorganizer.notes.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.lifeorganizer.notes.todo.TaskDeadline

@Entity(tableName = "notes", indices = [Index("parentId")])
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
    val templateId: Long? = null,
    val documentId: Long? = null, // attached Dokki document
    /** Main note this one is a sub-note of; one level only (a main note is never a sub-note itself). */
    val parentId: Long? = null,
    /** Sub-notes and to-do notes can be ticked off as done. */
    @androidx.room.ColumnInfo(defaultValue = "0") val isDone: Boolean = false,
    /** Appointment day of a "To Do X" note, null = none set. */
    val dueEpochDay: Long? = null,
    /** Appointment time as minutes after midnight on [dueEpochDay]. */
    @ColumnInfo(defaultValue = "1439") val dueMinute: Int = TaskDeadline.END_OF_DAY,
    /** Reminders as minutes before the appointment, e.g. "1440,120"; one at the appointment itself always comes on top. */
    @ColumnInfo(defaultValue = "1440,120") val reminderOffsets: String = TaskDeadline.DEFAULT_OFFSETS,
    /** Reminders up to this time (epoch millis) are handled; later ones missed while the phone was off are caught up. */
    val lastReminderAt: Long? = null
)

/** What a label does besides grouping: nothing, a to-do (can be ticked off) or a to-do with an appointment. */
object TodoKind {
    const val NONE = 0
    /** "To Do": general, no appointment – a normal note that can be ticked off. */
    const val TODO = 1
    /** "To Do X": with an appointment (day + time), reminders and a place in the calendar. */
    const val DATED = 2
}

@Entity(tableName = "note_labels")
data class NoteLabel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    /** Starting content for new notes created with this label (e.g. a "- [ ]" shopping list). */
    @ColumnInfo(defaultValue = "") val template: String = "",
    /** See [TodoKind]. */
    @ColumnInfo(defaultValue = "0") val kind: Int = TodoKind.NONE
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
) {
    /** The strongest to-do kind among the labels ([TodoKind]). */
    val todoKind: Int get() = labels.maxOfOrNull { it.kind } ?: TodoKind.NONE
}

@Entity(tableName = "note_templates")
data class NoteTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val content: String,
    val isChecklist: Boolean = false,
    /** Label given to notes created from this template. */
    val labelId: Long? = null
)
