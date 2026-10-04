package com.example.lifeorganizer.calendar.ui

import com.example.lifeorganizer.calendar.util.SeriesScope
import androidx.compose.material.icons.filled.FilterList
import com.example.lifeorganizer.calendar.device.DeviceCalendar
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lifeorganizer.calendar.data.EventColor
import com.example.lifeorganizer.calendar.data.EventWithReminders
import com.example.lifeorganizer.calendar.data.RecurrenceType
import com.example.lifeorganizer.calendar.debug.DebugLogger
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import com.example.lifeorganizer.core.theme.ThemedLabelChip
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import java.time.LocalTime
import java.time.Month
import com.example.lifeorganizer.calendar.util.IcsExporter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.alpha
import com.example.lifeorganizer.calendar.data.Category

data class PinnedNote(
    val id: Long,
    val title: String,
    val content: String,
    val pinnedToDate: Long?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    openAgenda: Boolean = false,
    onAgendaOpened: () -> Unit = {},
    onSmartAddClick: (() -> Unit)? = null,
    onGlobalSearchClick: (() -> Unit)? = null,
    allNotes: List<PinnedNote> = emptyList(),
    onNoteClick: ((Long) -> Unit)? = null,
    /** Creates a note linked to the event (app shell), then opens it. */
    onCreateNoteForEvent: ((com.example.lifeorganizer.calendar.data.Event) -> Unit)? = null,
    /** Dokki documents (id to title) for attaching, and how to open one. */
    documents: List<Pair<Long, String>> = emptyList(),
    onOpenDocument: ((Long) -> Unit)? = null,
    onMenuClick: () -> Unit = {},
    // When hosted in LifeOrganizer the shell draws a shared FAB cluster and triggers these requests.
    showFabs: Boolean = true,
    addEventRequest: Int = 0,
    settingsRequest: Int = 0,
    focusRequest: Pair<LocalDate, Long?>? = null,
    onFocusHandled: () -> Unit = {},
    /** Resolves an address to a saved waypoint name (provided by the app shell). */
    placeName: (String?) -> String? = { null },
    /** Saved places (name to address) offered in the event dialog's address field. */
    placeSuggestions: List<Pair<String, String>> = emptyList(),
    /** Opens the new-event dialog pre-filled (e.g. "plan an event here" from a waypoint). */
    prefillRequest: com.example.lifeorganizer.core.smartadd.SmartResult.Event? = null,
    onPrefillHandled: () -> Unit = {}
) {
    val eventsWithReminders by viewModel.eventsWithReminders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val preferredTravelMode by viewModel.travelMode.collectAsState()
    val useHomeAsOrigin by viewModel.useHomeAsOrigin.collectAsState()
    val useBrowserForMaps by viewModel.useBrowserForMaps.collectAsState()
    val homeAddress by viewModel.homeAddress.collectAsState()
    val lang by viewModel.languageCode.collectAsState()
    val upcomingEvents by viewModel.getUpcomingEvents(3).collectAsState()
    val categories by viewModel.categories.collectAsState()
    val eventTemplates by viewModel.eventTemplates.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val enableSmartAlarms by viewModel.enableSmartAlarms.collectAsState()

    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var viewingEvent by remember { mutableStateOf<EventWithReminders?>(null) }
    var editingEvent by remember { mutableStateOf<EventWithReminders?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<EventWithReminders?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSmartAddDialog by remember { mutableStateOf(false) }
    var smartAddResult by remember { mutableStateOf<com.example.lifeorganizer.core.smartadd.SmartResult.Event?>(null) }
    var snackbarAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var duplicateTargetDate by remember { mutableStateOf<LocalDate?>(null) }

    var viewMode by rememberSaveable { mutableStateOf(ViewMode.MONTH) }
    val masterEvents by viewModel.masterEvents.collectAsState()
    // Occurrences of a series map back to the stored event (real start, real reminder ids).
    fun master(item: EventWithReminders): EventWithReminders =
        masterEvents.firstOrNull { it.event.id == item.event.id } ?: item

    // Remember which external requests were already handled so re-entering the screen doesn't replay them.
    var handledAddRequest by rememberSaveable { mutableIntStateOf(addEventRequest) }
    var handledSettingsRequest by rememberSaveable { mutableIntStateOf(settingsRequest) }

    LaunchedEffect(addEventRequest) {
        if (addEventRequest != handledAddRequest) {
            handledAddRequest = addEventRequest
            showAddDialog = true
        }
    }
    LaunchedEffect(prefillRequest) {
        if (prefillRequest != null) {
            smartAddResult = prefillRequest
            onPrefillHandled()
        }
    }
    LaunchedEffect(settingsRequest) {
        if (settingsRequest != handledSettingsRequest) {
            handledSettingsRequest = settingsRequest
            showSettings = true
        }
    }

    BackHandler(enabled = viewMode != ViewMode.MONTH) {
        viewMode = ViewMode.MONTH
    }

    LaunchedEffect(openAgenda) {
        if (openAgenda) {
            viewMode = ViewMode.AGENDA
            onAgendaOpened()
        }
    }

    val scope = rememberCoroutineScope()
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(1200) }
    val endMonth = remember { currentMonth.plusMonths(1200) }
    val daysOfWeek = remember { daysOfWeek() }
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val dragDropState = rememberDragDropState()

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    // Jump to a date (and optionally open an event), e.g. from universal search or Smart Add.
    LaunchedEffect(focusRequest) {
        val (date, eventId) = focusRequest ?: return@LaunchedEffect
        viewMode = ViewMode.MONTH
        selectedDate = date
        state.animateScrollToMonth(YearMonth.from(date))
        if (eventId != null) {
            viewingEvent = eventsWithReminders.firstOrNull { it.event.id == eventId }
        }
        onFocusHandled()
    }

    val travelInfo by remember { com.example.lifeorganizer.calendar.worker.TravelInfoStore.observe(context) }.collectAsState()

    // Pre-compute event dates for O(1) calendar day lookup
    val eventDates = remember(eventsWithReminders) {
        eventsWithReminders.map {
            Instant.ofEpochMilli(it.event.startTimeMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    }

    val defaultEventColor = MaterialTheme.colorScheme.primary
    val eventColorsByDate = remember(eventsWithReminders, defaultEventColor) {
        val map = mutableMapOf<LocalDate, MutableList<Color>>()
        eventsWithReminders.filter { !it.event.isBirthday }.forEach {
            val date = Instant.ofEpochMilli(it.event.startTimeMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val color = it.category?.color?.let { c -> Color(c) } 
                ?: it.event.color?.let { c -> Color(c) } 
                ?: defaultEventColor
            
            val list = map.getOrPut(date) { mutableListOf() }
            if (!list.contains(color)) list.add(color)
        }
        map
    }

    val birthdayDates = remember(eventsWithReminders) {
        eventsWithReminders.filter { it.event.isBirthday }
            .map {
                Instant.ofEpochMilli(it.event.startTimeMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .toSet()
    }

    // Repeating events first ask whether a change is for this occurrence, the following ones or all.
    var seriesPrompt by remember { mutableStateOf<Pair<EventWithReminders, Boolean>?>(null) } // occurrence, isDelete
    var editScope by remember { mutableStateOf(SeriesScope.ALL) }
    var editOccurrence by remember { mutableStateOf<EventWithReminders?>(null) }
    var copyRequest by remember { mutableStateOf<EventWithReminders?>(null) }
    fun isSeries(item: EventWithReminders) = !master(item).event.recurrenceRule.isNullOrBlank()
    fun requestEdit(item: EventWithReminders) {
        if (DeviceCalendar.isDeviceEvent(item.event)) { scope.launch { snackbarHostState.showSnackbar(Str.deviceEventReadOnly.of(lang)) }; return }
        if (isSeries(item)) seriesPrompt = item to false
        else { editScope = SeriesScope.ALL; editOccurrence = null; editingEvent = master(item) }
    }
    fun requestDelete(item: EventWithReminders) {
        if (DeviceCalendar.isDeviceEvent(item.event)) { scope.launch { snackbarHostState.showSnackbar(Str.deviceEventReadOnly.of(lang)) }; return }
        if (isSeries(item)) seriesPrompt = item to true else showDeleteConfirm = master(item)
    }

    // Snackbar helper
    fun showSnackbarMessage(message: String, action: (() -> Unit)? = null) {
        snackbarMessage = message
        snackbarAction = action
        showSnackbar = true
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (action != null) com.example.lifeorganizer.core.i18n.Str.undo.of(lang) else null,
                duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) action?.invoke()
            showSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("LifeOrganizer", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = Str.menu.text())
                    }
                },
                actions = {
                    IconButton(onClick = { onGlobalSearchClick?.invoke() }) {
                        Icon(Icons.Default.Search, contentDescription = Str.globalSearch.text())
                    }
                        IconButton(onClick = {
                            scope.launch { state.scrollToMonth(YearMonth.now()) }
                            selectedDate = LocalDate.now()
                        }) {
                            Icon(Icons.Default.Today, contentDescription = Translations.get(TransKey.GO_TO_TODAY, lang))
                        }
                        IconButton(onClick = {
                            viewMode = when (viewMode) {
                                ViewMode.MONTH -> ViewMode.WEEK
                                ViewMode.WEEK -> ViewMode.AGENDA
                                ViewMode.AGENDA -> ViewMode.MONTH
                            }
                        }) {
                            val icon = when (viewMode) {
                                ViewMode.MONTH -> Icons.AutoMirrored.Filled.ViewList
                                ViewMode.WEEK -> Icons.Default.CalendarMonth
                                ViewMode.AGENDA -> Icons.Default.ViewWeek
                            }
                            val desc = when (viewMode) {
                                ViewMode.MONTH -> Str.weekView.text()
                                ViewMode.WEEK -> Str.agendaView.text()
                                ViewMode.AGENDA -> Str.monthView.text()
                            }
                            Icon(icon, contentDescription = desc)
                        }

                    CalendarFilterButton(viewModel, categories)
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.Settings, contentDescription = Str.settings.text())
                    }
                }
            )
        },
        floatingActionButton = {
            if (showFabs) Column(horizontalAlignment = Alignment.End) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { 
                            onSmartAddClick?.invoke()
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Smart Add")
                    }
                    FloatingActionButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = Str.addEvent.text())
                    }
                }
            }
        },
        // Left of the shell's floating action cluster so the Undo button stays reachable
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(end = if (showFabs) 0.dp else 72.dp)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View switcher
            if (viewMode == ViewMode.MONTH) {
                val displayEvents = if (searchQuery.isNotBlank()) {
                    eventsWithReminders.filter {
                        it.event.title.contains(searchQuery, ignoreCase = true) ||
                        it.event.description.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    eventsWithReminders.filter {
                        Instant.ofEpochMilli(it.event.startTimeMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                    }
                }

                val dayNotes = if (searchQuery.isNotBlank()) {
                    emptyList()
                } else {
                    allNotes.filter { note ->
                        note.pinnedToDate != null &&
                        Instant.ofEpochMilli(note.pinnedToDate)
                            .atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Room for the floating action cluster
                    contentPadding = PaddingValues(bottom = 200.dp)
                ) {
                    item {
                        if (upcomingEvents.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(true) }
                            AnimatedVisibility(
                                visible = expanded,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clickable { viewMode = ViewMode.AGENDA },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            Translations.get(TransKey.UPCOMING_EVENTS, lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        upcomingEvents.take(3).forEach { event ->
                                            val eventDate = Instant.ofEpochMilli(event.event.startTimeMillis)
                                                .atZone(ZoneId.systemDefault()).toLocalDate()
                                            val dateLabel = when (eventDate) {
                                                LocalDate.now() -> Translations.get(TransKey.TODAY, lang)
                                                LocalDate.now().plusDays(1) -> Translations.get(TransKey.TOMORROW, lang)
                                                else -> eventDate.format(DateTimeFormatter.ofPattern("MMM dd", Locale.Builder().setLanguage(lang).build()))
                                            }
                                            val birthdayBadge = if (event.event.isBirthday) {
                                                val occurrenceYear = eventDate.year
                                                val age = event.event.birthYear?.let { occurrenceYear - it }
                                                val ageText = age?.let { " (${Translations.get(TransKey.AGE_TURNS, lang)} $it)" } ?: ""
                                                " ${Translations.get(TransKey.CAKE_EMOJI, lang)}$ageText"
                                            } else ""
                                            Text(
                                                "• ${event.event.title}$birthdayBadge — $dateLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Month Header
                        val visibleMonth = state.firstVisibleMonth.yearMonth
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showJumpDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage(lang).build())} ${visibleMonth.year}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        // Day Headers
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            daysOfWeek.forEach { dayOfWeek ->
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.Builder().setLanguage(lang).build()).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalCalendar(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                            dayContent = { day ->
                                Day(
                                    day = day,
                                    isSelected = selectedDate == day.date,
                                    eventColors = eventColorsByDate[day.date] ?: emptyList(),
                                    hasBirthday = birthdayDates.contains(day.date),
                                    dragDropState = dragDropState,
                                    onClick = {
                                        selectedDate = it.date
                                    }
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd", Locale.Builder().setLanguage(lang).build())),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (displayEvents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    Translations.get(TransKey.NO_EVENTS, lang),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Several events at the same place on one day are shown together.
                    val dayEntries = if (searchQuery.isNotBlank()) displayEvents.map { DayEntry.Single(it) }
                    else EventGrouping.byPlace(displayEvents)
                    val renderEvent: @Composable (EventWithReminders, Boolean) -> Unit = { item, showNavigation ->
                        var itemGlobalPosition by remember { mutableStateOf(Offset.Zero) }
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    itemGlobalPosition = coordinates.localToWindow(Offset.Zero)
                                }
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            dragDropState.onDragStart(item, itemGlobalPosition)
                                        },
                                        onDragEnd = {
                                            // Handled by the overlay
                                        },
                                        onDragCancel = {
                                            dragDropState.onDragCancel()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            // The overlay's detectDragGestures might not get this, so we feed it to the state
                                            dragDropState.onDrag(dragAmount)
                                        }
                                    )
                                }
                        ) {
                            EventCard(
                                item = item,
                                travelMode = preferredTravelMode,
                                useHomeAsOrigin = useHomeAsOrigin,
                                useBrowserForMaps = useBrowserForMaps,
                                homeAddress = homeAddress,
                                enableSmartAlarms = enableSmartAlarms,
                                lang = lang,
                                travelInfo = travelInfo[item.event.id],
                                placeLabel = placeName(item.event.targetAddress),
                                onDelete = { requestDelete(item) },
                                onEdit = { requestEdit(item) },
                                onView = { viewingEvent = item },
                                showNavigation = showNavigation,
                                modifier = Modifier.alpha(if (dragDropState.isDragging && dragDropState.draggingEvent?.event?.id == item.event.id) 0f else 1f)
                            )
                        }
                    }
                    items(dayEntries, key = { entry ->
                        when (entry) {
                            is DayEntry.Single -> "${entry.event.event.id}_${entry.event.event.startTimeMillis}"
                            is DayEntry.PlaceGroup -> "place_${entry.place}_${entry.firstStart}"
                        }
                    }) { entry ->
                        when (entry) {
                            is DayEntry.Single -> renderEvent(entry.event, true)
                            is DayEntry.PlaceGroup -> PlaceGroupCard(
                                place = placeName(entry.place) ?: entry.place,
                                count = entry.events.size,
                                lang = lang,
                                onNavigate = {
                                    openNavigation(context, entry.events.first(), preferredTravelMode, useHomeAsOrigin, homeAddress, useBrowserForMaps)
                                }
                            ) {
                                entry.events.forEach { renderEvent(it, false) }
                            }
                        }
                    }

                    if (dayNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = Str.pinnedNotes.of(lang),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        items(dayNotes, key = { "note_${it.id}" }) { noteItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { onNoteClick?.invoke(noteItem.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PushPin, contentDescription = Str.pinnedNote.text(), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(noteItem.title.ifBlank { Str.untitledNote.text() }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        if (noteItem.content.isNotBlank()) {
                                            Text(noteItem.content, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (viewMode == ViewMode.WEEK) {
                // Week view
                WeekView(
                    events = eventsWithReminders,
                    lang = lang,
                    currentDate = selectedDate,
                    onEventClick = { viewingEvent = it }
                )
            } else {
                // Agenda view
                val now = System.currentTimeMillis()
                val agendaEvents = remember(eventsWithReminders) {
                    eventsWithReminders.filter { e ->
                        val end = e.event.endTimeMillis ?: (e.event.startTimeMillis + 3600000)
                        if (e.event.isAllDay) {
                            val endOfDay = Instant.ofEpochMilli(e.event.startTimeMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            endOfDay > now
                        } else {
                            end > now
                        }
                    }.let { upcoming ->
                        // A weekly series appears once (next date) instead of every week for years.
                        EventGrouping.collapseSeries(upcoming, now)
                    }
                }
                AgendaView(
                    events = agendaEvents,
                    lang = lang,
                    onEventClick = { viewingEvent = it },
                    onEventEdit = { requestEdit(it) },
                    onEventDelete = { requestDelete(it) },
                    onEventView = { viewingEvent = it }
                )
            }
        }

        // Dialogs
        if (showSettings) {
            SettingsDialog(
                viewModel = viewModel,
                onClose = { showSettings = false },
                onExport = { showExportDialog = true }
            )
        }

        if (showExportDialog) {
            ExportOptionsDialog(
                categories = categories,
                onDismiss = { showExportDialog = false },
                onExport = { exportAll, exportBirthdays, selectedCategoryIds ->
                    // Masters only: exporting expanded occurrences duplicated every series on import.
                    val eventsToExport = if (exportAll) {
                        masterEvents
                    } else {
                        masterEvents.filter { event ->
                            (exportBirthdays && event.event.isBirthday) ||
                            (event.event.categoryId != null && selectedCategoryIds.contains(event.event.categoryId))
                        }
                    }
                    val uri = IcsExporter.exportEvents(context, eventsToExport)
                    uri?.let { IcsExporter.shareIcs(context, it) }
                    showExportDialog = false
                }
            )
        }

        if (showJumpDialog) {
            MonthYearJumpDialog(
                currentYearMonth = state.firstVisibleMonth.yearMonth,
                onDismiss = { showJumpDialog = false },
                onJump = { ym ->
                    scope.launch { state.scrollToMonth(ym) }
                    showJumpDialog = false
                },
                lang = lang
            )
        }

        viewingEvent?.let { event ->
            EventDetailDialog(
                eventWithReminders = event,
                lang = lang,
                onDismiss = { viewingEvent = null },
                onEdit = {
                    viewingEvent = null
                    requestEdit(event)
                },
                onDelete = {
                    viewingEvent = null
                    requestDelete(event)
                },
                onOpenNote = event.event.linkedNoteId?.let { noteId ->
                    { viewingEvent = null; onNoteClick?.invoke(noteId) }
                },
                documents = documents,
                onAttachDocument = if (DeviceCalendar.isDeviceEvent(event.event)) null else { id ->
                    viewingEvent = null
                    viewModel.attachDocument(event.event.id, id)
                },
                onOpenDocument = onOpenDocument?.let { open -> { id: Long -> viewingEvent = null; open(id) } },
                onCopyToDevice = if (DeviceCalendar.isDeviceEvent(event.event)) null else {
                    { viewingEvent = null; copyRequest = master(event) }
                },
                onCreateNote = onCreateNoteForEvent?.takeIf { !DeviceCalendar.isDeviceEvent(event.event) }?.let { create ->
                    { viewingEvent = null; create(master(event).event) }
                },
                onDuplicate = {
                    viewingEvent = null
                    duplicateTargetDate = selectedDate
                    scope.launch {
                        viewModel.duplicateEvent(event, event.event.startTimeMillis + 86400000L)
                        showSnackbarMessage(Translations.get(TransKey.EVENT_CREATED, lang))
                    }
                }
            )
        }

        copyRequest?.let { toCopy ->
            CopyToDeviceFlow(
                event = toCopy,
                onDone = { message -> copyRequest = null; message?.let { showSnackbarMessage(it) } }
            )
        }

        seriesPrompt?.let { (occurrence, isDelete) ->
            SeriesScopeDialog(
                isDelete = isDelete,
                onDismiss = { seriesPrompt = null },
                onPick = { scope ->
                    seriesPrompt = null
                    val m = master(occurrence)
                    if (isDelete) {
                        val before = m.event
                        if (scope != SeriesScope.ALL && viewModel.cutSeries(m, occurrence.event.startTimeMillis, scope)) {
                            showSnackbarMessage(Translations.get(TransKey.EVENT_DELETED, lang)) { viewModel.replaceEvent(before) }
                        } else showDeleteConfirm = m
                    } else if (scope == SeriesScope.ALL) {
                        editScope = SeriesScope.ALL; editOccurrence = null; editingEvent = m
                    } else {
                        editScope = scope; editOccurrence = occurrence; editingEvent = occurrence
                    }
                }
            )
        }

        showDeleteConfirm?.let { eventToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text(Translations.get(TransKey.CONFIRM_DELETE, lang)) },
                text = { Text(eventToDelete.event.title) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteEvent(eventToDelete)
                            showDeleteConfirm = null
                            showSnackbarMessage(
                                Translations.get(TransKey.EVENT_DELETED, lang)
                            ) {
                                viewModel.restoreEvent(eventToDelete)
                            }
                        }
                    ) {
                        Text(Translations.get(TransKey.YES, lang), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) {
                        Text(Translations.get(TransKey.NO, lang))
                    }
                }
            )
        }


        if (showAddDialog || editingEvent != null || smartAddResult != null) {
            AddEventDialog(
                // Editing keeps the event's own day; before, it silently moved to the day selected in the month view.
                selectedDate = editingEvent?.let {
                    Instant.ofEpochMilli(it.event.startTimeMillis).atZone(ZoneId.of(it.event.timezone)).toLocalDate()
                } ?: smartAddResult?.startTimeMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                } ?: selectedDate,
                editingEvent = editingEvent,
                lang = lang,
                categories = categories,
                prefilledSmartResult = smartAddResult,
                placeSuggestions = placeSuggestions,
                onDismiss = {
                    showAddDialog = false
                    editingEvent = null
                    editOccurrence = null
                    smartAddResult = null
                },
                checkConflict = { start, end, excludeId -> viewModel.checkConflict(start, end, excludeId) },
                eventTemplates = eventTemplates,
                onSaveTemplate = { name, title, description, isAllDay, addr, buffer, lead, categoryId, recurrence ->
                    viewModel.saveEventTemplate(name, title, description, isAllDay, addr, buffer, lead, categoryId, recurrence)
                    showSnackbarMessage(Str.templateSaved.of(lang))
                },
                onDeleteTemplate = { template ->
                    viewModel.deleteEventTemplate(template)
                },
                onSave = { title, description, startTime, endTime, isAllDay, offsets, addr, buffer, lead, color, recurrence, timezone, isBirthday, birthYear, categoryId ->
                    val occurrence = editOccurrence
                    if (editingEvent != null && occurrence != null && editScope != SeriesScope.ALL) {
                        // Only this / the following occurrences: cut them out of the series, store the change as a new event.
                        val m = master(occurrence)
                        if (viewModel.cutSeries(m, occurrence.event.startTimeMillis, editScope)) {
                            viewModel.addEvent(
                                title, description, startTime, endTime, isAllDay, offsets,
                                addr, buffer, lead, color, if (editScope == SeriesScope.THIS) null else recurrence,
                                timezone, isBirthday, birthYear, categoryId
                            )
                        } else {
                            viewModel.updateEvent(
                                m, title, description, startTime, endTime,
                                isAllDay, offsets, addr, buffer, lead, color, recurrence, timezone, isBirthday, birthYear, categoryId
                            )
                        }
                        showSnackbarMessage(Translations.get(TransKey.EVENT_SAVED, lang))
                    } else if (editingEvent != null) {
                        viewModel.updateEvent(
                            editingEvent!!, title, description, startTime, endTime,
                            isAllDay, offsets, addr, buffer, lead, color, recurrence, timezone, isBirthday, birthYear, categoryId
                        )
                        showSnackbarMessage(Translations.get(TransKey.EVENT_SAVED, lang))
                    } else {
                        viewModel.addEvent(
                            title, description, startTime, endTime, isAllDay, offsets,
                            addr, buffer, lead, color, recurrence, timezone, isBirthday, birthYear, categoryId
                        )
                        showSnackbarMessage(Translations.get(TransKey.EVENT_CREATED, lang))
                    }
                    showAddDialog = false
                    editingEvent = null
                    editOccurrence = null
                    smartAddResult = null
                }
            )
        }

        // Drag Drop Overlay
        if (dragDropState.isDragging && dragDropState.draggingEvent != null) {
            val event = dragDropState.draggingEvent!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                dragDropState.onDragEnd { occurrence, newDate ->
                                    if (DeviceCalendar.isDeviceEvent(occurrence.event)) return@onDragEnd
                                    // Move the stored event by the same distance the occurrence was dragged.
                                    val ev = master(occurrence)
                                    val currentStartTime = Instant.ofEpochMilli(occurrence.event.startTimeMillis).atZone(ZoneId.of(ev.event.timezone)).toLocalTime()
                                    val droppedMillis = newDate.atTime(currentStartTime).atZone(ZoneId.of(ev.event.timezone)).toInstant().toEpochMilli()
                                    val offset = droppedMillis - occurrence.event.startTimeMillis
                                    val newStartMillis = ev.event.startTimeMillis + offset
                                    val newEndMillis = ev.event.endTimeMillis?.let { it + offset }
                                    viewModel.updateEvent(
                                        eventWithReminders = ev,
                                        newTitle = ev.event.title,
                                        newDescription = ev.event.description,
                                        newStartTime = newStartMillis,
                                        newEndTime = newEndMillis,
                                        isAllDay = ev.event.isAllDay,
                                        newOffsets = ev.reminders.filter { it.type != "Birthday at 23:59" }.map { ev.event.startTimeMillis - it.reminderTimeMillis },
                                        targetAddress = ev.event.targetAddress,
                                        arrivalBuffer = ev.event.arrivalBufferMinutes,
                                        alarmLead = ev.event.alarmLeadMinutes,
                                        color = ev.event.color,
                                        recurrenceRule = ev.event.recurrenceRule,
                                        timezone = ev.event.timezone,
                                        isBirthday = ev.event.isBirthday,
                                        birthYear = ev.event.birthYear,
                                        categoryId = ev.event.categoryId
                                    )
                                }
                            },
                            onDragCancel = { dragDropState.onDragCancel() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDropState.onDrag(dragAmount)
                            }
                        )
                    }
            ) {
                // Dim background
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f))
                )
                // The dragged card
                Box(
                    modifier = Modifier
                        .offset { 
                            androidx.compose.ui.unit.IntOffset(
                                (dragDropState.dragPosition.x + dragDropState.dragOffset.x - 100).toInt(),
                                (dragDropState.dragPosition.y + dragDropState.dragOffset.y - 50).toInt()
                            )
                        }
                        .size(width = 250.dp, height = 80.dp)
                ) {
                    EventCard(
                        item = event,
                        travelMode = preferredTravelMode,
                        useHomeAsOrigin = useHomeAsOrigin,
                        useBrowserForMaps = useBrowserForMaps,
                        homeAddress = homeAddress,
                        enableSmartAlarms = enableSmartAlarms,
                        lang = lang,
                        onDelete = {}, onEdit = {}, onView = {}
                    )
                }
            }
        }
    }
}

@Composable
fun EventCard(
    item: EventWithReminders,
    travelMode: String,
    useHomeAsOrigin: Boolean,
    useBrowserForMaps: Boolean,
    homeAddress: String?,
    enableSmartAlarms: Boolean,
    lang: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
    travelInfo: com.example.lifeorganizer.calendar.worker.TravelInfo? = null,
    // Name of a saved waypoint at this address, shown instead of the raw address
    placeLabel: String? = null,
    showNavigation: Boolean = true
) {
    val context = LocalContext.current
    val eventColor = item.category?.color?.let { Color(it) } ?: item.event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val zone = remember(item.event.timezone) { runCatching { ZoneId.of(item.event.timezone) }.getOrDefault(ZoneId.systemDefault()) }
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    // No real place (empty or "N/A"-style placeholder): no address line, no navigation, no travel info.
    val place = com.example.lifeorganizer.core.util.Places.clean(item.event.targetAddress)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onView),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time column
            Column(modifier = Modifier.width(64.dp).padding(start = 14.dp)) {
                if (item.event.isAllDay || item.event.isBirthday) {
                    Text(
                        if (item.event.isBirthday) Translations.get(TransKey.CAKE_EMOJI, lang)
                        else Translations.get(TransKey.ALL_DAY, lang).split(" ").first(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        Instant.ofEpochMilli(item.event.startTimeMillis).atZone(zone).format(timeFormat),
                        style = MaterialTheme.typography.titleSmall
                    )
                    item.event.endTimeMillis?.let {
                        Text(
                            Instant.ofEpochMilli(it).atZone(zone).format(timeFormat),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Color accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(eventColor, RoundedCornerShape(2.dp))
            )

            Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp).weight(1f)) {
                Text(
                    text = item.event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                if (item.event.isBirthday) {
                    val occurrenceYear = Instant.ofEpochMilli(item.event.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate().year
                    val age = item.event.birthYear?.let { occurrenceYear - it }
                    Text(
                        text = buildString {
                            if (age != null && age > 0) append("${Translations.get(TransKey.AGE_TURNS, lang)} $age · ")
                            append(Translations.get(TransKey.BIRTHDAY, lang))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE5598A),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Inside a place group the place is already in the group header.
                if (place != null && showNavigation) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            placeLabel ?: place,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Calculated departure time (travel time from the home address)
                if (travelInfo != null && place != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            when (travelInfo.mode) {
                                "transit" -> Icons.Default.DirectionsTransit
                                "bicycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                                else -> Icons.Default.DirectionsCar
                            },
                            null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(6.dp))
                        val departure = Instant.ofEpochMilli(travelInfo.departureMillis).atZone(ZoneId.systemDefault()).format(timeFormat)
                        Text(
                            "${Str.leaveAt.of(lang)} $departure · ${travelInfo.durationMinutes} ${Str.minShort.of(lang)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                val visibleReminders = item.reminders.count { it.type != "Birthday at 23:59" }
                val showSmartAlarm = enableSmartAlarms && place != null
                val repeat = if (item.event.isBirthday) null else recurrenceLabel(item.event.recurrenceRule, lang)
                if (item.category != null || visibleReminders > 0 || showSmartAlarm || repeat != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        if (item.category != null) {
                            ThemedLabelChip(text = item.category.name, customColor = eventColor)
                        }
                        if (visibleReminders > 0) {
                            Icon(Icons.Default.NotificationsNone, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$visibleReminders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (showSmartAlarm) {
                            Icon(Icons.Default.Alarm, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                        }
                        if (repeat != null) {
                            Icon(Icons.Default.Repeat, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(repeat, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (place != null && showNavigation) {
                FilledTonalIconButton(
                    onClick = { openNavigation(context, item, travelMode, useHomeAsOrigin, homeAddress, useBrowserForMaps) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = Str.navigate.of(lang))
                }
            }
        }
    }
}

/** Container for several events at the same place on one day, with one shared navigation button. */
@Composable
private fun PlaceGroupCard(
    place: String,
    count: Int,
    lang: String,
    onNavigate: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(place, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(
                        "$count ${Str.events.of(lang)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Directions, contentDescription = Str.navigate.of(lang))
                }
            }
            content()
        }
    }
}

private fun openNavigation(
    context: android.content.Context,
    item: EventWithReminders,
    travelMode: String,
    useHomeAsOrigin: Boolean,
    homeAddress: String?,
    useBrowserForMaps: Boolean
) {
    val arrivalTimeMillis = item.event.startTimeMillis - (item.event.arrivalBufferMinutes * 60 * 1000L)
    val arrivalDateTime = Instant.ofEpochMilli(arrivalTimeMillis).atZone(ZoneId.systemDefault())
    val timeStr = arrivalDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val dateStr = arrivalDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val arrivalTimeSeconds = arrivalTimeMillis / 1000L

    val modeParam = when (travelMode) {
        "bicycling" -> "bicycling"
        "transit" -> "transit"
        else -> "driving"
    }

    val transitFlags = if (travelMode == "transit") "&dirflg=r&tt=arr&time=${Uri.encode(timeStr)}&date=${Uri.encode(dateStr)}" else ""
    val originStr = if (useHomeAsOrigin && !homeAddress.isNullOrBlank()) Uri.encode(homeAddress) else "My+Location"
    val destStr = Uri.encode(item.event.targetAddress)

    val uriString = "https://www.google.com/maps/dir/?api=1" +
            "&origin=$originStr" +
            "&destination=$destStr" +
            "&travelmode=$modeParam" +
            "&arrival_time=$arrivalTimeSeconds" +
            transitFlags

    DebugLogger.log("Navigating to arrival: $timeStr (Browser: $useBrowserForMaps)")

    val intent = Intent(Intent.ACTION_VIEW, uriString.toUri())
    if (useBrowserForMaps) {
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
    } else {
        intent.setPackage("com.google.android.apps.maps")
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uriString.toUri()))
    }
}

@Composable
fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    eventColors: List<Color>,
    hasBirthday: Boolean = false,
    dragDropState: DragDropState? = null,
    onClick: (CalendarDay) -> Unit
) {
    val today = LocalDate.now()
    val isToday = day.date == today

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
            )
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) }
            )
            .onGloballyPositioned { coordinates ->
                dragDropState?.let { state ->
                    if (day.position == DayPosition.MonthDate) {
                        state.dayCellBounds[day.date] = coordinates.boundsInWindow()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    day.position != DayPosition.MonthDate -> MaterialTheme.colorScheme.outlineVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (hasBirthday) {
                Text(
                    text = "🎂",
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (eventColors.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    eventColors.take(3).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else color,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onExport: () -> Unit
) {
    val homeAddress by viewModel.homeAddress.collectAsState()
    val travelMode by viewModel.travelMode.collectAsState()
    val useHomeAsOrigin by viewModel.useHomeAsOrigin.collectAsState()
    val useBrowserForMaps by viewModel.useBrowserForMaps.collectAsState()
    val lang by viewModel.languageCode.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val notificationSoundUri by viewModel.notificationSoundUri.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val enableSmartAlarms by viewModel.enableSmartAlarms.collectAsState()
    val useSystemAlarm by viewModel.useSystemAlarmForReminders.collectAsState()
    var homeAddressInput by remember { mutableStateOf("") }
    var geminiKeyInput by remember { mutableStateOf("") }
    var smartAlarmsEnabled by remember { mutableStateOf(enableSmartAlarms) }
    var systemAlarmEnabled by remember { mutableStateOf(useSystemAlarm) }
    var showDebugLogs by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var importMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(importMessage) {
        importMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            importMessage = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.importEventsFromUri(context, uri) { imported, skipped ->
                importMessage = Str.importResult.of(lang).format(imported, skipped)
            }
        }
    }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as? android.net.Uri
            }
            viewModel.saveNotificationSoundUri(uri?.toString())
        }
    }

    LaunchedEffect(homeAddress) {
        if (homeAddress != null) homeAddressInput = homeAddress!!
    }

    LaunchedEffect(geminiApiKey) {
        if (geminiApiKey != null) geminiKeyInput = geminiApiKey!!
    }

    LaunchedEffect(enableSmartAlarms) {
        smartAlarmsEnabled = enableSmartAlarms
    }

    LaunchedEffect(useSystemAlarm) {
        systemAlarmEnabled = useSystemAlarm
    }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                TopAppBar(
                    title = { Text(Translations.get(TransKey.SETTINGS_TITLE, lang)) },
                    actions = {
                        TextButton(onClick = onClose) { Text(Translations.get(TransKey.DONE, lang)) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme & Display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = Str.designStyle.of(lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            val designStyle by viewModel.designStyle.collectAsState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("white_light" to "White Light", "midnight" to "Midnight", "pastel" to "Pastel", "neon" to "Neon").forEach { (styleId, name) ->
                                    FilterChip(
                                        selected = designStyle == styleId,
                                        onClick = { viewModel.saveDesignStyle(styleId) },
                                        label = { Text(name) }
                                    )
                                }
                            }

                            if (designStyle == "pastel") {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                Text(
                                    text = Translations.get(TransKey.THEME, lang),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = isDarkTheme == false,
                                        onClick = { viewModel.saveDarkTheme(false) },
                                        label = { Text(Translations.get(TransKey.LIGHT, lang)) }
                                    )
                                    FilterChip(
                                        selected = isDarkTheme == true,
                                        onClick = { viewModel.saveDarkTheme(true) },
                                        label = { Text(Translations.get(TransKey.DARK, lang)) }
                                    )
                                    FilterChip(
                                        selected = isDarkTheme == null,
                                        onClick = { viewModel.saveDarkTheme(null) },
                                        label = { Text(Translations.get(TransKey.SYSTEM_DEFAULT, lang)) }
                                    )
                                }
                            }

                            if (designStyle == "pastel" || designStyle == "neon") {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                Text(
                                    text = Translations.get(TransKey.ACCENT_COLOR, lang),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                val accentColor by viewModel.accentColor.collectAsState()
                                val colorOptions = if (designStyle == "pastel") {
                                    listOf(
                                        null to "Mist Blue",
                                        0xFFF48FB1.toInt() to "Blush Pink",
                                        0xFFCE93D8.toInt() to "Lavender",
                                        0xFF90CAF9.toInt() to "Mist Blue",
                                        0xFF80CBC4.toInt() to "Soft Mint",
                                        0xFFFFCC80.toInt() to "Peach Cream"
                                    )
                                } else {
                                    listOf(
                                        null to "Electric Mint",
                                        0xFF00FFB2.toInt() to "Electric Mint",
                                        0xFF00E5FF.toInt() to "Neon Cyan",
                                        0xFFD4FF00.toInt() to "Acid Lime",
                                        0xFFD500F9.toInt() to "Plasma Purple",
                                        0xFFFF007F.toInt() to "Hot Magenta"
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    colorOptions.forEach { (colorValue, name) ->
                                        val isSelected = accentColor == colorValue || (accentColor == null && colorValue == null)
                                        val bgColor = if (colorValue == null) MaterialTheme.colorScheme.primary else Color(colorValue)
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(color = bgColor, shape = CircleShape)
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.saveAccentColor(colorValue) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (colorValue == null) {
                                                Icon(
                                                    Icons.Default.Palette,
                                                    contentDescription = name,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            
                            Text(
                                text = Translations.get(TransKey.LANGUAGE, lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                mapOf("en" to "EN", "de" to "DE", "tr" to "TR", "es" to "ES").forEach { (code, label) ->
                                    FilterChip(
                                        selected = lang == code,
                                        onClick = { viewModel.saveLanguageCode(code) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }

                    // Location & Navigation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = Str.locationNavigation.of(lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = homeAddressInput,
                                onValueChange = { homeAddressInput = it },
                                label = { Text(Translations.get(TransKey.HOME_ADDRESS, lang)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                trailingIcon = {
                                    TextButton(onClick = { viewModel.saveHomeAddress(homeAddressInput) }) {
                                        Text(Translations.get(TransKey.UPDATE, lang))
                                    }
                                }
                            )

                            ListItem(
                                headlineContent = { Text(Translations.get(TransKey.USE_HOME_ORIGIN, lang)) },
                                trailingContent = {
                                    Switch(
                                        checked = useHomeAsOrigin,
                                        onCheckedChange = { viewModel.saveUseHomeAsOrigin(it) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            ListItem(
                                headlineContent = { Text(Translations.get(TransKey.OPEN_BROWSER, lang)) },
                                trailingContent = {
                                    Switch(
                                        checked = useBrowserForMaps,
                                        onCheckedChange = { viewModel.saveUseBrowserForMaps(it) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            Text(
                                text = Translations.get(TransKey.TRAVEL_MODE, lang),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("driving", "bicycling", "transit").forEach { mode ->
                                    val key = when (mode) {
                                        "driving" -> TransKey.DRIVING
                                        "bicycling" -> TransKey.BICYCLING
                                        else -> TransKey.TRANSIT
                                    }
                                    FilterChip(
                                        selected = travelMode == mode,
                                        onClick = { viewModel.saveTravelMode(mode) },
                                        label = { Text(Translations.get(key, lang)) }
                                    )
                                }
                            }
                        }
                    }

                    // Notifications & Categories
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = Str.appPreferences.of(lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            val context = LocalContext.current
                            val soundName = notificationSoundUri?.let { uriStr ->
                                try {
                                    val ringtone = android.media.RingtoneManager.getRingtone(context, android.net.Uri.parse(uriStr))
                                    ringtone.getTitle(context) ?: Str.soundCustom.of(lang)
                                } catch (_: Exception) { Str.soundCustom.of(lang) }
                            } ?: Str.soundDefault.of(lang)

                            ListItem(
                                headlineContent = { Text(Str.notificationSound.of(lang)) },
                                supportingContent = { Text(soundName) },
                                modifier = Modifier.clickable {
                                    val intent = android.media.RingtoneManager.ACTION_RINGTONE_PICKER.let { action ->
                                        android.content.Intent(action).apply {
                                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select notification sound")
                                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, notificationSoundUri?.let { android.net.Uri.parse(it) })
                                        }
                                    }
                                    ringtonePicker.launch(intent)
                                },
                                trailingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            ListItem(
                                headlineContent = { Text(Str.categories.of(lang)) },
                                supportingContent = { Text(Str.categoriesDesc.of(lang)) },
                                modifier = Modifier.clickable { showCategoryManager = true },
                                trailingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            val enableAppLock by viewModel.enableAppLock.collectAsState()
                            ListItem(
                                headlineContent = { Text(Translations.get(TransKey.APP_LOCK, lang)) },
                                supportingContent = { Text(Translations.get(TransKey.APP_LOCK_DESCRIPTION, lang)) },
                                trailingContent = {
                                    Switch(
                                        checked = enableAppLock,
                                        onCheckedChange = { viewModel.saveEnableAppLock(it) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }

                    // Advanced & Export
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = Str.advancedExport.of(lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            OutlinedTextField(
                                value = geminiKeyInput,
                                onValueChange = { geminiKeyInput = it },
                                label = { Text(Str.groqKey.of(lang)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                trailingIcon = {
                                    TextButton(onClick = { viewModel.saveGeminiApiKey(geminiKeyInput.ifBlank { null }) }) {
                                        Text(Translations.get(TransKey.UPDATE, lang))
                                    }
                                }
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = Str.enableSmartAlarms.of(lang),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Switch(
                                    checked = smartAlarmsEnabled,
                                    onCheckedChange = {
                                        smartAlarmsEnabled = it
                                        viewModel.saveEnableSmartAlarms(it)
                                    }
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = Translations.get(TransKey.USE_SYSTEM_ALARM, lang),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Switch(
                                    checked = systemAlarmEnabled,
                                    onCheckedChange = {
                                        systemAlarmEnabled = it
                                        viewModel.saveUseSystemAlarmForReminders(it)
                                    }
                                )
                            }
                            DeviceCalendarRow(viewModel)
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            ListItem(
                                headlineContent = { Text(Translations.get(TransKey.EXPORT, lang)) },
                                modifier = Modifier.clickable { onExport() },
                                trailingContent = { Icon(androidx.compose.material.icons.Icons.Default.Share, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            
                            ListItem(
                                headlineContent = { Text(Translations.get(TransKey.IMPORT, lang)) },
                                modifier = Modifier.clickable { importLauncher.launch(arrayOf("*/*")) },
                                trailingContent = { Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            ListItem(
                                headlineContent = { Text(Translations.get(TransKey.SYSTEM_EVENTS, lang)) },
                                trailingContent = {
                                    Switch(
                                        checked = showDebugLogs,
                                        onCheckedChange = { showDebugLogs = it }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            
                            AnimatedVisibility(visible = showDebugLogs) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    TextButton(onClick = { DebugLogger.logs.clear() }) {
                                        Text(Translations.get(TransKey.CLEAR, lang))
                                    }
                                    DebugLogger.logs.forEach { log ->
                                        Text(
                                            text = log,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = categories,
            onAdd = { name, color -> viewModel.addCategory(name, color) },
            onDelete = { viewModel.deleteCategory(it) },
            onClose = { showCategoryManager = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    selectedDate: LocalDate,
    editingEvent: EventWithReminders? = null,
    lang: String,
    categories: List<Category>,
    prefilledSmartResult: com.example.lifeorganizer.core.smartadd.SmartResult.Event? = null,
    placeSuggestions: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit,
    checkConflict: (Long, Long?, Long?) -> EventWithReminders?,
    eventTemplates: List<com.example.lifeorganizer.calendar.data.EventTemplate> = emptyList(),
    onSaveTemplate: (String, String, String, Boolean, String?, Int, Int, Long?, String?) -> Unit,
    onDeleteTemplate: (com.example.lifeorganizer.calendar.data.EventTemplate) -> Unit,
    onSave: (String, String, Long, Long?, Boolean, List<Long>, String?, Int, Int, Int?, String?, String, Boolean, Int?, Long?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(prefilledSmartResult?.title ?: editingEvent?.event?.title ?: "") }
    var description by remember { mutableStateOf(prefilledSmartResult?.description ?: editingEvent?.event?.description ?: "") }
    var selectedRecurrence by remember { mutableStateOf(if (prefilledSmartResult?.isBirthday == true) "FREQ=YEARLY" else editingEvent?.event?.recurrenceRule) }
    var showCustomRecurrence by remember { mutableStateOf(false) }
    var isAllDay by remember { mutableStateOf(prefilledSmartResult?.isBirthday ?: editingEvent?.event?.isAllDay ?: false) }
    var targetAddress by remember { mutableStateOf(prefilledSmartResult?.location ?: editingEvent?.event?.targetAddress ?: "") }
    var arrivalBuffer by remember { mutableStateOf(editingEvent?.event?.arrivalBufferMinutes?.toString() ?: "10") }
    var alarmLead by remember { mutableStateOf(editingEvent?.event?.alarmLeadMinutes?.toString() ?: "5") }
    var selectedTimezone by remember { mutableStateOf(editingEvent?.event?.timezone ?: ZoneId.systemDefault().id) }
    var isBirthday by remember { mutableStateOf(prefilledSmartResult?.isBirthday ?: editingEvent?.event?.isBirthday ?: false) }
    var birthYear by remember { mutableStateOf(prefilledSmartResult?.birthYear?.toString() ?: editingEvent?.event?.birthYear?.toString() ?: "") }
    var selectedCategoryId by remember { mutableStateOf(prefilledSmartResult?.categoryId ?: editingEvent?.event?.categoryId) }

    val initialTime = if (prefilledSmartResult != null) {
        Instant.ofEpochMilli(prefilledSmartResult.startTimeMillis)
            .atZone(ZoneId.systemDefault()).toLocalTime()
    } else if (editingEvent != null) {
        Instant.ofEpochMilli(editingEvent.event.startTimeMillis)
            .atZone(ZoneId.of(editingEvent.event.timezone)).toLocalTime()
    } else LocalTime.of(12, 0)

    val initialEndTime = if (prefilledSmartResult != null) {
        Instant.ofEpochMilli(prefilledSmartResult.endTimeMillis)
            .atZone(ZoneId.systemDefault()).toLocalTime()
    } else editingEvent?.event?.endTimeMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.of(editingEvent.event.timezone)).toLocalTime()
    }

    val startTimeState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )
    val endTimeState = rememberTimePickerState(
        initialHour = initialEndTime?.hour ?: (initialTime.hour + 1),
        initialMinute = initialEndTime?.minute ?: initialTime.minute
    )

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(editingEvent?.event?.endTimeMillis != null) }

    val reminders = remember {
        mutableStateListOf<Pair<String, Long>>().apply {
            if (prefilledSmartResult != null && prefilledSmartResult.reminderMinutesBefore.isNotEmpty()) {
                prefilledSmartResult.reminderMinutesBefore.forEach { mins ->
                    val typeStr = if (mins % 1440 == 0) "${mins / 1440} days before"
                    else if (mins % 60 == 0) "${mins / 60} hours before"
                    else "$mins minutes before"
                    add(typeStr to mins * 60000L)
                }
            } else {
                editingEvent?.reminders?.forEach {
                    add(it.type to (editingEvent.event.startTimeMillis - it.reminderTimeMillis))
                }
            }
        }
    }

    var newReminderValue by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("Minutes") }

    val selectedCategoryColor = categories.firstOrNull { it.id == selectedCategoryId }?.color?.let { Color(it) }

    // Saves the entry; also used when the dialog is closed, so nothing typed gets lost.
    fun commit() {
                            val startTime = if (isAllDay) {
                                selectedDate.atStartOfDay(ZoneId.of(selectedTimezone)).toInstant().toEpochMilli()
                            } else {
                                selectedDate.atTime(startTimeState.hour, startTimeState.minute)
                                    .atZone(ZoneId.of(selectedTimezone)).toInstant().toEpochMilli()
                            }
                            val endTime = if (showEndTime && !isAllDay) {
                                selectedDate.atTime(endTimeState.hour, endTimeState.minute)
                                    .atZone(ZoneId.of(selectedTimezone)).toInstant().toEpochMilli()
                                    .let { if (it <= startTime) it + 86400000L else it }
                            } else null
                            onSave(
                                title,
                                description,
                                startTime,
                                endTime,
                                isAllDay,
                                reminders.map { it.second },
                                targetAddress.ifBlank { null },
                                arrivalBuffer.toIntOrNull() ?: 0,
                                alarmLead.toIntOrNull() ?: 0,
                                null,
                                selectedRecurrence,
                                selectedTimezone,
                                isBirthday,
                                birthYear.toIntOrNull(),
                                selectedCategoryId
                            )
                        }

    Dialog(
        // Closing (back / tap outside) saves automatically; "Discard" is the explicit way out.
        onDismissRequest = { if (title.isNotBlank()) commit() else onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.95f).padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            border = selectedCategoryColor?.let { BorderStroke(2.dp, it) }
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (editingEvent != null) Translations.get(TransKey.EDIT_EVENT, lang)
                    else Translations.get(TransKey.NEW_EVENT, lang),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Templates
                if (eventTemplates.isNotEmpty() && editingEvent == null && prefilledSmartResult == null) {
                    var templateDropdownExpanded by remember { mutableStateOf(false) }
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = templateDropdownExpanded,
                        onExpandedChange = { templateDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = Str.loadTemplate.text(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = templateDropdownExpanded,
                            onDismissRequest = { templateDropdownExpanded = false }
                        ) {
                            eventTemplates.forEach { template ->
                                DropdownMenuItem(
                                    text = { Text(template.name) },
                                    trailingIcon = {
                                        IconButton(onClick = { onDeleteTemplate(template) }) {
                                            Icon(Icons.Default.Delete, contentDescription = Str.deleteTemplate.text(), tint = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    onClick = {
                                        title = template.title
                                        description = template.description
                                        isAllDay = template.isAllDay
                                        targetAddress = template.targetAddress ?: ""
                                        arrivalBuffer = template.arrivalBufferMinutes.toString()
                                        alarmLead = template.alarmLeadMinutes.toString()
                                        selectedCategoryId = template.categoryId
                                        selectedRecurrence = template.recurrenceRule
                                        templateDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 1. Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Translations.get(TransKey.TITLE, lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 2. Category
                Text(Str.category.of(lang), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                var categoryDropdownExpanded by remember { mutableStateOf(false) }
                val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: Str.none.of(lang)
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Str.none.of(lang)) },
                            onClick = {
                                selectedCategoryId = null
                                categoryDropdownExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 3. All Day & Times
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = isAllDay, onCheckedChange = { isAllDay = it })
                    Text(Translations.get(TransKey.ALL_DAY, lang))
                }

                if (!isAllDay) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${Translations.get(TransKey.TIME, lang)}: ${String.format(Locale.getDefault(), "%02d:%02d", startTimeState.hour, startTimeState.minute)}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showEndTime,
                            onCheckedChange = { showEndTime = it }
                        )
                        Text(Translations.get(TransKey.END_TIME, lang))
                    }
                    if (showEndTime) {
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${Translations.get(TransKey.END_TIME, lang)}: ${String.format(Locale.getDefault(), "%02d:%02d", endTimeState.hour, endTimeState.minute)}")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 4. Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Translations.get(TransKey.DESCRIPTION, lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // 5. Recurrence & Birthday
                Text(Translations.get(TransKey.RECURRENCE, lang), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceType.ALL.forEach { recurrence ->
                        FilterChip(
                            selected = selectedRecurrence == recurrence.rrule,
                            onClick = { selectedRecurrence = recurrence.rrule },
                            label = { Text(Translations.get(TransKey.valueOf(recurrence.displayName.uppercase().replace("-", "")), lang), maxLines = 1, softWrap = false) }
                        )
                    }
                    FilterChip(
                        selected = selectedRecurrence != null && RecurrenceType.ALL.none { it.rrule == selectedRecurrence },
                        onClick = { showCustomRecurrence = true },
                        label = { Text("Custom...", maxLines = 1, softWrap = false) }
                    )
                }

                if (showCustomRecurrence) {
                    CustomRecurrenceDialog(
                        initialRule = selectedRecurrence,
                        onDismiss = { showCustomRecurrence = false },
                        onSave = { rule ->
                            selectedRecurrence = rule
                            showCustomRecurrence = false
                        }
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isBirthday,
                        onCheckedChange = {
                            isBirthday = it
                            if (it) {
                                selectedRecurrence = RecurrenceType.YEARLY.rrule
                                isAllDay = true
                            }
                        }
                    )
                    Text(
                        Translations.get(TransKey.BIRTHDAY, lang),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isBirthday) Color(0xFFFF6B9D) else MaterialTheme.colorScheme.onSurface
                    )
                    if (isBirthday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.get(TransKey.CAKE_EMOJI, lang))
                    }
                }
                AnimatedVisibility(visible = isBirthday) {
                    OutlinedTextField(
                        value = birthYear,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) birthYear = it },
                        label = { Text(Translations.get(TransKey.BIRTH_YEAR, lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // 6. Reminders
                Text(Translations.get(TransKey.REMINDERS, lang), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                reminders.forEach { reminder ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(reminder.first, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { reminders.remove(reminder) }) {
                            Icon(Icons.Default.Delete, contentDescription = Str.delete2.text(), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newReminderValue,
                        onValueChange = { if (it.all { char -> char.isDigit() }) newReminderValue = it },
                        placeholder = { Text(Translations.get(TransKey.VAL, lang), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    var expanded by remember { mutableStateOf(false) }
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        val currentLabel = when (selectedUnit) {
                            "Minutes" -> Translations.get(TransKey.MINUTES, lang)
                            "Hours" -> Translations.get(TransKey.HOURS, lang)
                            else -> Translations.get(TransKey.DAYS, lang)
                        }
                        OutlinedTextField(
                            value = currentLabel,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("Minutes", "Hours", "Days").forEach { unit ->
                                val label = when (unit) {
                                    "Minutes" -> Translations.get(TransKey.MINUTES, lang)
                                    "Hours" -> Translations.get(TransKey.HOURS, lang)
                                    else -> Translations.get(TransKey.DAYS, lang)
                                }
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedUnit = unit
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            val value = newReminderValue.toLongOrNull() ?: 0L
                            val offset = when (selectedUnit) {
                                "Minutes" -> value * 60 * 1000L
                                "Hours" -> value * 60 * 60 * 1000L
                                "Days" -> value * 24 * 60 * 60 * 1000L
                                else -> 0L
                            }
                            val unitLabel = when (selectedUnit) {
                                "Minutes" -> Translations.get(TransKey.MINUTES, lang).lowercase()
                                "Hours" -> Translations.get(TransKey.HOURS, lang).lowercase()
                                else -> Translations.get(TransKey.DAYS, lang).lowercase()
                            }
                            val label = if (value == 0L) Str.exactTime.of(lang) else Str.before.of(lang).format("$value $unitLabel")
                            reminders.add(label to offset)
                            newReminderValue = ""
                        },
                        enabled = newReminderValue.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = Str.addReminder.text())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // 7. Smart Alarm (Location)
                Text(Translations.get(TransKey.SMART_ALARM, lang), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = targetAddress,
                    onValueChange = { targetAddress = it },
                    label = { Text(Translations.get(TransKey.DESTINATION_ADDRESS, lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    isError = targetAddress.isNotBlank() && targetAddress.length < 5,
                    supportingText = {
                        if (targetAddress.isNotBlank() && targetAddress.length < 5) {
                            Text(Str.addressTooShort.text(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                // Saved waypoints as one-tap suggestions (filtered by what is typed)
                val query = targetAddress.trim()
                val matches = placeSuggestions.filter { (name, address) ->
                    !com.example.lifeorganizer.core.util.Places.samePlace(address, query) &&
                        (query.isEmpty() || name.contains(query, true) || address.contains(query, true))
                }.take(6)
                if (matches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matches.forEach { (name, address) ->
                            SuggestionChip(
                                onClick = { targetAddress = address },
                                label = { Text(name, maxLines = 1) },
                                icon = { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = arrivalBuffer,
                        onValueChange = { if (it.all { c -> c.isDigit() }) arrivalBuffer = it },
                        label = { Text(Translations.get(TransKey.ARR_BUFFER, lang), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = alarmLead,
                        onValueChange = { if (it.all { c -> c.isDigit() }) alarmLead = it },
                        label = { Text(Translations.get(TransKey.LEAD_TIME, lang), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    var showSaveTemplateDialog by remember { mutableStateOf(false) }
                    
                    if (showSaveTemplateDialog) {
                        var templateName by remember { mutableStateOf(title.takeIf { it.isNotBlank() } ?: "New Template") }
                        Dialog(onDismissRequest = { showSaveTemplateDialog = false }) {
                            Card(modifier = Modifier.padding(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(Str.saveAsTemplate2.text(), style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = templateName,
                                        onValueChange = { templateName = it },
                                        label = { Text(Str.templateName.text()) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { showSaveTemplateDialog = false }) { Text(Str.cancel2.text()) }
                                        Button(
                                            onClick = {
                                                onSaveTemplate(
                                                    templateName,
                                                    title,
                                                    description,
                                                    isAllDay,
                                                    targetAddress.ifBlank { null },
                                                    arrivalBuffer.toIntOrNull() ?: 0,
                                                    alarmLead.toIntOrNull() ?: 0,
                                                    selectedCategoryId,
                                                    selectedRecurrence
                                                )
                                                showSaveTemplateDialog = false
                                            },
                                            enabled = templateName.isNotBlank()
                                        ) { Text(Str.save.text()) }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(onClick = { showSaveTemplateDialog = true }, enabled = title.isNotBlank()) {
                        Text(Str.saveAsTemplate2.text())
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(com.example.lifeorganizer.core.i18n.Str.discard.of(lang)) }
                    Button(
                        onClick = { commit() },
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (editingEvent != null) Translations.get(TransKey.UPDATE, lang) else Translations.get(TransKey.CREATE, lang))
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        Dialog(onDismissRequest = { showStartTimePicker = false }) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    TimePicker(state = startTimeState)
                    TextButton(
                        onClick = { showStartTimePicker = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text(Translations.get(TransKey.DONE, lang)) }
                }
            }
        }
    }

    if (showEndTimePicker) {
        Dialog(onDismissRequest = { showEndTimePicker = false }) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    TimePicker(state = endTimeState)
                    TextButton(
                        onClick = { showEndTimePicker = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text(Translations.get(TransKey.DONE, lang)) }
                }
            }
        }
    }
}

@Composable
fun MonthYearJumpDialog(
    currentYearMonth: YearMonth,
    onDismiss: () -> Unit,
    onJump: (YearMonth) -> Unit,
    lang: String
) {
    var selectedYear by remember { mutableIntStateOf(currentYearMonth.year) }
    var step by remember { mutableIntStateOf(0) }

    val years = remember { (currentYearMonth.year - 50..currentYearMonth.year + 50).toList() }
    val currentYearIndex = years.indexOf(currentYearMonth.year)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentYearIndex - 3).coerceAtLeast(0)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(500.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (step == 0) Translations.get(TransKey.SELECT_YEAR, lang)
                    else Translations.get(TransKey.SELECT_MONTH, lang),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (step == 0) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(years) { year ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedYear = year
                                        step = 1
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (year == currentYearMonth.year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (year == currentYearMonth.year) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            onClick = { step = 0 },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = selectedYear.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val months = (1..12).toList()
                        months.chunked(3).forEach { rowMonths ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowMonths.forEach { month ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.5f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (month == currentYearMonth.monthValue && selectedYear == currentYearMonth.year)
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { onJump(YearMonth.of(selectedYear, month)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.Builder().setLanguage(lang).build()),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (month == currentYearMonth.monthValue && selectedYear == currentYearMonth.year)
                                                MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == 1) {
                        TextButton(onClick = { step = 0 }) {
                            Text(
                                Translations.get(TransKey.BACK, lang),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            Translations.get(TransKey.CANCEL, lang),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesScopeDialog(isDelete: Boolean, onDismiss: () -> Unit, onPick: (SeriesScope) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDelete) Str.deleteRepeating.text() else Str.editRepeating.text()) },
        text = {
            Column {
                // "From here on" is the usual choice, so it comes first and stands out.
                Button(onClick = { onPick(SeriesScope.FOLLOWING) }, modifier = Modifier.fillMaxWidth()) {
                    Text(Str.thisAndFollowing.text())
                }
                listOf(
                    SeriesScope.THIS to Str.thisEventOnly,
                    SeriesScope.ALL to Str.allEvents
                ).forEach { (scope, label) ->
                    TextButton(onClick = { onPick(scope) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label.text(), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel.text()) } }
    )
}

/** Settings row: show the phone's calendars (read-only) next to the app's own events. */
@Composable
private fun DeviceCalendarRow(viewModel: MainViewModel) {
    val context = LocalContext.current
    val enabled by viewModel.deviceCalendarEnabled.collectAsState()
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.setDeviceCalendarEnabled(true) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(Str.showDeviceCalendars.text(), style = MaterialTheme.typography.bodyLarge)
            Text(Str.showDeviceCalendarsDesc.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = enabled && DeviceCalendar.hasPermission(context),
            onCheckedChange = { on ->
                when {
                    !on -> viewModel.setDeviceCalendarEnabled(false)
                    DeviceCalendar.hasPermission(context) -> viewModel.setDeviceCalendarEnabled(true)
                    else -> permission.launch(android.Manifest.permission.READ_CALENDAR)
                }
            }
        )
    }
}

/** Asks for calendar access, lets the user pick a phone calendar and copies the event there. */
@Composable
private fun CopyToDeviceFlow(event: EventWithReminders, onDone: (String?) -> Unit) {
    val context = LocalContext.current
    val lang = com.example.lifeorganizer.core.i18n.LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var calendars by remember { mutableStateOf<List<Pair<Long, String>>?>(null) }
    fun load() {
        scope.launch {
            val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { DeviceCalendar.writableCalendars(context) }
            if (list.isEmpty()) onDone(Str.noWritableCalendar.of(lang)) else calendars = list
        }
    }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { granted -> if (granted.values.all { it }) load() else onDone(null) }
    LaunchedEffect(Unit) {
        if (DeviceCalendar.hasPermission(context) && DeviceCalendar.hasWritePermission(context)) load()
        else permission.launch(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR))
    }
    val list = calendars ?: return
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text(Str.copyToDevice.text()) },
        text = {
            Column {
                list.forEach { (id, name) ->
                    TextButton(onClick = {
                        scope.launch {
                            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                DeviceCalendar.copyToDevice(context, id, event) != null
                            }
                            onDone(if (ok) Str.copiedToDevice.of(lang) else Str.copyFailed.of(lang))
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(name, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text(Str.cancel.text()) } }
    )
}

/** Top-bar filter: show/hide repeating events and single categories; changes apply immediately. */
@Composable
private fun CalendarFilterButton(viewModel: MainViewModel, categories: List<com.example.lifeorganizer.calendar.data.Category>) {
    var open by remember { mutableStateOf(false) }
    val hideRecurring by viewModel.hideRecurring.collectAsState()
    val hidden by viewModel.hiddenCategories.collectAsState()
    val active = hideRecurring || hidden.isNotEmpty()
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Default.FilterList, contentDescription = Str.filter.text(),
                tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(Str.show.text(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            DropdownMenuItem(
                text = { Text(Str.repeatingEvents.text()) },
                leadingIcon = { Checkbox(checked = !hideRecurring, onCheckedChange = null) },
                onClick = { viewModel.setHideRecurring(!hideRecurring) }
            )
            categories.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name) },
                    leadingIcon = { Checkbox(checked = c.id !in hidden, onCheckedChange = null) },
                    trailingIcon = { Box(Modifier.size(12.dp).background(Color(c.color), CircleShape)) },
                    onClick = { viewModel.toggleCategoryHidden(c.id) }
                )
            }
        }
    }
}
