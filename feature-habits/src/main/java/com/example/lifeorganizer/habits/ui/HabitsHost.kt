package com.example.lifeorganizer.habits.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.domain.HabitSchedule
import com.example.lifeorganizer.habits.domain.TaskGroup
import com.example.lifeorganizer.habits.domain.taskGroup
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class HabitsTab { TODAY, TASKS, HABITS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsHost(onMenuClick: () -> Unit, vm: HabitsViewModel = viewModel()) {
    val lang = LocalAppLanguage.current
    val habits by vm.habits.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(HabitsTab.TODAY) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }
    var editTask by remember { mutableStateOf<TaskItem?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Str.habitsTasks.text()) },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, Str.menu.text()) } }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = {
                    when (tab) {
                        HabitsTab.TODAY -> showAddMenu = true
                        HabitsTab.TASKS -> editTask = TaskItem(title = "")
                        HabitsTab.HABITS -> editHabit = Habit(title = "")
                    }
                }) {
                    Icon(Icons.Default.Add, if (tab == HabitsTab.HABITS) HabitsStr.newHabit.text() else HabitsStr.newTask.text())
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(text = { Text(HabitsStr.newTask.text()) }, onClick = { showAddMenu = false; editTask = TaskItem(title = "") })
                    DropdownMenuItem(text = { Text(HabitsStr.newHabit.text()) }, onClick = { showAddMenu = false; editHabit = Habit(title = "") })
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab.ordinal) {
                HabitsTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = {
                        Text(
                            when (t) {
                                HabitsTab.TODAY -> HabitsStr.today.text()
                                HabitsTab.TASKS -> HabitsStr.tasks.text()
                                HabitsTab.HABITS -> HabitsStr.habits.text()
                            }
                        )
                    })
                }
            }
            val openTasks = tasks.filter { it.doneAt == null }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                when (tab) {
                    HabitsTab.TODAY -> {
                        val dueHabits = habits.filter { it.dueToday }
                        val urgent = openTasks.filter { taskGroup(it.dueEpochDay, today).let { g -> g == TaskGroup.OVERDUE || g == TaskGroup.TODAY } }
                        if (dueHabits.isEmpty() && urgent.isEmpty()) item { EmptyHint(HabitsStr.nothingToday.text()) }
                        if (dueHabits.isNotEmpty()) {
                            header(HabitsStr.habits.of(lang))
                            items(dueHabits, key = { "h${it.habit.id}" }) { row ->
                                HabitItem(row, lang, onToggle = { vm.toggleHabit(row) }, onClick = { editHabit = row.habit })
                            }
                        }
                        taskSection(HabitsStr.overdue.of(lang), urgent.filter { taskGroup(it.dueEpochDay, today) == TaskGroup.OVERDUE }, today, lang, vm) { editTask = it }
                        taskSection(HabitsStr.dueToday.of(lang), urgent.filter { taskGroup(it.dueEpochDay, today) == TaskGroup.TODAY }, today, lang, vm) { editTask = it }
                    }
                    HabitsTab.TASKS -> {
                        if (tasks.isEmpty()) item { EmptyHint(HabitsStr.noTasks.text()) }
                        val groups = openTasks.groupBy { taskGroup(it.dueEpochDay, today) }
                        TaskGroup.entries.forEach { g ->
                            val title = when (g) {
                                TaskGroup.OVERDUE -> HabitsStr.overdue
                                TaskGroup.TODAY -> HabitsStr.dueToday
                                TaskGroup.SOON -> HabitsStr.soon
                                TaskGroup.LATER -> HabitsStr.later
                                TaskGroup.NO_DEADLINE -> HabitsStr.noDeadline
                            }.of(lang)
                            taskSection(title, groups[g].orEmpty(), today, lang, vm) { editTask = it }
                        }
                        // Recently done, newest first; older ones stay saved but out of the way
                        taskSection(
                            HabitsStr.done.of(lang),
                            tasks.filter { it.doneAt != null }.sortedByDescending { it.doneAt }.take(30), today, lang, vm
                        ) { editTask = it }
                    }
                    HabitsTab.HABITS -> {
                        if (habits.isEmpty()) item { EmptyHint(HabitsStr.noHabits.text()) }
                        items(habits, key = { "h${it.habit.id}" }) { row ->
                            HabitItem(row, lang, showSchedule = true, onToggle = { vm.toggleHabit(row) }, onClick = { editHabit = row.habit })
                        }
                    }
                }
            }
        }
    }

    editHabit?.let { h ->
        HabitDialog(
            initial = h,
            onDismiss = { editHabit = null },
            onSave = { vm.saveHabit(it); editHabit = null },
            onDelete = if (h.id == 0L) null else ({
                vm.deleteHabit(h); editHabit = null
                scope.launch { snackbar.showSnackbar(HabitsStr.habitDeleted.of(lang), duration = SnackbarDuration.Short) }
            })
        )
    }
    editTask?.let { t ->
        TaskDialog(
            initial = t,
            onDismiss = { editTask = null },
            onSave = { vm.saveTask(it); editTask = null },
            onDelete = if (t.id == 0L) null else ({
                vm.deleteTask(t); editTask = null
                scope.launch {
                    val r = snackbar.showSnackbar(HabitsStr.taskDeleted.of(lang), HabitsStr.undo.of(lang), duration = SnackbarDuration.Short)
                    if (r == SnackbarResult.ActionPerformed) vm.restoreTask(t)
                }
            })
        )
    }
}

private fun LazyListScope.header(title: String) = item(key = "header_$title") {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

private fun LazyListScope.taskSection(
    title: String, tasks: List<TaskItem>, today: LocalDate, lang: String, vm: HabitsViewModel, onClick: (TaskItem) -> Unit
) {
    if (tasks.isEmpty()) return
    header(title)
    items(tasks, key = { "t${it.id}" }) { task -> TaskItemRow(task, today, lang, onToggle = { vm.toggleTask(task) }, onClick = { onClick(task) }) }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(32.dp)
    )
}

internal fun formatTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/** "Every day" or the chosen weekday letters, e.g. "M W F". */
internal fun weekdaySummary(weekdays: Int, lang: String): String {
    if (weekdays and HabitSchedule.EVERY_DAY == HabitSchedule.EVERY_DAY) return HabitsStr.everyDay.of(lang)
    val letters = HabitsStr.weekdayLetters.of(lang)
    return java.time.DayOfWeek.values().filter { weekdays and HabitSchedule.bit(it) != 0 }.joinToString(" ") { letters[it.value - 1].toString() }
}

@Composable
private fun HabitItem(row: HabitRow, lang: String, showSchedule: Boolean = false, onToggle: () -> Unit, onClick: () -> Unit) {
    val details = buildList {
        if (showSchedule) add(weekdaySummary(row.habit.weekdays, lang))
        row.habit.reminderMinutes?.let { add(formatTime(it)) }
        if (showSchedule && !row.dueToday) add(HabitsStr.notToday.of(lang))
    }.joinToString(" · ")
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Checkbox(checked = row.doneToday, onCheckedChange = { onToggle() }, enabled = row.dueToday || row.doneToday) },
        headlineContent = {
            Text(row.habit.title, textDecoration = if (row.doneToday) TextDecoration.LineThrough else null)
        },
        supportingContent = if (details.isEmpty()) null else ({
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (row.habit.reminderMinutes != null) Icon(Icons.Outlined.Notifications, null, Modifier.size(16.dp))
                Text(details)
            }
        }),
        trailingContent = if (row.streak > 0) ({
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalFireDepartment, HabitsStr.streakDays.of(lang).format(row.streak), tint = MaterialTheme.colorScheme.tertiary)
                Text(row.streak.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }) else null
    )
}

@Composable
private fun TaskItemRow(task: TaskItem, today: LocalDate, lang: String, onToggle: () -> Unit, onClick: () -> Unit) {
    val done = task.doneAt != null
    val overdue = !done && task.dueEpochDay != null && task.dueEpochDay < today.toEpochDay()
    val due = task.dueEpochDay?.let {
        LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang)))
    }
    val details = listOfNotNull(due, task.note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }).joinToString(" · ")
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Checkbox(checked = done, onCheckedChange = { onToggle() }) },
        headlineContent = { Text(task.title, textDecoration = if (done) TextDecoration.LineThrough else null) },
        supportingContent = if (details.isEmpty()) null else ({
            Text(details, color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        })
    )
}
