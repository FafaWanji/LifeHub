package com.example.lifeorganizer.notes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

/** Colors offered for notes; readable as tints in light and dark themes. */
val NoteColors = listOf(
    0xFFF48FB1.toInt(), 0xFFCE93D8.toInt(), 0xFF90CAF9.toInt(), 0xFF80CBC4.toInt(),
    0xFFA5D6A7.toInt(), 0xFFFFE082.toInt(), 0xFFFFCC80.toInt(), 0xFFBCAAA4.toInt()
)

private enum class EditorMode { EDIT, PREVIEW }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NotesViewModel,
    noteId: Long? = null,
    templateId: Long? = null,
    /** New note started from a label: assign it and use its template. */
    labelId: Long? = null,
    pinnedDate: Long? = null,
    onBack: () -> Unit = {},
    onCreateReminder: (noteId: Long, title: String, timeMillis: Long) -> Unit = { _, _, _ -> }
) {
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val sessionKey = rememberSaveable { UUID.randomUUID().toString() }

    var loaded by rememberSaveable { mutableStateOf(noteId == null && templateId == null && labelId == null) }
    var currentId by rememberSaveable { mutableStateOf(noteId) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var isPinned by rememberSaveable { mutableStateOf(false) }
    var colorLabel by rememberSaveable { mutableStateOf<Int?>(null) }
    var pinnedToDate by rememberSaveable { mutableStateOf(pinnedDate) }
    var labelIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var mode by rememberSaveable { mutableStateOf(if (noteId == null) EditorMode.EDIT else EditorMode.PREVIEW) }
    var deleted by remember { mutableStateOf(false) }
    var lastSaved by remember { mutableStateOf<NoteDraft?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showLabelSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showReminderDate by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf<LocalDate?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val allLabels by viewModel.labels.collectAsState()

    // Load from the database (not from the filtered list) so opening a note never shows – and then saves – an empty body.
    LaunchedEffect(noteId, templateId, labelId) {
        if (loaded) return@LaunchedEffect
        if (noteId != null) {
            val existing = viewModel.loadNote(noteId)
            if (existing == null) {
                currentId = null
                mode = EditorMode.EDIT
            }
            existing?.let { n ->
                title = n.note.title
                val content = if (n.note.isChecklist) migrateLegacyChecklist(n.note.content) else n.note.content
                body = TextFieldValue(content)
                isPinned = n.note.isPinned
                colorLabel = n.note.colorLabel
                pinnedToDate = n.note.pinnedToDate
                labelIds = n.labels.map { it.id }.toSet()
                if (content.isBlank()) mode = EditorMode.EDIT
            }
        } else if (templateId != null) {
            viewModel.loadTemplate(templateId)?.let { body = TextFieldValue(it.content) }
        } else if (labelId != null) {
            viewModel.loadLabel(labelId)?.let { label ->
                labelIds = setOf(label.id)
                if (label.template.isNotBlank()) body = TextFieldValue(label.template, TextRange(label.template.length))
            }
        }
        loaded = true
        lastSaved = draft(currentId, title, body.text, colorLabel, isPinned, pinnedToDate, templateId, labelIds)
    }

    fun currentDraft() = draft(currentId, title, body.text, colorLabel, isPinned, pinnedToDate, templateId, labelIds)

    fun save(onSaved: (Long) -> Unit = {}) {
        if (!loaded || deleted) return
        val d = currentDraft()
        val isEmpty = d.title.isBlank() && d.content.isBlank()
        if (d.id == null && isEmpty) return
        if (d == lastSaved && d.id != null) { onSaved(d.id); return }
        lastSaved = d
        viewModel.saveNote(d, sessionKey) { id ->
            currentId = id
            onSaved(id)
        }
    }

    // Autosave when the app goes to the background and when the editor leaves the screen.
    val saveLatest by rememberUpdatedState<() -> Unit>({ save() })
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { saveLatest() }
    DisposableEffect(Unit) { onDispose { saveLatest() } }

    BackHandler {
        save()
        onBack()
    }

    val dateFormatter = remember(lang) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang))
    }
    val noteTint = colorLabel?.let { tintFor(Color(it)) } ?: MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = noteTint,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteTint),
                title = {},
                navigationIcon = {
                    IconButton(onClick = { save(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Str.back.text())
                    }
                },
                actions = {
                    // Edit ↔ Preview toggle
                    FilledTonalIconToggleButton(
                        checked = mode == EditorMode.PREVIEW,
                        onCheckedChange = { mode = if (it) EditorMode.PREVIEW else EditorMode.EDIT }
                    ) {
                        Icon(
                            if (mode == EditorMode.PREVIEW) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (mode == EditorMode.PREVIEW) Str.edit.text() else Str.preview.text()
                        )
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = Str.pinNote.text(),
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, Str.more.text())
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        MenuEntry(Icons.AutoMirrored.Filled.Label, "${Str.labels.text()} & ${Str.color.text()}") {
                            showMenu = false; showLabelSheet = true
                        }
                        MenuEntry(Icons.Default.Event, Str.pinToDate.text()) {
                            showMenu = false; showDatePicker = true
                        }
                        MenuEntry(Icons.Default.NotificationAdd, Str.setReminder.text()) {
                            showMenu = false; showReminderDate = true
                        }
                        MenuEntry(Icons.Outlined.Bookmarks, Str.saveAsTemplate.text()) {
                            showMenu = false; showTemplateDialog = true
                        }
                        HorizontalDivider()
                        MenuEntry(Icons.Default.Delete, Str.delete.text(), MaterialTheme.colorScheme.error) {
                            showMenu = false
                            currentId?.let { viewModel.softDeleteNote(it) }
                            deleted = true
                            onBack()
                        }
                    }
                }
            )
        },
        bottomBar = {
            EditorBottomBar(
                mode = mode,
                text = body.text,
                containerColor = noteTint,
                onFormat = { action -> body = applyFormat(body, action) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(Str.title.text(), style = MaterialTheme.typography.headlineSmall) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = transparentFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // Meta chips: calendar day, labels, color
            val noteLabels = allLabels.filter { it.id in labelIds }
            AnimatedVisibility(visible = pinnedToDate != null || noteLabels.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pinnedToDate?.let { millis ->
                        InputChip(
                            selected = true,
                            onClick = { showDatePicker = true },
                            label = {
                                Text("${Str.pinnedTo.text()} ${Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)}")
                            },
                            leadingIcon = { Icon(Icons.Default.Event, null, Modifier.size(18.dp)) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close, Str.unpin.text(),
                                    Modifier.size(18.dp).clickable { pinnedToDate = null }
                                )
                            }
                        )
                    }
                    noteLabels.forEach { label ->
                        AssistChip(
                            onClick = { showLabelSheet = true },
                            label = { Text(label.name) },
                            leadingIcon = { Box(Modifier.size(10.dp).background(Color(label.color), CircleShape)) }
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "editor_mode"
            ) { current ->
                when (current) {
                    EditorMode.EDIT -> {
                        val highlighter = MarkdownHighlighter(
                            markerColor = MaterialTheme.colorScheme.outline,
                            accentColor = MaterialTheme.colorScheme.primary,
                            codeBackground = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        TextField(
                            value = body,
                            onValueChange = { new ->
                                val continued = continueListOnNewline(body.text, new.text, new.selection.start)
                                body = if (continued != null) {
                                    TextFieldValue(continued.first, TextRange(continued.second))
                                } else new
                            },
                            placeholder = { Text(Str.startWriting.text()) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            visualTransformation = highlighter,
                            colors = transparentFieldColors(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                    }
                    EditorMode.PREVIEW -> {
                        // Tapping empty space switches to editing; checkboxes toggle in place.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { mode = EditorMode.EDIT }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (body.text.isBlank()) {
                                Text(Str.startWriting.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                MarkdownView(
                                    content = body.text,
                                    onToggleCheckbox = { line ->
                                        body = body.copy(text = toggleCheckbox(body.text, line))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLabelSheet) {
        LabelAndColorSheet(
            allLabels = allLabels,
            selectedIds = labelIds,
            color = colorLabel,
            onToggleLabel = { id ->
                val adding = id !in labelIds
                labelIds = if (adding) labelIds + id else labelIds - id
                // An empty note picks up the label's template right away.
                val template = allLabels.firstOrNull { it.id == id }?.template.orEmpty()
                if (adding && body.text.isBlank() && template.isNotBlank()) {
                    body = TextFieldValue(template, TextRange(template.length))
                    mode = EditorMode.EDIT
                }
            },
            onCreateLabel = { name, color -> viewModel.insertLabel(com.example.lifeorganizer.notes.data.NoteLabel(name = name, color = color)) },
            onColor = { colorLabel = it },
            onDismiss = { showLabelSheet = false }
        )
    }

    if (showDatePicker) {
        // DatePicker works in UTC; convert to/from local midnight so the note lands on the right calendar day.
        val initialUtc = pinnedToDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pinnedToDate = state.selectedDateMillis?.let(::utcPickerToLocalMidnight)
                    showDatePicker = false
                }) { Text(Str.ok.text()) }
            },
            dismissButton = {
                TextButton(onClick = { pinnedToDate = null; showDatePicker = false }) { Text(Str.unpin.text()) }
            }
        ) { DatePicker(state = state) }
    }

    if (showReminderDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showReminderDate = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderDate = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showReminderDate = false
                }) { Text(Str.next.text()) }
            },
            dismissButton = { TextButton(onClick = { showReminderDate = false }) { Text(Str.cancel.text()) } }
        ) { DatePicker(state = state) }
    }

    reminderDate?.let { date ->
        val now = LocalTime.now().plusHours(1).withMinute(0)
        val timeState = rememberTimePickerState(initialHour = now.hour, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { reminderDate = null },
            title = { Text(Str.setReminder.text()) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val millis = date.atTime(timeState.hour, timeState.minute)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    reminderDate = null
                    val reminderTitle = title.ifBlank { markdownToPlainText(body.text).lineSequence().firstOrNull().orEmpty() }
                        .ifBlank { Str.untitled.of(lang) }
                    // The reminder event links to the note, so the note must exist first.
                    lastSaved = null
                    save { id -> onCreateReminder(id, reminderTitle, millis) }
                }) { Text(Str.save.text()) }
            },
            dismissButton = { TextButton(onClick = { reminderDate = null }) { Text(Str.cancel.text()) } }
        )
    }

    if (showTemplateDialog) {
        var name by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text(Str.saveAsTemplate.text()) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(Str.templateName.text()) }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        viewModel.insertTemplate(
                            com.example.lifeorganizer.notes.data.NoteTemplate(
                                name = name.trim(),
                                content = body.text,
                                isChecklist = hasChecklist(body.text)
                            )
                        )
                        showTemplateDialog = false
                        scope.launch { snackbarHostState.showSnackbar(Str.templateSaved.of(lang)) }
                    }
                ) { Text(Str.save.text()) }
            },
            dismissButton = { TextButton(onClick = { showTemplateDialog = false }) { Text(Str.cancel.text()) } }
        )
    }
}

private fun draft(
    id: Long?, title: String, content: String, color: Int?, pinned: Boolean,
    pinnedToDate: Long?, templateId: Long?, labels: Set<Long>
) = NoteDraft(id, title, content, color, pinned, pinnedToDate, templateId, labels)

private fun utcPickerToLocalMidnight(utcMillis: Long): Long =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
fun tintFor(color: Color): Color =
    androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, color, 0.22f)

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
private fun MenuEntry(icon: ImageVector, label: String, tint: Color = LocalContentColor.current, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (tint == LocalContentColor.current) Color.Unspecified else tint) },
        leadingIcon = { Icon(icon, null, tint = tint) },
        onClick = onClick
    )
}

// ---------------------------------------------------------------------------------------------
// Formatting toolbar
// ---------------------------------------------------------------------------------------------

enum class FormatAction { BOLD, ITALIC, STRIKE, CODE, HEADING, BULLET, CHECKBOX }

@Composable
private fun EditorBottomBar(mode: EditorMode, text: String, containerColor: Color, onFormat: (FormatAction) -> Unit) {
    val words = remember(text) { countWords(text) }
    Surface(color = containerColor, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mode == EditorMode.EDIT) {
                    ToolButton(Icons.Default.FormatBold, Str.bold.text()) { onFormat(FormatAction.BOLD) }
                    ToolButton(Icons.Default.FormatItalic, Str.italic.text()) { onFormat(FormatAction.ITALIC) }
                    ToolButton(Icons.Default.FormatStrikethrough, "Strike") { onFormat(FormatAction.STRIKE) }
                    ToolButton(Icons.Default.Title, Str.heading.text()) { onFormat(FormatAction.HEADING) }
                    ToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, Str.bulletList.text()) { onFormat(FormatAction.BULLET) }
                    ToolButton(Icons.Default.CheckBox, Str.checkbox.text()) { onFormat(FormatAction.CHECKBOX) }
                    ToolButton(Icons.Default.Code, "Code") { onFormat(FormatAction.CODE) }
                } else {
                    checklistProgress(text)?.let { p ->
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("${p.done}/${p.total}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text(
                "$words ${Str.words.text()} · ${text.length} ${Str.chars.text()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
}

/** Wraps the selection in inline markers, or toggles a line prefix for block formats. */
internal fun applyFormat(value: TextFieldValue, action: FormatAction): TextFieldValue {
    val text = value.text
    val sel = value.selection
    fun wrap(marker: String): TextFieldValue {
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)
        val newText = text.substring(0, start) + marker + text.substring(start, end) + marker + text.substring(end)
        // Empty selection: place the cursor between the markers.
        val cursor = if (start == end) TextRange(start + marker.length) else TextRange(start + marker.length, end + marker.length)
        return TextFieldValue(newText, cursor)
    }
    fun linePrefix(prefix: String, matcher: Regex): TextFieldValue {
        val lineStart = text.lastIndexOf('\n', (sel.start - 1).coerceAtLeast(0)).let { if (sel.start == 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val existing = matcher.find(line)
        val newLine = if (existing != null) line.removeRange(existing.range) else prefix + line
        val delta = newLine.length - line.length
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        return TextFieldValue(newText, TextRange((sel.start + delta).coerceIn(lineStart, lineStart + newLine.length)))
    }
    return when (action) {
        FormatAction.BOLD -> wrap("**")
        FormatAction.ITALIC -> wrap("*")
        FormatAction.STRIKE -> wrap("~~")
        FormatAction.CODE -> wrap("`")
        FormatAction.HEADING -> linePrefix("## ", Regex("^#{1,6}\\s"))
        FormatAction.BULLET -> linePrefix("- ", Regex("^[-*+]\\s(?!\\[)"))
        FormatAction.CHECKBOX -> linePrefix("- [ ] ", Regex("^[-*+]\\s\\[[ xX]]\\s?"))
    }
}

// ---------------------------------------------------------------------------------------------
// Labels & color sheet
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LabelAndColorSheet(
    allLabels: List<com.example.lifeorganizer.notes.data.NoteLabel>,
    selectedIds: Set<Long>,
    color: Int?,
    onToggleLabel: (Long) -> Unit,
    onCreateLabel: (String, Int) -> Unit,
    onColor: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var newLabel by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(Str.color.text(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorSwatch(null, color == null) { onColor(null) }
                NoteColors.forEach { c -> ColorSwatch(c, color == c) { onColor(c) } }
            }

            Spacer(Modifier.height(24.dp))
            Text(Str.labels.text(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (allLabels.isEmpty()) {
                Text(Str.noLabels.text(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allLabels.forEach { label ->
                    FilterChip(
                        selected = label.id in selectedIds,
                        onClick = { onToggleLabel(label.id) },
                        label = { Text(label.name) },
                        leadingIcon = { Box(Modifier.size(10.dp).background(Color(label.color), CircleShape)) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    label = { Text(Str.newLabel.text()) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    enabled = newLabel.isNotBlank(),
                    onClick = {
                        onCreateLabel(newLabel.trim(), NoteColors[allLabels.size % NoteColors.size])
                        newLabel = ""
                    }
                ) { Text(Str.add.text()) }
            }
        }
    }
}

@Composable
internal fun ColorSwatch(color: Int?, selected: Boolean, onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            .border(if (selected) 3.dp else 1.dp, if (selected) outline else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == null) Icon(Icons.Default.Close, Str.noColor.text(), Modifier.size(18.dp))
        else if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color.Black.copy(alpha = 0.7f))
    }
}
