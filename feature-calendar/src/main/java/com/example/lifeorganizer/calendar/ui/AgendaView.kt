package com.example.lifeorganizer.calendar.ui

import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.i18n.Str
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import com.example.lifeorganizer.calendar.data.EventWithReminders
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ViewMode { MONTH, WEEK, AGENDA }

@Composable
fun AgendaView(
    events: List<EventWithReminders>,
    lang: String,
    onEventClick: (EventWithReminders) -> Unit,
    onEventEdit: (EventWithReminders) -> Unit,
    onEventDelete: (EventWithReminders) -> Unit,
    onEventView: (EventWithReminders) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd", Locale.Builder().setLanguage(lang).build())
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.Builder().setLanguage(lang).build())

    // Group events by month, then by day
    val groupedEvents = remember(events) {
        events
            .sortedBy { it.event.startTimeMillis }
            .groupBy {
                Instant.ofEpochMilli(it.event.startTimeMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                    .withDayOfMonth(1)
            }
            .toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room for the floating action cluster
        contentPadding = PaddingValues(bottom = 200.dp)
    ) {
        if (groupedEvents.isEmpty()) {
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

        groupedEvents.forEach { (monthDate, monthEvents) ->
            // Month header
            item {
                Text(
                    text = monthDate.format(monthFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Group by day within month
            val dayGroups = monthEvents.groupBy {
                Instant.ofEpochMilli(it.event.startTimeMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
            }.toSortedMap()

            dayGroups.forEach { (dayDate, dayEvents) ->
                // Day header
                item {
                    val isToday = dayDate == LocalDate.now()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayDate.format(dateFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = Translations.get(TransKey.TODAY, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Events at the same place on this day are listed together under the place.
                EventGrouping.byPlace(dayEvents).forEach { entry ->
                    val group = when (entry) {
                        is DayEntry.Single -> listOf(entry.event)
                        is DayEntry.PlaceGroup -> entry.events
                    }
                    if (entry is DayEntry.PlaceGroup) {
                        item(key = "place_${dayDate}_${entry.place}") {
                            Row(
                                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${entry.place} · ${entry.events.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    items(group, key = { "${it.event.id}_${it.event.startTimeMillis}" }) { item ->
                        SwipeToDeleteItem(
                            item = item,
                            lang = lang,
                            onDelete = { onEventDelete(item) },
                            onClick = { onEventView(item) },
                            onEdit = { onEventEdit(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeToDeleteItem(
    item: EventWithReminders,
    lang: String,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val dismissThreshold = 200f

    var showDeleteBackground by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Delete background (shown when swiped)
        if (showDeleteBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = Str.delete2.text(),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Foreground card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetX.value.dp)
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val eventColor = item.category?.color?.let { Color(it) }
                    ?: item.event.color?.let { Color(it) }
                    ?: MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(eventColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    val timeText = if (item.event.isAllDay) {
                        Translations.get(TransKey.ALL_DAY, lang)
                    } else {
                        Instant.ofEpochMilli(item.event.startTimeMillis)
                            .atZone(ZoneId.of(item.event.timezone))
                            .toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    }
                    val repeat = if (item.event.isBirthday) null else recurrenceLabel(item.event.recurrenceRule, lang)
                    Text(
                        text = if (repeat != null) "$timeText · 🔁 $repeat" else timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.event.isBirthday) {
                        val occurrenceYear = Instant.ofEpochMilli(item.event.startTimeMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate().year
                        val age = item.event.birthYear?.let { occurrenceYear - it }
                        Text(
                            text = "🎂 ${Translations.get(TransKey.BIRTHDAY, lang)}${age?.let { " ($it)" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF4081)
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = Str.edit2.text(), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
