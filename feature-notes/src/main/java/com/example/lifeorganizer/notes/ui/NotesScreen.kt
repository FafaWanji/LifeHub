package com.example.lifeorganizer.notes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.notes.data.NoteLabel
import com.example.lifeorganizer.notes.data.NoteWithLabels

enum class NotesView {
    MAIN, TRASH, LABELS, TEMPLATES
}

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit = {},
    onCreateNote: () -> Unit = {},
    onGlobalSearchClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {}
) {
    var currentView by rememberSaveable { mutableStateOf(NotesView.MAIN) }

    // Sub-pages return to the list instead of leaving the notes module.
    BackHandler(enabled = currentView != NotesView.MAIN) { currentView = NotesView.MAIN }

    AnimatedContent(
        targetState = currentView,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "NotesView"
    ) { view ->
        when (view) {
            NotesView.MAIN -> NotesMainView(
                viewModel = viewModel,
                onNoteClick = onNoteClick,
                onGlobalSearchClick = onGlobalSearchClick,
                onMenuClick = onMenuClick,
                onNavigate = { currentView = it }
            )
            NotesView.TRASH -> TrashView(viewModel) { currentView = NotesView.MAIN }
            NotesView.LABELS -> LabelsManagerView(viewModel) { currentView = NotesView.MAIN }
            NotesView.TEMPLATES -> TemplatesManagerView(viewModel) { currentView = NotesView.MAIN }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesMainView(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit,
    onGlobalSearchClick: (() -> Unit)?,
    onMenuClick: () -> Unit,
    onNavigate: (NotesView) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val selectedLabelFilter by viewModel.selectedLabelFilter.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val recentlyTrashed by viewModel.recentlyTrashed.collectAsState()
    val lang = LocalAppLanguage.current

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    // Multi-select: long-press a card to start, tap to add/remove.
    var selectedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    val selectionMode = selectedIds.isNotEmpty()
    val selectedNotes = notes.filter { it.note.id in selectedIds }.map { it.note }
    val snackbarHostState = remember { SnackbarHostState() }

    // Drop ids that vanished (deleted elsewhere / filtered out)
    LaunchedEffect(notes) {
        val visible = notes.map { it.note.id }.toSet()
        if (!visible.containsAll(selectedIds)) selectedIds = selectedIds intersect visible
    }
    BackHandler(enabled = selectionMode) { selectedIds = emptySet() }

    // "Moved to trash · Undo" – also after deleting from inside the editor.
    LaunchedEffect(recentlyTrashed) {
        if (recentlyTrashed.isEmpty()) return@LaunchedEffect
        if (System.currentTimeMillis() - viewModel.trashedAt > 10_000) {
            viewModel.recentlyTrashed.value = emptyList()
            return@LaunchedEffect
        }
        val result = snackbarHostState.showSnackbar(
            message = if (recentlyTrashed.size == 1) Str.movedToTrash.of(lang)
            else Str.notesMovedToTrash.of(lang).format(recentlyTrashed.size),
            actionLabel = Str.undo.of(lang),
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoTrash()
        else viewModel.recentlyTrashed.value = emptyList()
    }

    fun onCardClick(id: Long) {
        if (selectionMode) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        else onNoteClick(id)
    }
    fun onCardLongClick(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    Scaffold(
        snackbarHost = {
            // Left of the floating action cluster so neither covers the other.
            SnackbarHost(snackbarHostState, modifier = Modifier.padding(end = 72.dp))
        },
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) { Icon(Icons.Default.Close, Str.cancel.text()) }
                    },
                    title = { Text("${selectedIds.size} ${Str.selected.text()}") },
                    actions = {
                        val allPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.isPinned }
                        IconButton(onClick = {
                            viewModel.setPinned(selectedNotes, !allPinned)
                            selectedIds = emptySet()
                        }) {
                            Icon(
                                if (allPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                                contentDescription = if (allPinned) Str.unpinNote.text() else Str.pinNote.text()
                            )
                        }
                        IconButton(onClick = { showColorPicker = true }) {
                            Icon(Icons.Outlined.Palette, contentDescription = Str.color.text())
                        }
                        IconButton(onClick = {
                            viewModel.softDeleteNotes(selectedIds)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = Str.delete.text())
                        }
                    }
                )
            } else TopAppBar(
                title = {
                    // Search pill opens the universal search across all modules.
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = { onGlobalSearchClick?.invoke() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                Str.searchEverywhere.text(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = Str.menu.text()) }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = Str.sort.text())
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(sortLabel(mode)) },
                                    trailingIcon = { if (mode == sortMode) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        viewModel.sortMode.value = mode
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = Str.more.text())
                        }
                        DropdownMenu(expanded = overflowMenuExpanded, onDismissRequest = { overflowMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(Str.manageLabels.text()) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, null) },
                                onClick = { overflowMenuExpanded = false; onNavigate(NotesView.LABELS) }
                            )
                            DropdownMenuItem(
                                text = { Text(Str.manageTemplates.text()) },
                                leadingIcon = { Icon(Icons.Outlined.Bookmarks, null) },
                                onClick = { overflowMenuExpanded = false; onNavigate(NotesView.TEMPLATES) }
                            )
                            DropdownMenuItem(
                                text = { Text(Str.trash.text()) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = { overflowMenuExpanded = false; onNavigate(NotesView.TRASH) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (labels.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedLabelFilter == null,
                            onClick = { viewModel.selectedLabelFilter.value = null },
                            label = { Text(Str.all.text()) }
                        )
                    }
                    items(labels, key = { it.id }) { label ->
                        FilterChip(
                            selected = selectedLabelFilter == label.id,
                            onClick = {
                                viewModel.selectedLabelFilter.value = if (selectedLabelFilter == label.id) null else label.id
                            },
                            label = { Text(label.name) },
                            leadingIcon = { Box(Modifier.size(10.dp).background(Color(label.color), CircleShape)) }
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                // With an active label filter the list isn't empty in general – say so.
                if (selectedLabelFilter != null) EmptyNotes(Icons.AutoMirrored.Outlined.Label, Str.noNotesWithLabel.text())
                else EmptyNotes(Icons.AutoMirrored.Filled.Notes, Str.emptyNotes.text())
            } else {
                val pinnedNotes = notes.filter { it.note.isPinned }
                val otherNotes = notes.filter { !it.note.isPinned }
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize(),
                    // Leave room for the floating action cluster.
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine, key = "h_pinned") { SectionHeader(Str.pinned.text()) }
                        items(pinnedNotes, key = { it.note.id }) { n ->
                            NoteCard(
                                n,
                                onClick = { onCardClick(n.note.id) },
                                onLongClick = { onCardLongClick(n.note.id) },
                                selected = n.note.id in selectedIds,
                                onToggleItem = { line -> viewModel.toggleChecklistItem(n, line) }
                            )
                        }
                        if (otherNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "h_other") { SectionHeader(Str.notes.text()) }
                        }
                    }
                    items(otherNotes, key = { it.note.id }) { n ->
                        NoteCard(
                            n,
                            onClick = { onCardClick(n.note.id) },
                            onLongClick = { onCardLongClick(n.note.id) },
                            selected = n.note.id in selectedIds,
                            onToggleItem = { line -> viewModel.toggleChecklistItem(n, line) }
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(Str.color.text()) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val current = selectedNotes.map { it.colorLabel }.distinct().singleOrNull()
                    ColorSwatch(null, selectedNotes.isNotEmpty() && current == null) {
                        viewModel.setColor(selectedNotes, null); showColorPicker = false; selectedIds = emptySet()
                    }
                    NoteColors.forEach { c ->
                        ColorSwatch(c, current == c) {
                            viewModel.setColor(selectedNotes, c); showColorPicker = false; selectedIds = emptySet()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text(Str.cancel.text()) } }
        )
    }
}

@Composable
private fun sortLabel(mode: SortMode) = when (mode) {
    SortMode.UPDATED_DESC -> Str.sortUpdated.text()
    SortMode.CREATED_DESC -> Str.sortCreated.text()
    SortMode.TITLE_ASC -> Str.sortTitle.text()
    SortMode.COLOR -> Str.sortColor.text()
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyNotes(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubPageScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Str.back.text()) }
                },
                actions = actions
            )
        },
        content = content
    )
}

@Composable
fun TrashView(viewModel: NotesViewModel, onBack: () -> Unit) {
    val trashNotes by viewModel.trashNotes.collectAsState()
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<NoteWithLabels?>(null) }

    SubPageScaffold(
        title = Str.trash.text(),
        onBack = onBack,
        actions = {
            if (trashNotes.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) { Text(Str.emptyTrash.text()) }
            }
        }
    ) { padding ->
        if (trashNotes.isEmpty()) {
            Box(Modifier.padding(padding)) { EmptyNotes(Icons.Default.Delete, Str.trashEmpty.text()) }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                items(trashNotes, key = { it.note.id }) { n ->
                    Column {
                        NoteCard(noteWithLabels = n, onClick = {})
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = { viewModel.restoreNote(n.note.id) }) {
                                Icon(Icons.Default.Restore, contentDescription = Str.restore.text(), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { confirmDelete = n }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = Str.deleteForever.text(), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(Str.emptyTrashConfirm.text(), Str.emptyTrash.text(), onDismiss = { confirmEmpty = false }) {
            viewModel.emptyTrash()
            confirmEmpty = false
        }
    }
    confirmDelete?.let { n ->
        ConfirmDialog(Str.deleteForeverConfirm.text(), Str.deleteForever.text(), onDismiss = { confirmDelete = null }) {
            viewModel.permanentlyDeleteNote(n.note.id)
            confirmDelete = null
        }
    }
}

@Composable
private fun ConfirmDialog(message: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel.text()) } }
    )
}

@Composable
fun LabelsManagerView(viewModel: NotesViewModel, onBack: () -> Unit) {
    val labels by viewModel.labels.collectAsState()
    var newLabelName by remember { mutableStateOf("") }
    var newLabelColor by remember { mutableStateOf(NoteColors.first()) }
    var recolor by remember { mutableStateOf<NoteLabel?>(null) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }

    SubPageScaffold(title = Str.manageLabels.text(), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newLabelName,
                                onValueChange = { newLabelName = it },
                                label = { Text(Str.newLabel.text()) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = newLabelName.isNotBlank(),
                                onClick = {
                                    viewModel.insertLabel(NoteLabel(name = newLabelName.trim(), color = newLabelColor))
                                    newLabelName = ""
                                }
                            ) { Text(Str.add.text()) }
                        }
                        Spacer(Modifier.height(12.dp))
                        ColorRow(selected = newLabelColor) { newLabelColor = it }
                    }
                }
            }
            if (labels.isEmpty()) {
                item {
                    Text(
                        Str.noLabels.text(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(labels, key = { it.id }) { label ->
                val expanded = expandedId == label.id
                ListItem(
                    modifier = Modifier.clickable { expandedId = if (expanded) null else label.id },
                    headlineContent = { Text(label.name) },
                    supportingContent = {
                        if (!expanded) {
                            Text(
                                label.template.takeIf { it.isNotBlank() }?.let { markdownToPlainText(it).lineSequence().first() }
                                    ?: Str.labelTemplateNone.text(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    leadingContent = {
                        Box(
                            Modifier.size(28.dp).background(Color(label.color), CircleShape).clickable { recolor = label }
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteLabel(label) }) {
                            Icon(Icons.Default.Delete, contentDescription = Str.delete.text())
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (expanded) LabelTemplateEditor(label) { viewModel.updateLabel(label.copy(template = it)) }
            }
        }
    }

    recolor?.let { label ->
        AlertDialog(
            onDismissRequest = { recolor = null },
            title = { Text(label.name) },
            text = {
                ColorRow(selected = label.color) {
                    viewModel.updateLabel(label.copy(color = it))
                    recolor = null
                }
            },
            confirmButton = { TextButton(onClick = { recolor = null }) { Text(Str.done.text()) } }
        )
    }
}

/** Template of a label; every change is saved immediately and used by the next new note. */
@Composable
private fun LabelTemplateEditor(label: NoteLabel, onChange: (String) -> Unit) {
    var value by remember(label.id) {
        mutableStateOf(TextFieldValue(label.template, TextRange(label.template.length)))
    }
    fun update(v: TextFieldValue) {
        value = v
        onChange(v.text)
    }
    val focus = remember { FocusRequester() }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { update(it) },
            label = { Text(Str.labelTemplate.text()) },
            supportingText = { Text(Str.placeholderHint.text()) },
            placeholder = { Text("- [ ] …") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().focusRequester(focus)
        )
        TextButton(onClick = {
            // Adds a checklist item on a new line, cursor behind it
            val t = value.text
            val prefix = if (t.isEmpty() || t.endsWith('\n')) "" else "\n"
            val next = "$t$prefix- [ ] "
            update(TextFieldValue(next, TextRange(next.length)))
            runCatching { focus.requestFocus() }
        }) {
            Icon(Icons.Default.CheckBox, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Str.addChecklistItem.text())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NoteColors.forEach { c -> ColorSwatch(c, c == selected) { onPick(c) } }
    }
}

@Composable
fun TemplatesManagerView(viewModel: NotesViewModel, onBack: () -> Unit) {
    val templates by viewModel.templates.collectAsState()
    val labels by viewModel.labels.collectAsState()

    SubPageScaffold(title = Str.manageTemplates.text(), onBack = onBack) { padding ->
        if (templates.isEmpty()) {
            Box(Modifier.padding(padding)) { EmptyNotes(Icons.Outlined.Bookmarks, Str.noTemplates.text()) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                      Column {
                        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleMedium)
                                if (template.content.isNotBlank()) {
                                    Text(
                                        markdownToPlainText(template.content),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteTemplate(template) }) {
                                Icon(Icons.Default.Delete, contentDescription = Str.delete.text())
                            }
                        }
                        // Standard label for notes created from this template
                        if (labels.isNotEmpty()) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                labels.forEach { label ->
                                    FilterChip(
                                        selected = template.labelId == label.id,
                                        onClick = {
                                            viewModel.updateTemplate(template.copy(labelId = if (template.labelId == label.id) null else label.id))
                                        },
                                        label = { Text(label.name) },
                                        leadingIcon = { Box(Modifier.size(10.dp).background(Color(label.color), CircleShape)) }
                                    )
                                }
                            }
                        }
                      }
                    }
                }
            }
        }
    }
}
