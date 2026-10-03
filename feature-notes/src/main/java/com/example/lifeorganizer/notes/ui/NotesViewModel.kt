package com.example.lifeorganizer.notes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeorganizer.notes.data.Note
import com.example.lifeorganizer.notes.data.NoteLabel
import com.example.lifeorganizer.notes.data.NoteLabelCrossRef
import com.example.lifeorganizer.notes.data.NoteTemplate
import com.example.lifeorganizer.notes.data.NoteWithLabels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SortMode {
    CREATED_DESC, UPDATED_DESC, TITLE_ASC, COLOR
}

/** What the editor hands over for saving. */
data class NoteDraft(
    val id: Long?,
    val title: String,
    val content: String,
    val colorLabel: Int?,
    val isPinned: Boolean,
    val pinnedToDate: Long?,
    val templateId: Long?,
    val labelIds: Set<Long>
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = com.example.lifeorganizer.notes.data.NotesDatabase.getDatabase(application).noteDao()
    private val saveMutex = Mutex()
    // Editor sessions that already created a note, so repeated saves update instead of inserting twice.
    private val sessionIds = mutableMapOf<String, Long>()

    val searchQuery = MutableStateFlow("")
    val selectedLabelFilter = MutableStateFlow<Long?>(null)
    val sortMode = MutableStateFlow(SortMode.UPDATED_DESC)

    private val searchedNotes = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) noteDao.getAllNotes() else noteDao.searchNotes(query)
    }

    /** Notes for the notes list (search, label filter and sort applied). */
    val notes: StateFlow<List<NoteWithLabels>> = combine(searchedNotes, selectedLabelFilter, sortMode) { notesList, labelFilter, sort ->
        val filtered = if (labelFilter == null) notesList
        else notesList.filter { n -> n.labels.any { it.id == labelFilter } }
        when (sort) {
            SortMode.CREATED_DESC -> filtered.sortedByDescending { it.note.createdAt }
            SortMode.UPDATED_DESC -> filtered.sortedByDescending { it.note.updatedAt }
            SortMode.TITLE_ASC -> filtered.sortedBy { it.note.title.lowercase() }
            SortMode.COLOR -> filtered.sortedBy { it.note.colorLabel ?: Int.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All non-deleted notes regardless of the list filters (universal search). */
    val allActiveNotes: StateFlow<List<NoteWithLabels>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Notes attached to a calendar day. */
    val pinnedToDateNotes: StateFlow<List<NoteWithLabels>> = noteDao.getNotesPinnedToDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<NoteWithLabels>> = noteDao.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labels: StateFlow<List<NoteLabel>> = noteDao.getLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<NoteTemplate>> = noteDao.getTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Loads straight from the database – never from the filtered list, which may be empty or stale. */
    suspend fun loadNote(noteId: Long): NoteWithLabels? = noteDao.getNoteWithLabels(noteId)

    suspend fun loadTemplate(templateId: Long): NoteTemplate? = noteDao.getTemplate(templateId)

    suspend fun loadLabel(labelId: Long): NoteLabel? = noteDao.getLabel(labelId)

    /**
     * Inserts or updates a note and its labels. [sessionKey] identifies one editor session so a new
     * note is inserted only once even if save is triggered several times. Returns the note id.
     */
    fun saveNote(draft: NoteDraft, sessionKey: String, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = saveMutex.withLock { upsert(draft, sessionKey) }
            onSaved(id)
        }
    }

    private suspend fun upsert(draft: NoteDraft, sessionKey: String): Long {
        val now = System.currentTimeMillis()
        val existingId = draft.id ?: sessionIds[sessionKey]
        val existing = existingId?.let { noteDao.getNoteWithLabels(it)?.note }
        val note = Note(
            id = existingId ?: 0L,
            title = draft.title,
            content = draft.content,
            createdAt = existing?.createdAt ?: now, // keep the original creation date
            updatedAt = now,
            colorLabel = draft.colorLabel,
            isPinned = draft.isPinned,
            isChecklist = hasChecklist(draft.content),
            pinnedToDate = draft.pinnedToDate,
            isDeleted = existing?.isDeleted ?: false,
            deletedAt = existing?.deletedAt,
            templateId = draft.templateId
        )
        val id = if (existing != null) {
            noteDao.updateNote(note)
            note.id
        } else {
            noteDao.insertNote(note)
        }
        sessionIds[sessionKey] = id
        noteDao.deleteCrossRefsForNote(id)
        draft.labelIds.forEach { noteDao.insertCrossRef(NoteLabelCrossRef(id, it)) }
        return id
    }

    fun insertNote(note: Note, labelIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val noteId = noteDao.insertNote(note)
            labelIds.forEach { labelId ->
                noteDao.insertCrossRef(NoteLabelCrossRef(noteId, labelId))
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch { noteDao.updateNote(note) }
    }

    /** Toggles a "- [ ]" item directly from a list/preview without opening the editor. */
    fun toggleChecklistItem(noteWithLabels: NoteWithLabels, lineIndex: Int) {
        val note = noteWithLabels.note
        val content = toggleCheckbox(note.content, lineIndex)
        viewModelScope.launch {
            noteDao.updateNote(note.copy(content = content, updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch { noteDao.updateNote(note.copy(isPinned = !note.isPinned)) }
    }

    /** Notes just moved to the trash – the list offers "Undo" for them. */
    val recentlyTrashed = MutableStateFlow<List<Long>>(emptyList())
    var trashedAt = 0L
        private set

    fun softDeleteNote(noteId: Long) = softDeleteNotes(listOf(noteId))

    fun softDeleteNotes(noteIds: Collection<Long>) {
        if (noteIds.isEmpty()) return
        viewModelScope.launch {
            noteIds.forEach { noteDao.softDeleteNote(it) }
            trashedAt = System.currentTimeMillis()
            recentlyTrashed.value = noteIds.toList()
        }
    }

    fun undoTrash() {
        val ids = recentlyTrashed.value
        recentlyTrashed.value = emptyList()
        viewModelScope.launch { ids.forEach { noteDao.restoreNote(it) } }
    }

    fun setPinned(notes: Collection<Note>, pinned: Boolean) {
        viewModelScope.launch { notes.forEach { noteDao.updateNote(it.copy(isPinned = pinned)) } }
    }

    fun setColor(notes: Collection<Note>, color: Int?) {
        viewModelScope.launch {
            notes.forEach { noteDao.updateNote(it.copy(colorLabel = color, updatedAt = System.currentTimeMillis())) }
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch { noteDao.restoreNote(noteId) }
    }

    fun permanentlyDeleteNote(noteId: Long) {
        viewModelScope.launch {
            noteDao.deleteCrossRefsForNote(noteId)
            noteDao.permanentlyDeleteNote(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashNotes.value.forEach { noteDao.deleteCrossRefsForNote(it.note.id) }
            noteDao.emptyTrash()
        }
    }

    fun insertLabel(label: NoteLabel) {
        viewModelScope.launch { noteDao.insertLabel(label) }
    }

    fun updateLabel(label: NoteLabel) {
        viewModelScope.launch { noteDao.updateLabel(label) }
    }

    fun deleteLabel(label: NoteLabel) {
        viewModelScope.launch {
            noteDao.deleteCrossRefsForLabel(label.id)
            noteDao.deleteLabel(label)
            if (selectedLabelFilter.value == label.id) selectedLabelFilter.value = null
        }
    }

    fun insertTemplate(template: NoteTemplate) {
        viewModelScope.launch { noteDao.insertTemplate(template) }
    }

    fun deleteTemplate(template: NoteTemplate) {
        viewModelScope.launch { noteDao.deleteTemplate(template) }
    }
}
