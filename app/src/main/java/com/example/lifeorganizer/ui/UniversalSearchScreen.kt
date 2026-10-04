package com.example.lifeorganizer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.documents.ui.screens.DocumentViewModel
import com.example.lifeorganizer.notes.ui.NotesViewModel
import com.example.lifeorganizer.notes.ui.markdownToPlainText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import com.example.lifeorganizer.calendar.ui.MainViewModel as CalendarViewModel
import com.example.lifeorganizer.waypoints.ui.MainViewModel as WaypointsViewModel

private data class SearchHit(
    val id: Long,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
fun UniversalSearchScreen(
    calendarViewModel: CalendarViewModel,
    notesViewModel: NotesViewModel,
    waypointsViewModel: WaypointsViewModel,
    documentViewModel: DocumentViewModel,
    onClose: () -> Unit,
    onEventClick: (eventId: Long, startMillis: Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onWaypointClick: (Long) -> Unit,
    onDocumentClick: (Long) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val lang = LocalAppLanguage.current

    // Unfiltered sources, independent of the per-module search / label filters.
    val allEvents by calendarViewModel.eventsWithReminders.collectAsState()
    val allNotes by notesViewModel.allActiveNotes.collectAsState()
    val allWaypoints by waypointsViewModel.allWaypoints.collectAsState()
    val allDocuments by documentViewModel.allDocuments.collectAsState()

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val q = query.trim()
    val dateFormatter = remember(lang) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang))
    }

    val eventHits = remember(q, allEvents, lang) {
        if (q.isEmpty()) emptyList() else allEvents
            .filter {
                it.event.title.contains(q, true) || it.event.description.contains(q, true) ||
                    it.event.targetAddress?.contains(q, true) == true
            }
            // A series is one hit: its next date (or the last one if it is over), not one per week.
            .let { hits ->
                val now = System.currentTimeMillis()
                hits.sortedWith(compareBy({ it.event.startTimeMillis < now }, { it.event.startTimeMillis }))
                    .distinctBy { it.event.id }
                    .sortedBy { it.event.startTimeMillis }
            }
            .map { e ->
                val date = Instant.ofEpochMilli(e.event.startTimeMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)
                SearchHit(e.event.id, e.event.title, listOfNotNull(date, e.event.targetAddress).joinToString(" · ")) {
                    onEventClick(e.event.id, e.event.startTimeMillis)
                }
            }
    }
    val noteHits = remember(q, allNotes, lang) {
        if (q.isEmpty()) emptyList() else allNotes
            .filter { it.note.title.contains(q, true) || it.note.content.contains(q, true) }
            .map { n ->
                SearchHit(
                    n.note.id,
                    n.note.title.ifBlank { Str.untitled.of(lang) },
                    snippet(markdownToPlainText(n.note.content), q)
                ) { onNoteClick(n.note.id) }
            }
    }
    val waypointHits = remember(q, allWaypoints) {
        if (q.isEmpty()) emptyList() else allWaypoints
            .filter {
                it.waypoint.name.contains(q, true) || it.waypoint.address.contains(q, true) ||
                    it.waypoint.notes.contains(q, true)
            }
            .map { w -> SearchHit(w.waypoint.id, w.waypoint.name, w.waypoint.address) { onWaypointClick(w.waypoint.id) } }
    }
    val documentHits = remember(q, allDocuments) {
        if (q.isEmpty()) emptyList() else allDocuments
            .filter { it.title.contains(q, true) || it.category.contains(q, true) }
            .map { d -> SearchHit(d.id, d.title, d.category) { onDocumentClick(d.id) } }
    }
    val total = eventHits.size + noteHits.size + waypointHits.size + documentHits.size

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Str.back.text())
                    }
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(Str.searchEverywhere.text()) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = Str.clear.text())
                        }
                    }
                }
            }

            when {
                q.isEmpty() -> EmptyState(Icons.Default.Search, Str.searchHint.text())
                total == 0 -> EmptyState(Icons.Default.Search, "${Str.noResults.text()} – „$q“")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp)
                ) {
                    section(Str.events.of(lang), Icons.Default.CalendarMonth, eventHits, q) {
                        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    section(Str.notes.of(lang), Icons.AutoMirrored.Filled.Notes, noteHits, q) {
                        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    section(Str.waypoints.of(lang), Icons.Default.Place, waypointHits, q) {
                        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    }
                    section(Str.documents.of(lang), Icons.Default.Description, documentHits, q) {
                        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    icon: ImageVector,
    hits: List<SearchHit>,
    query: String,
    colors: @Composable () -> Pair<Color, Color>
) {
    if (hits.isEmpty()) return
    item(key = "header_$title") {
        Text(
            "$title · ${hits.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
        )
    }
    items(hits, key = { "${title}_${it.id}" }) { hit ->
        val (container, content) = colors()
        ListItem(
            headlineContent = {
                Text(highlight(hit.title, query), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            },
            supportingContent = if (hit.subtitle.isNotBlank()) {
                { Text(highlight(hit.subtitle, query), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            } else null,
            leadingContent = {
                Box(
                    Modifier.size(40.dp).background(container, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp)) }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = hit.onClick)
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun highlight(text: String, query: String): AnnotatedString {
    val color = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        var start = 0
        while (query.isNotEmpty()) {
            val index = text.indexOf(query, start, ignoreCase = true)
            if (index < 0) break
            append(text.substring(start, index))
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
        append(text.substring(start))
    }
}

/** Short excerpt around the first match so long notes still show why they matched. */
private fun snippet(text: String, query: String): String {
    val flat = text.replace('\n', ' ')
    val index = flat.indexOf(query, ignoreCase = true)
    if (index < 0 || index < 40) return flat
    return "…" + flat.substring(index - 30)
}
