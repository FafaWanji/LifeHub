package com.example.lifeorganizer.habits.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import com.example.lifeorganizer.core.theme.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
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
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.domain.HabitSchedule
import com.example.lifeorganizer.habits.reminder.HabitReminders
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsHost(onMenuClick: () -> Unit, vm: HabitsViewModel = viewModel()) {
    val lang = LocalAppLanguage.current
    val habits by vm.habits.collectAsStateWithLifecycle()
    var quickText by rememberSaveable { mutableStateOf("") }
    var editHabit by remember { mutableStateOf<Habit?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Str.habitsTasks.text()) },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, Str.menu.text()) } },
                actions = { com.example.lifeorganizer.core.theme.SettingsAction() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editHabit = Habit(title = "") }) { Icon(Icons.Default.Add, HabitsStr.newHabit.text()) }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item(key = "permissions") { ReminderPermissionBanner(lang, onAllowed = { vm.rescheduleAll() }) }
            item(key = "quickAdd") {
                QuickAddHabit(quickText, { quickText = it }, HabitsStr.quickAddHabit.of(lang), lang) { vm.quickAdd(quickText); quickText = "" }
            }
            if (habits.isEmpty()) item { EmptyHint(HabitsStr.noHabits.text()) }
            // Due today first, the rest below
            items(habits.sortedBy { !it.dueToday }, key = { "h${it.habit.id}" }) { row ->
                HabitItem(row, lang, showSchedule = true, onToggle = { vm.toggleHabit(row) }, onClick = { editHabit = row.habit })
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
}

/** Type a habit in one line, e.g. "Gym Mon Wed Fri 7:00". */
@Composable
private fun QuickAddHabit(text: String, onText: (String) -> Unit, placeholder: String, lang: String, onAdd: () -> Unit) {
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
