package com.example.lifeorganizer.notes.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.durationLabel
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.theme.AlertDialog
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

private fun formatTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/** Time offered when "all day" is switched off: 12:00. */
private const val DEFAULT_TIME = 12 * 60

/**
 * Appointment of a "To Do X" note: the day straight in a calendar, the time as in calendar events (all day or a
 * time from the clock) and the reminders before it (one at the appointment itself always comes on top).
 * [onRemove] is offered when the note already has one.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppointmentDialog(
    day: Long?, minute: Int, offsets: String,
    onSave: (day: Long, minute: Int, offsets: String) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    // A new appointment starts today, so it can be saved right away; the picker works in UTC days
    val dateState = rememberDatePickerState(initialSelectedDateMillis = (day ?: LocalDate.now().toEpochDay()) * DAY_MS)
    var allDay by remember { mutableStateOf(minute == TaskDeadline.END_OF_DAY) }
    val start = if (minute == TaskDeadline.END_OF_DAY) DEFAULT_TIME else minute
    val timeState = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = true)
    var chosen by remember { mutableStateOf(TaskDeadline.parseOffsets(offsets).toSet()) }
    var pickTime by remember { mutableStateOf(false) }
    var ownOffset by remember { mutableStateOf(false) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = dateState.selectedDateMillis != null,
                onClick = {
                    val picked = dateState.selectedDateMillis ?: return@TextButton
                    val m = if (allDay) TaskDeadline.END_OF_DAY else timeState.hour * 60 + timeState.minute
                    onSave(picked / DAY_MS, m, TaskDeadline.formatOffsets(chosen))
                }
            ) { Text(TodoStr.save.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(TodoStr.cancel.text()) } }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            DatePicker(
                state = dateState, showModeToggle = false,
                title = { Text(TodoStr.appointment.text(), Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }
            )
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Time like in calendar events: all day, or a time from the clock
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                    Text(TodoStr.allDay.text())
                }
                if (!allDay) OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${TodoStr.time.text()}: ${formatTime(timeState.hour * 60 + timeState.minute)}")
                }
                Text(TodoStr.remindBefore.text(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Presets plus any own values the note already has
                    (TaskDeadline.PRESET_OFFSETS + chosen).distinct().sorted().forEach { m ->
                        FilterChip(
                            selected = m in chosen,
                            onClick = { chosen = if (m in chosen) chosen - m else chosen + m },
                            label = { Text(durationLabel(m, lang)) }
                        )
                    }
                    AssistChip(onClick = { ownOffset = true }, label = { Text("+ " + TodoStr.ownReminder.text()) })
                }
                Text(
                    TodoStr.alwaysAtAppointment.text(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onRemove != null) TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Text(TodoStr.remove.text(), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (pickTime) {
        Dialog(onDismissRequest = { pickTime = false }) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    TimePicker(state = timeState)
                    TextButton(onClick = { pickTime = false }, modifier = Modifier.align(Alignment.End)) { Text(TodoStr.done.text()) }
                }
            }
        }
    }

    if (ownOffset) OwnOffsetDialog(onDismiss = { ownOffset = false }, onAdd = { chosen = chosen + it; ownOffset = false })
}

/** Own reminder like "90 minutes" or "4 days" before the appointment. */
@Composable
private fun OwnOffsetDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableIntStateOf(60) }
    // Up to a year before is plenty
    val minutes = amount.trim().toIntOrNull()?.takeIf { it in 1..525_600 }?.times(unit)?.takeIf { it in 1..525_600 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(TodoStr.ownReminderTitle.text()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { v -> amount = v.filter { it.isDigit() }.take(6) },
                    label = { Text(TodoStr.amount.text()) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TodoStr.unitMinutes to 1, TodoStr.unitHours to 60, TodoStr.unitDays to 1440).forEach { (label, factor) ->
                        FilterChip(selected = unit == factor, onClick = { unit = factor }, label = { Text(label.text()) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = minutes != null, onClick = { minutes?.let(onAdd) }) { Text(TodoStr.add.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(TodoStr.cancel.text()) } }
    )
}
