package com.example.lifeorganizer.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.lifeorganizer.MainActivity
import com.example.lifeorganizer.calendar.ui.CalendarScreen
import com.example.lifeorganizer.calendar.ui.PinnedNote
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.smartadd.SmartAddDialog
import com.example.lifeorganizer.core.smartadd.SmartAddEngine
import com.example.lifeorganizer.core.smartadd.SmartAddStart
import com.example.lifeorganizer.core.smartadd.SmartResult
import com.example.lifeorganizer.documents.ui.screens.DocumentViewModel
import com.example.lifeorganizer.notes.ui.NoteEditorScreen
import com.example.lifeorganizer.notes.ui.NotesScreen
import com.example.lifeorganizer.notes.ui.NotesViewModel
import com.example.lifeorganizer.notes.ui.markdownToPlainText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.lifeorganizer.calendar.ui.MainViewModel as CalendarViewModel
import com.example.lifeorganizer.waypoints.ui.MainViewModel as WaypointsViewModel

enum class ActiveView {
    CALENDAR, NOTES, NOTE_EDITOR, WAYPOINTS, DOCUMENTS, MONEY
}

/** Sub-screens of the embedded Waypoints module. */
sealed interface WaypointRoute {
    data object List : WaypointRoute
    data object Add : WaypointRoute
    data class Edit(val id: Long) : WaypointRoute
    data object Settings : WaypointRoute
}

@Composable
fun MainScreen(
    initialSharedText: String? = null,
    initialSharedImageUri: Uri? = null,
    initialIntentAction: String? = null,
    initialNoteId: Long? = null,
    importUri: Uri? = null,
    onImportHandled: () -> Unit = {},
    intentsEnabled: Boolean = true,
    onSharedHandled: () -> Unit = {}
) {
    val calendarViewModel: CalendarViewModel = viewModel()
    val notesViewModel: NotesViewModel = viewModel()
    val waypointsViewModel: WaypointsViewModel = viewModel()
    val documentViewModel: DocumentViewModel = viewModel()

    // The app is calendar-centred: it always starts here.
    var activeView by rememberSaveable { mutableStateOf(ActiveView.CALENDAR) }
    var editingNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorTemplateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorLabelId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Each opening of the editor is a new session; otherwise the saved state of the previous note comes back.
    var editorSession by rememberSaveable { mutableIntStateOf(0) }
    var editorReturnView by rememberSaveable { mutableStateOf(ActiveView.NOTES) }
    var waypointRoute by remember { mutableStateOf<WaypointRoute>(WaypointRoute.List) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    // One-shot requests forwarded into the calendar screen.
    var addEventRequest by remember { mutableIntStateOf(0) }
    var settingsRequest by remember { mutableIntStateOf(0) }
    var calendarFocus by remember { mutableStateOf<Pair<LocalDate, Long?>?>(null) }
    var documentToOpen by remember { mutableStateOf<Long?>(null) }
    var eventPrefill by remember { mutableStateOf<SmartResult.Event?>(null) }

    var showSmartAdd by remember { mutableStateOf(false) }
    var smartAddInitialText by remember { mutableStateOf("") }
    var smartAddImageUri by remember { mutableStateOf<Uri?>(null) }
    var smartAddStart by remember { mutableStateOf(SmartAddStart.NONE) }
    var showUniversalSearch by remember { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var updateRequest by remember { mutableStateOf(false) }
    var foundUpdate by remember { mutableStateOf<com.example.lifeorganizer.update.UpdateInfo?>(null) }
    // A shared/opened backup file (.ics/.json) jumps straight into the import screen.
    LaunchedEffect(importUri, intentsEnabled) {
        if (importUri != null && intentsEnabled) showBackup = true
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lang = LocalAppLanguage.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val docsNavController = rememberNavController()

    val geminiApiKey by calendarViewModel.geminiApiKey.collectAsState()
    val categories by calendarViewModel.categories.collectAsState()
    val moneyContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val moneyRepository = remember { com.example.lifeorganizer.money.data.MoneyRepository.get(moneyContext) }
    val moneyCategories by moneyRepository.categories().collectAsState(initial = emptyList())
    val waypoints by waypointsViewModel.allWaypoints.collectAsState()
    val pinnedNotes by notesViewModel.pinnedToDateNotes.collectAsState()
    val templates by notesViewModel.templates.collectAsState()
    val allDocuments by documentViewModel.allDocuments.collectAsState()
    val appContextForUpdate = androidx.compose.ui.platform.LocalContext.current.applicationContext
    // Quiet daily check for a newer release on GitHub
    LaunchedEffect(Unit) { foundUpdate = com.example.lifeorganizer.update.dailyUpdateCheck(appContextForUpdate) }
    // Keep the note widget in sync with edits made in the app.
    val activeNotes by notesViewModel.allActiveNotes.collectAsState()
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    LaunchedEffect(activeNotes) { com.example.lifeorganizer.widgets.NoteWidget.refresh(appContext) }
    val noteLabels by notesViewModel.labels.collectAsState()

    fun openSmartAdd(text: String = "", image: Uri? = null, start: SmartAddStart = SmartAddStart.NONE) {
        smartAddInitialText = text
        smartAddImageUri = image
        smartAddStart = start
        showSmartAdd = true
    }

    fun openNoteEditor(noteId: Long?, templateId: Long? = null, labelId: Long? = null) {
        if (activeView != ActiveView.NOTE_EDITOR) editorReturnView = activeView
        editingNoteId = noteId
        editorTemplateId = templateId
        editorLabelId = labelId
        editorSession++
        activeView = ActiveView.NOTE_EDITOR
    }

    fun createNote() {
        if (templates.isEmpty() && noteLabels.isEmpty()) openNoteEditor(null) else showTemplatePicker = true
    }

    fun navigate(view: ActiveView) {
        if (view == ActiveView.WAYPOINTS) waypointRoute = WaypointRoute.List
        activeView = view
    }

    LaunchedEffect(initialSharedText, initialSharedImageUri, initialIntentAction, initialNoteId, intentsEnabled) {
        if (!intentsEnabled) return@LaunchedEffect
        when {
            initialSharedText != null -> openSmartAdd(text = initialSharedText)
            initialSharedImageUri != null -> openSmartAdd(image = initialSharedImageUri)
            else -> when (initialIntentAction) {
                MainActivity.ACTION_SMART_ADD -> openSmartAdd()
                MainActivity.ACTION_SMART_ADD_VOICE -> openSmartAdd(start = SmartAddStart.VOICE)
                MainActivity.ACTION_SMART_ADD_SCAN -> openSmartAdd(start = SmartAddStart.SCAN)
                MainActivity.ACTION_NEW_NOTE -> openNoteEditor(null)
                MainActivity.ACTION_OPEN_NOTE -> initialNoteId?.let { openNoteEditor(it) }
                MainActivity.ACTION_NEW_EVENT -> {
                    activeView = ActiveView.CALENDAR
                    addEventRequest++
                }
                MainActivity.ACTION_WAYPOINTS -> navigate(ActiveView.WAYPOINTS)
                MainActivity.ACTION_SEARCH -> showUniversalSearch = true
                MainActivity.ACTION_AGENDA -> activeView = ActiveView.CALENDAR
                else -> return@LaunchedEffect
            }
        }
        onSharedHandled()
    }

    // Android 13+: reminders can only be shown once the user allows notifications.
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(intentsEnabled) {
        if (intentsEnabled && android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Secondary modules return to the calendar instead of closing the app.
    BackHandler(enabled = activeView == ActiveView.NOTES || activeView == ActiveView.DOCUMENTS ||
        (activeView == ActiveView.WAYPOINTS && waypointRoute == WaypointRoute.List)
    ) {
        activeView = ActiveView.CALENDAR
    }
    BackHandler(enabled = activeView == ActiveView.WAYPOINTS && waypointRoute != WaypointRoute.List) {
        waypointRoute = WaypointRoute.List
    }
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val isTopLevel = activeView != ActiveView.NOTE_EDITOR &&
        !(activeView == ActiveView.WAYPOINTS && waypointRoute != WaypointRoute.List)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevel,
        drawerContent = {
            MenuDrawerSheet(
                currentView = activeView,
                onNavigate = {
                    navigate(it)
                    scope.launch { drawerState.close() }
                },
                onSettingsClick = {
                    activeView = ActiveView.CALENDAR
                    settingsRequest++
                    scope.launch { drawerState.close() }
                },
                onBackupClick = {
                    showBackup = true
                    scope.launch { drawerState.close() }
                },
                onAboutClick = {
                    showAbout = true
                    scope.launch { drawerState.close() }
                },
                onUpdateClick = {
                    updateRequest = true
                    scope.launch { drawerState.close() }
                },
                onChangelogClick = {
                    showChangelog = true
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                val openMenu: () -> Unit = { scope.launch { drawerState.open() } }

                // Calendar ↔ Notes morph: both keep their state (and ViewModels), nothing is reloaded.
                AnimatedContent(
                    targetState = activeView,
                    label = "main_content",
                    transitionSpec = {
                        val spec = tween<Float>(320, easing = FastOutSlowInEasing)
                        (fadeIn(spec) + scaleIn(spec, initialScale = 0.94f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)))
                            .togetherWith(fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 1.03f))
                    }
                ) { view ->
                    saveableStateHolder.SaveableStateProvider(if (view == ActiveView.NOTE_EDITOR) "${view.name}-$editorSession" else view.name) {
                        ReadableWidth(enabled = view != ActiveView.NOTES) {
                        when (view) {
                            ActiveView.CALENDAR -> CalendarScreen(
                                viewModel = calendarViewModel,
                                onSmartAddClick = { openSmartAdd() },
                                onGlobalSearchClick = { showUniversalSearch = true },
                                allNotes = pinnedNotes.map {
                                    PinnedNote(
                                        id = it.note.id,
                                        title = it.note.title,
                                        content = markdownToPlainText(it.note.content),
                                        pinnedToDate = it.note.pinnedToDate
                                    )
                                },
                                onNoteClick = { noteId -> openNoteEditor(noteId) },
                                documents = allDocuments.map { it.id to it.title },
                                onOpenDocument = { id ->
                                    activeView = ActiveView.DOCUMENTS
                                    documentToOpen = id
                                },
                                onCreateNoteForEvent = { event ->
                                    // Note pinned to the event's day and linked both ways
                                    val day = java.time.Instant.ofEpochMilli(event.startTimeMillis).atZone(ZoneId.systemDefault())
                                        .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    notesViewModel.saveNote(
                                        com.example.lifeorganizer.notes.ui.NoteDraft(
                                            id = null, title = event.title, content = "", colorLabel = null, isPinned = false,
                                            pinnedToDate = day, templateId = null, labelIds = emptySet()
                                        ),
                                        sessionKey = "event-${event.id}-${System.currentTimeMillis()}"
                                    ) { noteId ->
                                        calendarViewModel.linkNote(event.id, noteId)
                                        openNoteEditor(noteId)
                                    }
                                },
                                onMenuClick = openMenu,
                                showFabs = false,
                                addEventRequest = addEventRequest,
                                settingsRequest = settingsRequest,
                                focusRequest = calendarFocus,
                                onFocusHandled = { calendarFocus = null },
                                // Calendar and waypoints share places: names on cards, suggestions in the dialog.
                                placeName = { address ->
                                    val key = com.example.lifeorganizer.core.util.Places.key(address)
                                    key?.let { k -> waypoints.firstOrNull { com.example.lifeorganizer.core.util.Places.key(it.waypoint.address) == k }?.waypoint?.name }
                                },
                                placeSuggestions = waypoints
                                    .sortedWith(compareByDescending<com.example.lifeorganizer.waypoints.data.WaypointWithLabels> { it.waypoint.isPinned }
                                        .thenByDescending { it.waypoint.lastUsed })
                                    .filter { com.example.lifeorganizer.core.util.Places.hasPlace(it.waypoint.address) }
                                    .map { it.waypoint.name to it.waypoint.address },
                                prefillRequest = eventPrefill,
                                onPrefillHandled = { eventPrefill = null }
                            )

                            ActiveView.NOTES -> NotesScreen(
                                viewModel = notesViewModel,
                                onNoteClick = { noteId -> openNoteEditor(noteId) },
                                onCreateNote = { createNote() },
                                onGlobalSearchClick = { showUniversalSearch = true },
                                onMenuClick = openMenu
                            )

                            ActiveView.NOTE_EDITOR -> NoteEditorScreen(
                                viewModel = notesViewModel,
                                noteId = editingNoteId,
                                templateId = editorTemplateId,
                                labelId = editorLabelId,
                                onBack = { activeView = editorReturnView },
                                documents = allDocuments.map { it.id to it.title },
                                onOpenDocument = { id ->
                                    activeView = ActiveView.DOCUMENTS
                                    documentToOpen = id
                                },
                                onCreateReminder = { noteId, title, timeMillis ->
                                    calendarViewModel.addEvent(
                                        title = "${Str.reminderPrefix.of(lang)}: $title",
                                        description = "",
                                        startTimeMillis = timeMillis,
                                        endTimeMillis = timeMillis + 30 * 60_000L,
                                        isAllDay = false,
                                        reminderOffsets = listOf(0L),
                                        linkedNoteId = noteId
                                    )
                                    scope.launch { snackbarHostState.showSnackbar(Str.reminderCreated.of(lang)) }
                                }
                            )

                            ActiveView.WAYPOINTS -> WaypointsHost(
                                viewModel = waypointsViewModel,
                                route = waypointRoute,
                                onRouteChange = { waypointRoute = it },
                                onMenuClick = openMenu,
                                onPlanEvent = { wp ->
                                    // New event at this place, next full hour, 1 hour long
                                    val start = java.time.LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    eventPrefill = SmartResult.Event(
                                        title = wp.waypoint.name,
                                        description = "",
                                        location = wp.waypoint.address,
                                        startTimeMillis = start,
                                        endTimeMillis = start + 3_600_000L
                                    )
                                    waypointsViewModel.markWaypointUsed(wp.waypoint.id)
                                    activeView = ActiveView.CALENDAR
                                }
                            )

                            ActiveView.DOCUMENTS -> DocumentsHost(
                                navController = docsNavController,
                                viewModel = documentViewModel,
                                onMenuClick = openMenu,
                                openDocumentId = documentToOpen,
                                onDocumentOpened = { documentToOpen = null }
                            )
                            ActiveView.MONEY -> com.example.lifeorganizer.money.ui.MoneyHost(onMenuClick = openMenu)
                        }
                    }
}
                }

                // Unified action cluster: Smart Add, contextual "+" and the calendar/notes switcher in the corner.
                val showCluster = activeView == ActiveView.CALENDAR || activeView == ActiveView.NOTES
                AnimatedVisibility(
                    visible = showCluster,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 16.dp),
                    enter = fadeIn() + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)),
                    exit = fadeOut() + scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = { openSmartAdd() },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = Str.smartAdd.text())
                        }
                        FloatingActionButton(
                            onClick = {
                                if (activeView == ActiveView.NOTES) createNote() else addEventRequest++
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = if (activeView == ActiveView.NOTES) Str.newNote.text() else Str.newEvent.text()
                            )
                        }
                        ViewSwitcherButton(
                            showingNotes = activeView == ActiveView.NOTES,
                            onToggle = {
                                activeView = if (activeView == ActiveView.NOTES) ActiveView.CALENDAR else ActiveView.NOTES
                            }
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        // Stay left of the action cluster in the bottom-right corner
                        .padding(bottom = 8.dp, end = 72.dp)
                )

                if (showChangelog) ChangelogDialog(onDismiss = { showChangelog = false })
                if (showAbout) AboutScreen(onBack = { showAbout = false })
                if (updateRequest || foundUpdate != null) {
                    com.example.lifeorganizer.update.UpdateDialog(known = foundUpdate) { updateRequest = false; foundUpdate = null }
                }

                if (showTemplatePicker) {
                    TemplatePickerDialog(
                        templates = templates,
                        labels = noteLabels,
                        onDismiss = { showTemplatePicker = false },
                        onPick = { templateId ->
                            showTemplatePicker = false
                            openNoteEditor(null, templateId)
                        },
                        onPickLabel = { labelId ->
                            showTemplatePicker = false
                            openNoteEditor(null, labelId = labelId)
                        }
                    )
                }

                if (showSmartAdd) {
                    // Without a key Smart Add runs offline (rule-based); the key can be set in the settings.
                    run {
                        val contextData = SmartAddEngine.ContextData(
                            apiKey = geminiApiKey.orEmpty(),
                            categories = categories.map { it.id to it.name },
                            waypoints = waypoints.map { it.waypoint.name to it.waypoint.address },
                            moneyCategories = moneyCategories.map { it.name }
                        )
                        SmartAddDialog(
                            contextData = contextData,
                            initialText = smartAddInitialText,
                            initialImageUri = smartAddImageUri,
                            startWith = smartAddStart,
                            onDismiss = { showSmartAdd = false },
                            onResult = { results ->
                                showSmartAdd = false
                                saveSmartResults(results, calendarViewModel, notesViewModel, moneyRepository, scope)
                                val message = when {
                                    results.size > 1 -> "${results.size} ${Str.savedItems.of(lang)}"
                                    results.first() is SmartResult.Event -> Str.savedEvent.of(lang)
                                    results.first() is SmartResult.Transaction -> Str.savedTransaction.of(lang)
                                    else -> Str.savedNote.of(lang)
                                }
                                // Jump to the day of the (first) new event so the result is visible.
                                (results.firstOrNull { it is SmartResult.Event } as? SmartResult.Event)?.let { event ->
                                    activeView = ActiveView.CALENDAR
                                    calendarFocus = Instant.ofEpochMilli(event.startTimeMillis)
                                        .atZone(ZoneId.systemDefault()).toLocalDate() to null
                                }
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showBackup,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150))
                ) {
                    BackupScreen(
                        onClose = { showBackup = false },
                        pendingImport = importUri.takeIf { intentsEnabled },
                        onPendingImportHandled = onImportHandled
                    )
                }

                AnimatedVisibility(
                    visible = showUniversalSearch,
                    enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.98f),
                    exit = fadeOut(tween(150))
                ) {
                    UniversalSearchScreen(
                        calendarViewModel = calendarViewModel,
                        notesViewModel = notesViewModel,
                        waypointsViewModel = waypointsViewModel,
                        documentViewModel = documentViewModel,
                        onClose = { showUniversalSearch = false },
                        onEventClick = { eventId, startMillis ->
                            showUniversalSearch = false
                            activeView = ActiveView.CALENDAR
                            calendarFocus = Instant.ofEpochMilli(startMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate() to eventId
                        },
                        onNoteClick = { noteId ->
                            showUniversalSearch = false
                            openNoteEditor(noteId)
                        },
                        onWaypointClick = { waypointId ->
                            showUniversalSearch = false
                            activeView = ActiveView.WAYPOINTS
                            waypointRoute = WaypointRoute.Edit(waypointId)
                        },
                        onDocumentClick = { documentId ->
                            showUniversalSearch = false
                            activeView = ActiveView.DOCUMENTS
                            documentToOpen = documentId
                        }
                    )
                }
            }
        }
    }
}

private fun saveSmartResults(
    results: List<SmartResult>,
    calendarViewModel: CalendarViewModel,
    notesViewModel: NotesViewModel,
    moneyRepository: com.example.lifeorganizer.money.data.MoneyRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    results.forEach { result ->
        when (result) {
            is SmartResult.Transaction -> scope.launch {
                moneyRepository.addFromSmartAdd(result.title, result.amountCents, result.epochDay, result.categoryName)
            }
            is SmartResult.Event -> calendarViewModel.addEvent(
                title = result.title,
                description = result.description,
                startTimeMillis = result.startTimeMillis,
                endTimeMillis = result.endTimeMillis,
                isAllDay = false,
                reminderOffsets = result.reminderMinutesBefore.map { it * 60 * 1000L },
                targetAddress = com.example.lifeorganizer.core.util.Places.clean(result.location),
                categoryId = result.categoryId,
                isBirthday = result.isBirthday,
                birthYear = result.birthYear,
                recurrenceRule = if (result.isBirthday) "FREQ=YEARLY" else null
            )
            is SmartResult.Note -> notesViewModel.insertNote(
                com.example.lifeorganizer.notes.data.Note(
                    title = result.title,
                    // Checklists use the same "- [ ]" syntax the editor renders as checkboxes.
                    content = if (result.isChecklist) toChecklist(result.content) else result.content,
                    isChecklist = result.isChecklist,
                    colorLabel = result.colorLabel,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

private val existingItem = Regex("^\\s*[-*]\\s+\\[[ xX]]\\s")
private val bullet = Regex("^\\s*([-*•]|\\d+[.)])\\s+")

internal fun toChecklist(content: String): String =
    content.lines().filter { it.isNotBlank() }.joinToString("\n") { line ->
        if (existingItem.containsMatchIn(line)) line.trim() else "- [ ] " + line.replace(bullet, "").trim()
    }

/** On tablets / landscape, single-column screens stay at a readable width instead of stretching. */
@Composable
private fun ReadableWidth(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) { content(); return }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxHeight().widthIn(max = 840.dp)) { content() }
    }
}
