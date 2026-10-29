package com.example.lifeorganizer.notes.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.durationLabel
import com.example.lifeorganizer.core.i18n.localDateFormatter
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.theme.AlertDialog
import java.time.LocalDate

private const val DAY_MS = 86_400_000L

private fun formatTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/**
 * Appointment of a "To Do X" note: day, time and the reminders before it (one at the appointment itself
 * always comes on top). [onRemove] is offered when the note already has one.
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
    val today = LocalDate.now()
    var due by remember { mutableStateOf(day) }
    var dueMinute by remember { mutableIntStateOf(minute) }
    var chosen by remember { mutableStateOf(TaskDeadline.parseOffsets(offsets).toSet()) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var ownOffset by remember { mutableStateOf(false) }
    val dateFormat = localDateFormatter(lang, "yyyyMMMdEEE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(TodoStr.appointment.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TodoStr.today to 0L, TodoStr.tomorrow to 1L, TodoStr.nextWeek to 7L).forEach { (label, plus) ->
                        val d = today.plusDays(plus).toEpochDay()
                        FilterChip(selected = due == d, onClick = { due = d }, label = { Text(label.text()) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                        Text(due?.let { LocalDate.ofEpochDay(it).format(dateFormat) } ?: TodoStr.setAppointment.text(), maxLines = 1)
                    }
                    OutlinedButton(onClick = { pickTime = true }) { Text(formatTime(dueMinute)) }
                }
                Text(TodoStr.remindBefore.text())
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
        },
        confirmButton = {
            TextButton(enabled = due != null, onClick = { due?.let { onSave(it, dueMinute, TaskDeadline.formatOffsets(chosen)) } }) {
                Text(TodoStr.save.text())
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(TodoStr.cancel.text()) } }
    )

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (due ?: today.toEpochDay()) * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked -> due = picked / DAY_MS }
                    pickDate = false
                }) { Text(TodoStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(TodoStr.cancel.text()) } }
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = dueMinute / 60, initialMinute = dueMinute % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text(TodoStr.time.text()) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { dueMinute = state.hour * 60 + state.minute; pickTime = false }) { Text(TodoStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(TodoStr.cancel.text()) } }
        )
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
