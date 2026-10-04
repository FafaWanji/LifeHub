package com.example.lifeorganizer.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lifeorganizer.calendar.data.EventWithReminders
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EventDetailDialog(
    eventWithReminders: EventWithReminders,
    lang: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onOpenNote: (() -> Unit)? = null,
    onCreateNote: (() -> Unit)? = null
) {
    val event = eventWithReminders.event
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 200.dp, max = 600.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header with color indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val eventColor = event.color?.let { Color(it) }
                            ?: MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(eventColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Time
                val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.Builder().setLanguage(lang).build())
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.Builder().setLanguage(lang).build())
                val startDateTime = Instant.ofEpochMilli(event.startTimeMillis)
                    .atZone(ZoneId.of(event.timezone))

                DetailRow(label = Translations.get(TransKey.TIME, lang)) {
                    if (event.isAllDay) {
                        Text(Translations.get(TransKey.ALL_DAY, lang))
                    } else {
                        val startStr = startDateTime.format(dateFormatter)
                        val timeStr = startDateTime.format(timeFormatter)
                        val endStr = event.endTimeMillis?.let {
                            val endTime = Instant.ofEpochMilli(it).atZone(ZoneId.of(event.timezone))
                            " — ${endTime.format(timeFormatter)}"
                        } ?: ""
                        Text("$startStr at $timeStr$endStr")
                    }
                }

                if (eventWithReminders.category != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = "Category") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(eventWithReminders.category.color)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(eventWithReminders.category.name)
                        }
                    }
                }

                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = Translations.get(TransKey.DESCRIPTION, lang)) {
                        Text(event.description)
                    }
                }

                if (event.isBirthday) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val currentYear = LocalDate.now().year
                    val age = event.birthYear?.let { currentYear - it }
                    val isToday = LocalDate.now() ==
                        Instant.ofEpochMilli(event.startTimeMillis)
                            .atZone(ZoneId.of(event.timezone)).toLocalDate()
                    DetailRow(label = Translations.get(TransKey.BIRTHDAY, lang)) {
                        Column {
                            Text(
                                text = buildString {
                                    append(Translations.get(TransKey.CAKE_EMOJI, lang))
                                    append(" ")
                                    if (isToday) {
                                        append(Translations.get(TransKey.HAPPY_BIRTHDAY, lang))
                                        append("! ")
                                    }
                                    if (age != null && age > 0) {
                                        append("${Translations.get(TransKey.AGE_TURNS, lang)} $age")
                                    }
                                },
                                color = androidx.compose.ui.graphics.Color(0xFFFF6B9D),
                                fontWeight = FontWeight.Bold
                            )
                            if (event.birthYear != null) {
                                Text(
                                    text = "${Translations.get(TransKey.BIRTH_YEAR, lang)}: ${event.birthYear}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                if (com.example.lifeorganizer.core.util.Places.hasPlace(event.targetAddress)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = Translations.get(TransKey.DESTINATION_ADDRESS, lang)) {
                        Text(event.targetAddress.orEmpty())
                    }
                }

                if (!event.recurrenceRule.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = Translations.get(TransKey.RECURRENCE, lang)) {
                        Text(event.recurrenceRule)
                    }
                }

                if (eventWithReminders.reminders.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = Translations.get(TransKey.REMINDERS, lang)) {
                        Column {
                            eventWithReminders.reminders.forEach { reminder ->
                                Text("• ${reminder.type}")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Event created from a note reminder: jump back to the note.
                    if (onOpenNote == null && onCreateNote != null) {
                        FilledTonalButton(onClick = onCreateNote, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(com.example.lifeorganizer.core.i18n.Str.createNoteForEvent.of(lang))
                        }
                    }
                    if (onOpenNote != null) {
                        FilledTonalButton(onClick = onOpenNote, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(com.example.lifeorganizer.core.i18n.Str.openNote.of(lang))
                        }
                    }
                    OutlinedButton(
                        onClick = onDuplicate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.get(TransKey.DUPLICATE, lang))
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.get(TransKey.EDIT, lang))
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.get(TransKey.DELETE, lang))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        content()
    }
}
