package com.example.lifeorganizer.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.domain.HabitSchedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val DAY_MS = 86_400_000L
/** Suggested reminder time for a new habit: 08:00. */
private const val DEFAULT_REMINDER = 8 * 60

/** Suggested task reminder: 09:00 on the deadline if that is still ahead, otherwise the next full hour. */
internal fun defaultTaskReminder(due: Long?, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
    val onDeadline = due?.let { LocalDate.ofEpochDay(it).atTime(9, 0) }
    return onDeadline?.takeIf { it.isAfter(now) } ?: now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(initial: TaskItem, onDismiss: () -> Unit, onSave: (TaskItem) -> Unit, onDelete: (() -> Unit)?) {
    val lang = LocalAppLanguage.current
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var due by remember { mutableStateOf(initial.dueEpochDay) }
    var pickDate by remember { mutableStateOf(false) }
    var reminder by remember {
        mutableStateOf(initial.reminderAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() })
    }
    var pickReminderDate by remember { mutableStateOf(false) }
    var pickReminderTime by remember { mutableStateOf(false) }
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang))

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
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        due?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang))) }
                            ?: HabitsStr.noDeadline.text()
                    )
                }
                if (due != null) TextButton(onClick = { due = null }) { Text(HabitsStr.removeDeadline.text()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(HabitsStr.reminder.text(), Modifier.weight(1f))
                    Switch(checked = reminder != null, onCheckedChange = { on -> reminder = if (on) defaultTaskReminder(due) else null })
                }
                reminder?.let { at ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { pickReminderDate = true }, modifier = Modifier.weight(1f)) { Text(at.toLocalDate().format(dateFormat)) }
                        OutlinedButton(onClick = { pickReminderTime = true }) { Text(formatTime(at.hour * 60 + at.minute)) }
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it }, label = { Text(HabitsStr.note.text()) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (onDelete != null) com.example.lifeorganizer.core.theme.DeleteButton(
                    HabitsStr.delete.text(), onClick = onDelete, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(
                    initial.copy(
                        title = title.trim(), note = note.trim(), dueEpochDay = due,
                        reminderAt = reminder?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                    )
                )
            }) { Text(HabitsStr.save.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(HabitsStr.cancel.text()) } }
    )

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (due ?: LocalDate.now().toEpochDay()) * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        due = picked / DAY_MS
                        // A new task with a deadline gets a reminder on that morning unless one was set already
                        if (initial.id == 0L && reminder == null) reminder = defaultTaskReminder(due)
                    }
                    pickDate = false
                }) { Text(HabitsStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(HabitsStr.cancel.text()) } }
        ) { DatePicker(state) }
    }

    if (pickReminderDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (reminder ?: defaultTaskReminder(due)).toLocalDate().toEpochDay() * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { pickReminderDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val time = (reminder ?: defaultTaskReminder(due)).toLocalTime()
                        reminder = LocalDate.ofEpochDay(picked / DAY_MS).atTime(time)
                    }
                    pickReminderDate = false
                }) { Text(HabitsStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickReminderDate = false }) { Text(HabitsStr.cancel.text()) } }
        ) { DatePicker(state) }
    }

    if (pickReminderTime) {
        val start = reminder ?: defaultTaskReminder(due)
        val state = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickReminderTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { reminder = start.withHour(state.hour).withMinute(state.minute); pickReminderTime = false }) {
                    Text(HabitsStr.save.text())
                }
            },
            dismissButton = { TextButton(onClick = { pickReminderTime = false }) { Text(HabitsStr.cancel.text()) } }
        )
    }
}
