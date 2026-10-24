package com.example.lifeorganizer.habits.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import com.example.lifeorganizer.core.theme.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.habits.TaskActions
import com.example.lifeorganizer.habits.TaskSettings
import com.example.lifeorganizer.habits.rememberTaskSettings
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.domain.HabitSchedule
import com.example.lifeorganizer.habits.domain.TaskDeadline
import com.example.lifeorganizer.habits.domain.TaskGroup
import com.example.lifeorganizer.habits.domain.taskGroup
import com.example.lifeorganizer.habits.reminder.HabitReminders
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class HabitsTab { TODAY, TASKS, HABITS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsHost(
    onMenuClick: () -> Unit,
    /** Opens this task right away, e.g. tapped as a deadline in the calendar. */
    openTaskId: Long? = null,
    onTaskOpened: () -> Unit = {},
    vm: HabitsViewModel = viewModel()
) {
    val lang = LocalAppLanguage.current
    val habits by vm.habits.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val totalPoints by vm.totalPoints.collectAsStateWithLifecycle()
    val earnedPoints by vm.pointsByTask.collectAsStateWithLifecycle()
    var quickText by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(HabitsTab.TODAY) }
    var editHabit by remember { mutableStateOf<Habit?>(null) }
    var editTask by remember { mutableStateOf<TaskItem?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // A line that became a habit outside the habits tab says so, since it lands elsewhere
    val quickAdd = { habitOnly: Boolean ->
        val habit = vm.quickAdd(quickText, habitOnly)
        quickText = ""
        if (habit != null && !habitOnly) scope.launch { snackbar.showSnackbar(HabitsStr.habitAdded.of(lang).format(habit), duration = SnackbarDuration.Short) }
    }
    val confirmOnTimeClaim by vm.confirmOnTimeClaim.collectAsStateWithLifecycle()
    val onTimeClaimUsed by vm.onTimeClaimUsed.collectAsStateWithLifecycle()
    var claimTask by remember { mutableStateOf<TaskItem?>(null) }
    var confirmDelete by remember { mutableStateOf<TaskItem?>(null) }
    val ts = rememberTaskSettings()
    // Ticked off after the deadline: fewer or no points, unless it was only the ticking that came late
    val toggleTask: (TaskItem) -> Unit = { task ->
        vm.toggleTask(task)
        if (task.doneAt == null && TaskActions.canClaimOnTime(task, ts)) scope.launch {
            val got = TaskDeadline.points(TaskDeadline.deadline(task.dueEpochDay, task.dueMinute), LocalDateTime.now())
            val r = snackbar.showSnackbar(HabitsStr.doneLate.of(lang).format(got), HabitsStr.wasOnTime.of(lang), duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) claimTask = task
        }
    }
    // New tasks start with the deadline time and reminders from the settings
    fun newTask() = TaskItem(title = "", dueMinute = ts.defaultMinute, reminderOffsets = TaskDeadline.formatOffsets(TaskDeadline.parseOffsets(ts.defaultOffsets)))
    fun deleteTask(t: TaskItem) {
        vm.deleteTask(t); editTask = null
        scope.launch {
            val r = snackbar.showSnackbar(HabitsStr.taskDeleted.of(lang), HabitsStr.undo.of(lang), duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) vm.restoreTask(t)
        }
    }
    LaunchedEffect(openTaskId, tasks) {
        val task = openTaskId?.let { id -> tasks.firstOrNull { it.id == id } } ?: return@LaunchedEffect
        tab = HabitsTab.TASKS
        editTask = task
        onTaskOpened()
    }
    // Ticks every minute, so a task moves to "Overdue" right when its deadline has passed
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            now = LocalDateTime.now()
        }
    }

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
                        HabitsTab.TASKS -> editTask = newTask()
                        HabitsTab.HABITS -> editHabit = Habit(title = "")
                    }
                }) {
                    Icon(Icons.Default.Add, if (tab == HabitsTab.HABITS) HabitsStr.newHabit.text() else HabitsStr.newTask.text())
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(text = { Text(HabitsStr.newTask.text()) }, onClick = { showAddMenu = false; editTask = newTask() })
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
                        val urgent = openTasks.filter { taskGroup(it.dueEpochDay, it.dueMinute, now, ts.soonDays).let { g -> g == TaskGroup.OVERDUE || g == TaskGroup.TODAY } }
                        item(key = "quickAdd") { QuickAddTask(quickText, { quickText = it }, HabitsStr.quickAddAny.of(lang), lang) { quickAdd(false) } }
                        if (dueHabits.isEmpty() && urgent.isEmpty()) item { EmptyHint(HabitsStr.nothingToday.text()) }
                        if (dueHabits.isNotEmpty()) {
                            header(HabitsStr.habits.of(lang))
                            items(dueHabits, key = { "h${it.habit.id}" }) { row ->
                                HabitItem(row, lang, onToggle = { vm.toggleHabit(row) }, onClick = { editHabit = row.habit })
                            }
                        }
                        taskSection(HabitsStr.overdue.of(lang), urgent.filter { taskGroup(it.dueEpochDay, it.dueMinute, now, ts.soonDays) == TaskGroup.OVERDUE }, now, lang, ts, earnedPoints, toggleTask) { editTask = it }
                        taskSection(HabitsStr.dueToday.of(lang), urgent.filter { taskGroup(it.dueEpochDay, it.dueMinute, now, ts.soonDays) == TaskGroup.TODAY }, now, lang, ts, earnedPoints, toggleTask) { editTask = it }
                    }
                    HabitsTab.TASKS -> {
                        if (ts.showPoints) item(key = "points") { PointsHeader(totalPoints, ts.points, lang) }
                        item(key = "permissions") { ReminderPermissionBanner(lang, onAllowed = { vm.rescheduleAll() }) }
                        item(key = "quickAdd") {
                            QuickAddTask(quickText, { quickText = it }, HabitsStr.quickAdd.of(lang), lang) { quickAdd(false) }
                        }
                        if (tasks.isEmpty()) item { EmptyHint(HabitsStr.noTasks.text()) }
                        val groups = openTasks.groupBy { taskGroup(it.dueEpochDay, it.dueMinute, now, ts.soonDays) }
                        TaskGroup.entries.forEach { g ->
                            val title = when (g) {
                                TaskGroup.OVERDUE -> HabitsStr.overdue
                                TaskGroup.TODAY -> HabitsStr.dueToday
                                TaskGroup.SOON -> null
                                TaskGroup.LATER -> HabitsStr.later
                                TaskGroup.NO_DEADLINE -> HabitsStr.noDeadline
                            }?.of(lang) ?: HabitsStr.soon.of(lang).format(ts.soonDays)
                            taskSection(title, groups[g].orEmpty(), now, lang, ts, earnedPoints, toggleTask) { editTask = it }
                        }
                        // Recently done, newest first; older ones stay saved but out of the way
                        taskSection(
                            HabitsStr.done.of(lang),
                            tasks.filter { it.doneAt != null }.sortedByDescending { it.doneAt }.let { done -> if (ts.doneShown > 0) done.take(ts.doneShown) else done }, now, lang, ts, earnedPoints, toggleTask
                        ) { editTask = it }
                    }
                    HabitsTab.HABITS -> {
                        item(key = "quickAdd") { QuickAddTask(quickText, { quickText = it }, HabitsStr.quickAddHabit.of(lang), lang) { quickAdd(true) } }
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
            onClaimOnTime = if (t.id != 0L && TaskActions.canClaimOnTime(t, ts)) ({ editTask = null; claimTask = t }) else null,
            onDelete = if (t.id == 0L) null else ({ if (ts.confirmDelete) confirmDelete = t else deleteTask(t) })
        )
    }
    confirmDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(HabitsStr.deleteTaskTitle.text()) },
            text = { Text(t.title + if (ts.showPoints) "\n\n" + HabitsStr.deleteTaskBody.text() else "") },
            confirmButton = { TextButton(onClick = { confirmDelete = null; deleteTask(t) }) { Text(HabitsStr.delete.text()) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(HabitsStr.cancel.text()) } }
        )
    }
    claimTask?.let { t ->
        ClaimOnTimeDialogs(
            task = t, points = ts.points, ask = confirmOnTimeClaim, firstTime = !onTimeClaimUsed,
            onConfirm = { dontAskAgain -> vm.claimOnTime(t, dontAskAgain); claimTask = null },
            onDismiss = { claimTask = null }
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
    title: String, tasks: List<TaskItem>, now: LocalDateTime, lang: String, ts: TaskSettings, points: Map<Long, Int>,
    onToggle: (TaskItem) -> Unit, onClick: (TaskItem) -> Unit
) {
    if (tasks.isEmpty()) return
    header(title)
    items(tasks, key = { "t${it.id}" }) { task -> TaskItemRow(task, now, lang, ts, points[task.id], onToggle = { onToggle(task) }, onClick = { onClick(task) }) }
}

/** Total at the top of the tasks tab. */
@Composable
private fun PointsHeader(total: Int, perTask: Int, lang: String) {
    ListItem(
        leadingContent = { Icon(Icons.Outlined.EmojiEvents, null, tint = MaterialTheme.colorScheme.tertiary) },
        headlineContent = { Text(HabitsStr.points.of(lang).format(total), style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(HabitsStr.pointsHint.of(lang).format(perTask)) }
    )
}

/** Type a task with its deadline ("Exercise 5 tomorrow 14:00") or a habit ("Gym Mon Wed Fri") in one line. */
@Composable
private fun QuickAddTask(text: String, onText: (String) -> Unit, placeholder: String, lang: String, onAdd: () -> Unit) {
    val add = { if (text.isNotBlank()) onAdd() }
    OutlinedTextField(
        value = text, onValueChange = onText, singleLine = true,
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = {
            IconButton(onClick = add, enabled = text.isNotBlank()) { Icon(Icons.Default.Add, HabitsStr.add.of(lang)) }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { add() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/**
 * Shown when reminders could not come (on time): notifications off or exact alarms not allowed.
 * Checked again whenever the screen comes back, e.g. from the system settings.
 */
@Composable
private fun ReminderPermissionBanner(lang: String, onAllowed: () -> Unit) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(HabitReminders.notificationsAllowed(context)) }
    var exact by remember { mutableStateOf(HabitReminders.exactAlarmsAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        val n = HabitReminders.notificationsAllowed(context)
        val e = HabitReminders.exactAlarmsAllowed(context)
        if ((n && !notifications) || (e && !exact)) onAllowed()
        notifications = n
        exact = e
        onPauseOrDispose { }
    }
    val problems = buildList<Pair<String, () -> Unit>> {
        if (!notifications) add(HabitsStr.notificationsOff.of(lang) to {
            context.startSettings(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                }
            )
        })
        if (!exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(HabitsStr.exactAlarmsOff.of(lang) to {
            context.startSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        })
    }
    if (problems.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        problems.forEach { (text, open) ->
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsOff, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                TextButton(onClick = open) { Text(HabitsStr.openSettings.of(lang)) }
            }
        }
    }
}

/** Falls back to the app's settings page where a special settings screen does not exist. */
private fun Context.startSettings(intent: Intent) {
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK
    runCatching { startActivity(intent.addFlags(flags)) }.onFailure {
        runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).addFlags(flags)) }
    }
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
                Icon(Icons.Outlined.LocalFireDepartment, if (row.streak == 1) HabitsStr.streakOneDay.of(lang) else HabitsStr.streakDays.of(lang).format(row.streak), tint = MaterialTheme.colorScheme.tertiary)
                Text(row.streak.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }) else null
    )
}

@Composable
private fun TaskItemRow(task: TaskItem, now: LocalDateTime, lang: String, ts: TaskSettings, earned: Int?, onToggle: () -> Unit, onClick: () -> Unit) {
    val done = task.doneAt != null
    val deadline = TaskDeadline.deadline(task.dueEpochDay, task.dueMinute)
    val missed = !done && TaskDeadline.isMissed(deadline, now)
    val due = deadline?.let { deadlineLabel(it, now.toLocalDate(), lang) }
    // What was actually booked: tasks done before points existed show nothing instead of points they never got
    val booked = if (done) earned else null
    val onTime = booked != null && booked > 0
    // Done: "+1" or "0 – too late"; open past the deadline: "Deadline missed"
    val status = when {
        ts.showPoints && onTime -> HabitsStr.pointsEarned.of(lang).format(booked)
        ts.showPoints && booked == 0 -> HabitsStr.tooLate.of(lang)
        missed -> HabitsStr.deadlineMissed.of(lang)
        else -> null
    }
    // Next push of an open task, e.g. "Tomorrow 13:00"; the one at the deadline is not repeated here
    val remind = if (done) null else {
        deadline?.let { d ->
            TaskDeadline.nextReminder(d, TaskDeadline.parseOffsets(task.reminderOffsets), now, quiet = ts.quiet)?.takeIf { it.offsetMinutes > 0 }?.at
        }
            ?: task.reminderAt?.let { TaskDeadline.toLocal(it) }?.takeIf { deadline == null && it.isAfter(now) }
    }?.let { deadlineLabel(it, now.toLocalDate(), lang) }
    val details = listOfNotNull(due, task.note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }).joinToString(" · ")
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Checkbox(checked = done, onCheckedChange = { onToggle() }) },
        headlineContent = { Text(task.title, textDecoration = if (done) TextDecoration.LineThrough else null) },
        supportingContent = if (details.isEmpty() && remind == null) null else ({
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (details.isNotEmpty()) Text(
                    details, color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, modifier = Modifier.weight(1f, fill = false)
                )
                if (remind != null) {
                    Icon(Icons.Outlined.Notifications, null, Modifier.size(16.dp))
                    Text(remind, maxLines = 1)
                }
            }
        }),
        trailingContent = if (status == null) null else ({
            Text(
                status, style = MaterialTheme.typography.labelLarge,
                color = when {
                    onTime -> MaterialTheme.colorScheme.tertiary
                    missed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        })
    )
}
