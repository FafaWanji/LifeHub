package com.example.lifeorganizer.habits.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.domain.HabitSchedule
import com.example.lifeorganizer.habits.domain.TaskDeadline
import java.time.DayOfWeek
import java.time.LocalDate

private const val DAY_MS = 86_400_000L
/** Suggested reminder time for a new habit: 08:00. */
private const val DEFAULT_REMINDER = 8 * 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDialog(initial: Habit, onDismiss: () -> Unit, onSave: (Habit) -> Unit, onDelete: (() -> Unit)?) {
    val lang = LocalAppLanguage.current
    var title by remember { mutableStateOf(initial.title) }
    var weekdays by remember { mutableIntStateOf(initial.weekdays) }
    var reminder by remember { mutableStateOf(initial.reminderMinutes) }
    var pickTime by remember { mutableStateOf(false) }
    val letters = HabitsStr.weekdayLetters.of(lang)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) HabitsStr.newHabit.text() else HabitsStr.editHabit.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text(HabitsStr.title.text()) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(HabitsStr.weekdays.text())
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.values().forEach { day ->
                        val bit = HabitSchedule.bit(day)
                        FilterChip(
                            selected = weekdays and bit != 0,
                            onClick = { weekdays = weekdays xor bit },
                            label = { Text(letters[day.value - 1].toString(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(HabitsStr.reminder.text(), Modifier.weight(1f))
                    Switch(checked = reminder != null, onCheckedChange = { on -> reminder = if (on) DEFAULT_REMINDER else null })
                }
                reminder?.let { minutes ->
                    OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) { Text(formatTime(minutes)) }
                }
                if (onDelete != null) com.example.lifeorganizer.core.theme.DeleteButton(
                    HabitsStr.delete.text(), onClick = onDelete, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && weekdays != 0, onClick = {
                onSave(initial.copy(title = title.trim(), weekdays = weekdays, reminderMinutes = reminder))
            }) { Text(HabitsStr.save.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(HabitsStr.cancel.text()) } }
    )

    if (pickTime) {
        val start = reminder ?: DEFAULT_REMINDER
        val state = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { reminder = state.hour * 60 + state.minute; pickTime = false }) { Text(HabitsStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(HabitsStr.cancel.text()) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDialog(
    initial: TaskItem, onDismiss: () -> Unit, onSave: (TaskItem) -> Unit, onDelete: (() -> Unit)?,
    /** Shown for late tasks: "I did it on time – forgot to tick it off". */
    onClaimOnTime: (() -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    val today = LocalDate.now()
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var due by remember { mutableStateOf(initial.dueEpochDay) }
    var dueMinute by remember { mutableIntStateOf(initial.dueMinute) }
    var offsets by remember { mutableStateOf(TaskDeadline.parseOffsets(initial.reminderOffsets).toSet()) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var ownOffset by remember { mutableStateOf(false) }
    val dateFormat = com.example.lifeorganizer.core.i18n.localDateFormatter(lang, "yyyyMMMdEEE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) HabitsStr.newTask.text() else HabitsStr.editTask.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text(HabitsStr.title.text()) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(HabitsStr.deadline.text())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HabitsStr.today to 0L, HabitsStr.tomorrow to 1L, HabitsStr.nextWeek to 7L).forEach { (label, plus) ->
                        val day = today.plusDays(plus).toEpochDay()
                        FilterChip(selected = due == day, onClick = { due = day }, label = { Text(label.text()) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                        Text(due?.let { LocalDate.ofEpochDay(it).format(dateFormat) } ?: HabitsStr.noDeadline.text(), maxLines = 1)
                    }
                    OutlinedButton(onClick = { pickTime = true }) { Text(formatTime(dueMinute)) }
                }
                if (due == null) Text(
                    HabitsStr.pickDeadline.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
                )
                Text(HabitsStr.remindBefore.text())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Presets plus any own values the task already has
                    (TaskDeadline.PRESET_OFFSETS + offsets).distinct().sorted().forEach { minutes ->
                        FilterChip(
                            selected = minutes in offsets,
                            onClick = { offsets = if (minutes in offsets) offsets - minutes else offsets + minutes },
                            label = { Text(offsetLabel(minutes, lang)) }
                        )
                    }
                    AssistChip(onClick = { ownOffset = true }, label = { Text("+ " + HabitsStr.ownReminder.text()) })
                }
                Text(
                    HabitsStr.alwaysAtDeadline.text(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it }, label = { Text(HabitsStr.note.text()) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (onClaimOnTime != null) OutlinedButton(onClick = onClaimOnTime, modifier = Modifier.fillMaxWidth()) {
                    Text(HabitsStr.claimOnTime.text(), textAlign = TextAlign.Center)
                }
                if (onDelete != null) com.example.lifeorganizer.core.theme.DeleteButton(
                    HabitsStr.delete.text(), onClick = onDelete, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && due != null, onClick = {
                onSave(
                    initial.copy(
                        title = title.trim(), note = note.trim(), dueEpochDay = due, dueMinute = dueMinute,
                        reminderOffsets = TaskDeadline.formatOffsets(offsets),
                        // The fixed reminder of old tasks is replaced by the reminders before the deadline
                        reminderAt = null
                    )
                )
            }) { Text(HabitsStr.save.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(HabitsStr.cancel.text()) } }
    )

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (due ?: today.toEpochDay()) * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked -> due = picked / DAY_MS }
                    pickDate = false
                }) { Text(HabitsStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(HabitsStr.cancel.text()) } }
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = dueMinute / 60, initialMinute = dueMinute % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text(HabitsStr.time.text()) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { dueMinute = state.hour * 60 + state.minute; pickTime = false }) { Text(HabitsStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(HabitsStr.cancel.text()) } }
        )
    }

    if (ownOffset) OwnOffsetDialog(onDismiss = { ownOffset = false }, onAdd = { offsets = offsets + it; ownOffset = false })
}

/**
 * Confirms "I did it on time": the very first time twice ("Are you sure?", "Are you really sure?"), after that once
 * with a "Don't ask again" box. With [ask] off (box ticked earlier) it goes through without a question.
 */
@Composable
fun ClaimOnTimeDialogs(
    task: TaskItem, points: Int, ask: Boolean, firstTime: Boolean, onConfirm: (dontAskAgain: Boolean) -> Unit, onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    if (!ask && !firstTime) {
        LaunchedEffect(task.id) { onConfirm(false) }
        return
    }
    var step by remember { mutableIntStateOf(1) }
    var dontAskAgain by remember { mutableStateOf(false) }
    val deadline = TaskDeadline.deadline(task.dueEpochDay, task.dueMinute)
    val claim = HabitsStr.claimText.of(lang).format(
        deadline?.let { deadlineLabel(it, LocalDate.now(), lang) }.orEmpty(), points
    )
    val reallySure = firstTime && step == 2
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reallySure) HabitsStr.areYouReallySure.text() else HabitsStr.areYouSure.text()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall)
                Text(if (reallySure) HabitsStr.claimReallyText.text() else claim)
                // The box only comes once the double question of the first time is behind you
                if (!firstTime) Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { dontAskAgain = !dontAskAgain }
                ) {
                    Checkbox(checked = dontAskAgain, onCheckedChange = { dontAskAgain = it })
                    Text(HabitsStr.dontAskAgain.text(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (firstTime && step == 1) step = 2 else onConfirm(dontAskAgain) }) { Text(HabitsStr.yes.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(HabitsStr.cancel.text()) } }
    )
}

/** Own reminder like "90 minutes" or "4 days" before the deadline. */
@Composable
private fun OwnOffsetDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableIntStateOf(60) }
    // Up to a year before is plenty
    val minutes = amount.trim().toIntOrNull()?.takeIf { it in 1..525_600 }?.times(unit)?.takeIf { it in 1..525_600 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(HabitsStr.ownReminderTitle.text()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { v -> amount = v.filter { it.isDigit() }.take(6) },
                    label = { Text(HabitsStr.amount.text()) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HabitsStr.unitMinutes to 1, HabitsStr.unitHours to 60, HabitsStr.unitDays to 1440).forEach { (label, factor) ->
                        FilterChip(selected = unit == factor, onClick = { unit = factor }, label = { Text(label.text()) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = minutes != null, onClick = { minutes?.let(onAdd) }) { Text(HabitsStr.add.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(HabitsStr.cancel.text()) } }
    )
}
