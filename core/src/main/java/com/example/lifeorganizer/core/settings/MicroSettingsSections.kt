package com.example.lifeorganizer.core.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ChevronRight
import com.example.lifeorganizer.core.theme.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.SetStr
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.durationLabel
import com.example.lifeorganizer.core.i18n.localDateFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The sub-sections of the settings screen, one per module, each folded until tapped. Every change is
 * saved at once ([MicroSettings]) and picked up by the module the next time it reads it.
 */
@Composable
fun MicroSettingsSections(lang: String) {
    val modules = rememberEnabledModules()
    // One section open at a time keeps the long settings screen short
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val toggle: (String) -> Unit = { id -> open = if (open == id) null else id }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (AppModule.HABITS in modules) Section(Str.habitsTasks.of(lang), open == "habits", { toggle("habits") }) { HabitsSection(lang) }
        Section(SetStr.notifications.of(lang), open == "notifications", { toggle("notifications") }) { NotificationsSection(lang, AppModule.HABITS in modules) }
        Section(Str.calendar.of(lang), open == "calendar", { toggle("calendar") }) { CalendarSection(lang) }
        Section(Str.notes.of(lang), open == "notes", { toggle("notes") }) { NotesSection(lang) }
        if (AppModule.FINANCE in modules) Section(Str.finance.of(lang), open == "finance", { toggle("finance") }) { FinanceSection(lang) }
        Section(SetStr.appearance.of(lang), open == "appearance", { toggle("appearance") }) { AppearanceSection(lang, modules) }
        Section(SetStr.privacyBackup.of(lang), open == "privacy", { toggle("privacy") }) { PrivacySection(lang, modules) }
    }
}

@Composable
private fun HabitsSection(lang: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsManager(context.applicationContext) }
    val confirmOnTime by settings.confirmOnTimeClaim.collectAsState(initial = true)
    ListItem(
        headlineContent = { Text(Str.confirmOnTimeClaim.of(lang)) },
        supportingContent = { Text(Str.confirmOnTimeClaimHint.of(lang)) },
        trailingContent = { Switch(checked = confirmOnTime, onCheckedChange = { on -> scope.launch { settings.saveConfirmOnTimeClaim(on) } }) },
        colors = transparent()
    )
    TimeSetting(SetStr.defaultDeadlineTime.of(lang), MicroSettings.TASK_DEFAULT_MINUTE, lang, SetStr.defaultDeadlineTimeHint.of(lang))
    OffsetsSetting(SetStr.defaultReminders.of(lang), lang)
    val showPoints = rememberMicro(MicroSettings.SHOW_POINTS)
    SwitchSetting(SetStr.showPoints.of(lang), MicroSettings.SHOW_POINTS)
    NumberSetting(SetStr.doneShown.of(lang), MicroSettings.DONE_TASKS_SHOWN, listOf(10 to "10", 30 to "30", 100 to "100", 0 to SetStr.all.of(lang)), lang, NumberUnit.COUNT, 1..999)
    val quiet = rememberMicro(MicroSettings.QUIET_HOURS)
    SwitchSetting(SetStr.quietHours.of(lang), MicroSettings.QUIET_HOURS, SetStr.quietHoursHint.of(lang))
    if (quiet) {
        TimeSetting(SetStr.from.of(lang), MicroSettings.QUIET_START, lang)
        TimeSetting(SetStr.to.of(lang), MicroSettings.QUIET_END, lang)
    }
    NumberSetting(SetStr.soonDays.of(lang), MicroSettings.SOON_DAYS, listOf(3, 7, 14).map { it to SetStr.days.of(lang).format(it) }, lang, NumberUnit.DAYS, 1..365)
    SwitchSetting(SetStr.widgetDoneHabits.of(lang), MicroSettings.WIDGET_DONE_HABITS)
    SwitchSetting(SetStr.confirmDeleteTask.of(lang), MicroSettings.CONFIRM_DELETE_TASK)
}

@Composable
private fun NotificationsSection(lang: String, habitsOn: Boolean) {
    val context = LocalContext.current
    Label(SetStr.soundVibration.of(lang), SetStr.soundVibrationHint.of(lang))
    buildList {
        add(NotificationChannels.CALENDAR to SetStr.channelCalendar)
        add(NotificationChannels.BIRTHDAYS to SetStr.channelBirthdays)
        if (habitsOn) {
            add(NotificationChannels.HABITS to Str.channelHabits)
            add(NotificationChannels.TASKS to Str.channelTasks)
        }
    }.forEach { (channel, label) ->
        ListItem(
            headlineContent = { Text(label.of(lang)) },
            trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
            modifier = Modifier.clickable {
                // Habits and tasks create their channel on first use; make sure it is there to be set up
                if (channel == NotificationChannels.HABITS || channel == NotificationChannels.TASKS) {
                    NotificationChannels.ensureHabitsAndTasks(context, lang)
                }
                runCatching { context.startActivity(NotificationChannels.settingsIntent(context, channel).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            },
            colors = transparent()
        )
    }
    NumberSetting(SetStr.snoozeLength.of(lang), MicroSettings.SNOOZE_MINUTES, listOf(5, 10, 15, 30, 60).map { it to durationLabel(it, lang) }, lang, NumberUnit.MINUTES, 1..1440)
    if (habitsOn) NumberSetting(
        SetStr.deadlineRepeat.of(lang), MicroSettings.DEADLINE_REPEAT,
        listOf(0 to SetStr.off.of(lang)) + listOf(15, 30, 60).map { it to durationLabel(it, lang) }, lang, NumberUnit.MINUTES, 1..10080
    )
}

@Composable
private fun CalendarSection(lang: String) {
    val locale = com.example.lifeorganizer.core.i18n.appLocale(lang)
    fun day(d: java.time.DayOfWeek) = d.getDisplayName(java.time.format.TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }
    ChoiceSetting(
        SetStr.weekStart.of(lang), MicroSettings.WEEK_START,
        listOf("system" to SetStr.system.of(lang)) +
            listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.SATURDAY).map { it.name to day(it) }
    )
    NumberSetting(SetStr.eventLength.of(lang), MicroSettings.EVENT_LENGTH, listOf(30, 60, 90, 120).map { it to durationLabel(it, lang) }, lang, NumberUnit.MINUTES, 5..10080)
    NumberSetting(
        SetStr.eventReminder.of(lang), MicroSettings.EVENT_REMINDER,
        listOf(-1 to SetStr.off.of(lang)) + listOf(30, 60, 120, 180, 1440).map { it to durationLabel(it, lang) },
        lang, NumberUnit.MINUTES, 1..40320, SetStr.eventReminderHint.of(lang)
    )
    SwitchSetting(SetStr.weekNumbers.of(lang), MicroSettings.WEEK_NUMBERS)
    SwitchSetting(SetStr.widgetBirthdays.of(lang), MicroSettings.WIDGET_BIRTHDAYS)
}

@Composable
private fun NotesSection(lang: String) {
    SwitchSetting(SetStr.noteChecklist.of(lang), MicroSettings.NOTE_CHECKLIST)
    SwitchSetting(SetStr.noteDoneBottom.of(lang), MicroSettings.NOTE_DONE_BOTTOM)
    ChoiceSetting(
        SetStr.noteFont.of(lang), MicroSettings.NOTE_FONT,
        listOf(85 to SetStr.fontSmall.of(lang), 100 to SetStr.fontNormal.of(lang), 120 to SetStr.fontLarge.of(lang))
    )
    NumberSetting(
        SetStr.trashDays.of(lang), MicroSettings.TRASH_DAYS,
        listOf(0 to SetStr.never.of(lang)) + listOf(7, 30, 90).map { it to SetStr.days.of(lang).format(it) }, lang, NumberUnit.DAYS, 1..3650
    )
}

@Composable
private fun FinanceSection(lang: String) {
    ChoiceSetting(
        SetStr.currency.of(lang), MicroSettings.CURRENCY,
        listOf("EUR" to "€ EUR", "USD" to "$ USD", "GBP" to "£ GBP", "CHF" to "CHF", "TRY" to "₺ TRY", "CNY" to "¥ CNY"),
        SetStr.currencyHint.of(lang)
    )
    val context = LocalContext.current
    val start = rememberMicro(MicroSettings.MONTH_START)
    ListItem(
        headlineContent = { Text(SetStr.monthStart.of(lang)) },
        supportingContent = { Text(SetStr.monthStartHint.of(lang)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { MicroSettings.set(context, MicroSettings.MONTH_START, (start - 1).coerceAtLeast(1)) }, enabled = start > 1) {
                    Icon(Icons.Default.Remove, null)
                }
                Text(start.toString(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { MicroSettings.set(context, MicroSettings.MONTH_START, (start + 1).coerceAtMost(28)) }, enabled = start < 28) {
                    Icon(Icons.Default.Add, null)
                }
            }
        },
        colors = transparent()
    )
    NumberSetting(SetStr.budgetWarn.of(lang), MicroSettings.BUDGET_WARN, listOf(70, 80, 90).map { it to "$it %" }, lang, NumberUnit.PERCENT, 1..100)
}

@Composable
private fun AppearanceSection(lang: String, modules: Set<AppModule>) {
    SwitchSetting(SetStr.compact.of(lang), MicroSettings.COMPACT, SetStr.compactHint.of(lang))
    ChoiceSetting(
        SetStr.rgbGlow.of(lang), MicroSettings.GLOW_MODE,
        listOf("off" to SetStr.off.of(lang), "theme" to SetStr.glowTheme.of(lang), "custom" to SetStr.glowCustom.of(lang)),
        SetStr.rgbGlowHint.of(lang)
    )
    val glowMode = rememberMicro(MicroSettings.GLOW_MODE)
    if (glowMode != "off") ChoiceRow(
        MicroSettings.GLOW_PLACE,
        listOf("window" to SetStr.glowWindow.of(lang), "edge" to SetStr.glowEdge.of(lang), "both" to SetStr.glowBoth.of(lang))
    )
    if (glowMode == "custom") GlowColorRow(lang)
    val screens = buildList {
        add("CALENDAR" to Str.calendar)
        add("NOTES" to Str.notes)
        if (AppModule.HABITS in modules) add("HABITS" to Str.habitsTasks)
        if (AppModule.FINANCE in modules) add("MONEY" to Str.finance)
        if (AppModule.DOCUMENTS in modules) add("DOCUMENTS" to Str.documents)
        if (AppModule.WAYPOINTS in modules) add("WAYPOINTS" to Str.waypoints)
    }
    ChoiceSetting(SetStr.startScreen.of(lang), MicroSettings.START_SCREEN, screens.map { (id, label) -> id to label.of(lang) }, SetStr.startScreenHint.of(lang))
    // Each choice shows today's date that way
    val today = LocalDate.now()
    fun example(order: String): String = today.format(localDateFormatter(lang, "yyyyMMdd", order))
    ChoiceSetting(
        SetStr.dateOrder.of(lang), MicroSettings.DATE_ORDER,
        listOf("language" to SetStr.dateLanguage.of(lang) + " (" + example("language") + ")") +
            listOf("dmy", "mdy", "ymd").map { it to example(it) }
    )
}

@Composable
private fun PrivacySection(lang: String, modules: Set<AppModule>) {
    val scope = rememberMicro(MicroSettings.LOCK_SCOPE)
    Label(SetStr.lockScope.of(lang), SetStr.lockHint.of(lang))
    ChoiceRow(MicroSettings.LOCK_SCOPE, listOf("app" to SetStr.lockWholeApp.of(lang), "modules" to SetStr.lockModules.of(lang)))
    if (scope == "modules") {
        val views = buildList {
            add("NOTES" to Str.notes)
            if (AppModule.FINANCE in modules) add("MONEY" to Str.finance)
            if (AppModule.HABITS in modules) add("HABITS" to Str.habitsTasks)
            if (AppModule.DOCUMENTS in modules) add("DOCUMENTS" to Str.documents)
            if (AppModule.WAYPOINTS in modules) add("WAYPOINTS" to Str.waypoints)
        }
        MultiChoiceRow(MicroSettings.LOCKED_VIEWS, views.map { (id, label) -> id to label.of(lang) })
    }
    HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    NumberSetting(SetStr.backupKeep.of(lang), MicroSettings.BACKUP_KEEP, listOf(3, 5, 10, 20).map { it to it.toString() }, lang, NumberUnit.COUNT, 1..100)
    SwitchSetting(SetStr.backupWifi.of(lang), MicroSettings.BACKUP_WIFI_ONLY, SetStr.backupWifiHint.of(lang))
}

// ---------------------------------------------------------------- building blocks

@Composable
private fun transparent() = ListItemDefaults.colors(containerColor = Color.Transparent)

/** A folded card: tapping the title shows or hides [content]. */
@Composable
private fun Section(title: String, expanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
            }
            if (expanded) content()
        }
    }
}

@Composable
private fun Label(text: String, hint: String? = null) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchSetting(label: String, key: MicroSettings.BoolKey, hint: String? = null) {
    val context = LocalContext.current
    val on = rememberMicro(key)
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = if (hint != null) ({ Text(hint) }) else null,
        trailingContent = { Switch(checked = on, onCheckedChange = { MicroSettings.set(context, key, it) }) },
        modifier = Modifier.clickable { MicroSettings.set(context, key, !on) },
        colors = transparent()
    )
}

/** A label with one chip per choice; the chosen one is marked. */
@Composable
private fun <T> ChoiceSetting(label: String, key: MicroSettings.Key<T>, options: List<Pair<T, String>>, hint: String? = null) {
    Label(label, hint)
    ChoiceRow(key, options)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(key: MicroSettings.Key<T>, options: List<Pair<T, String>>) {
    val context = LocalContext.current
    val current = rememberMicro(key)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, text) ->
            FilterChip(selected = current == value, onClick = { MicroSettings.set(context, key, value) }, label = { Text(text) })
        }
    }
}

/** How an own value is entered and shown. */
private enum class NumberUnit { COUNT, DAYS, MINUTES, PERCENT }

private fun numberLabel(value: Int, unit: NumberUnit, lang: String): String = when (unit) {
    NumberUnit.COUNT -> value.toString()
    NumberUnit.DAYS -> SetStr.days.of(lang).format(value)
    NumberUnit.MINUTES -> durationLabel(value, lang)
    NumberUnit.PERCENT -> "$value %"
}

/**
 * Like [ChoiceSetting] for numbers, plus a pencil chip for an own value in [range]. While an own value is set,
 * the pencil chip shows it and is the marked one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumberSetting(
    label: String, key: MicroSettings.IntKey, options: List<Pair<Int, String>>, lang: String,
    unit: NumberUnit, range: IntRange, hint: String? = null
) {
    val context = LocalContext.current
    val current = rememberMicro(key)
    var editing by remember { mutableStateOf(false) }
    val custom = options.none { it.first == current }
    Label(label, hint)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, text) ->
            FilterChip(selected = current == value, onClick = { MicroSettings.set(context, key, value) }, label = { Text(text) })
        }
        FilterChip(
            selected = custom,
            onClick = { editing = true },
            leadingIcon = { Icon(Icons.Outlined.Edit, SetStr.customValue.of(lang), Modifier.size(18.dp)) },
            label = { Text(if (custom) numberLabel(current, unit, lang) else SetStr.customShort.of(lang)) }
        )
    }
    if (editing) CustomNumberDialog(
        title = label, initial = if (custom && current in range) current else null, unit = unit, range = range, lang = lang,
        onDismiss = { editing = false },
        onSave = { MicroSettings.set(context, key, it); editing = false }
    )
}

/** Small window for an own number; durations can be typed in minutes, hours or days. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomNumberDialog(
    title: String, initial: Int?, unit: NumberUnit, range: IntRange, lang: String,
    onDismiss: () -> Unit, onSave: (Int) -> Unit
) {
    // Minutes are shown in the largest unit that fits evenly: 90 → 90 min, 120 → 2 h, 2880 → 2 days
    val factors = listOf(1 to SetStr.unitMinutes, 60 to SetStr.unitHours, 1440 to SetStr.unitDays)
    var factor by remember {
        mutableStateOf(if (unit == NumberUnit.MINUTES && initial != null) factors.last { initial % it.first == 0 }.first else 1)
    }
    var text by remember { mutableStateOf(initial?.let { (it / factor).toString() }.orEmpty()) }
    val value = text.toIntOrNull()?.let { it.toLong() * factor }?.takeIf { it in range.first.toLong()..range.last.toLong() }?.toInt()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = text, onValueChange = { new -> text = new.filter(Char::isDigit).take(6) },
                    singleLine = true, isError = text.isNotEmpty() && value == null,
                    suffix = when (unit) {
                        NumberUnit.DAYS -> ({ Text(SetStr.unitDays.of(lang)) })
                        NumberUnit.PERCENT -> ({ Text("%") })
                        else -> null
                    },
                    supportingText = {
                        Text(SetStr.allowedRange.of(lang).format(numberLabel(range.first, unit, lang), numberLabel(range.last, unit, lang)))
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (unit == NumberUnit.MINUTES) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    factors.forEach { (f, name) ->
                        FilterChip(selected = factor == f, onClick = { factor = f }, label = { Text(name.of(lang)) })
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = value != null, onClick = { value?.let(onSave) }) { Text(com.example.lifeorganizer.core.i18n.Str.save.of(lang)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(com.example.lifeorganizer.core.i18n.Str.cancel.of(lang)) } }
    )
}

/** Own glow colour: rainbow or one of a few strong colours, each shown as a dot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlowColorRow(lang: String) {
    val context = LocalContext.current
    val current = rememberMicro(MicroSettings.GLOW_COLOR)
    val colors = listOf(0xFFFF4D6D, 0xFFFF9F1C, 0xFF2EC4B6, 0xFF4CC9F0, 0xFF7B61FF, 0xFFE056FD).map { it.toInt() }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = current == 0, onClick = { MicroSettings.set(context, MicroSettings.GLOW_COLOR, 0) }, label = { Text(SetStr.rainbow.of(lang)) })
        colors.forEach { c ->
            FilterChip(
                selected = current == c,
                onClick = { MicroSettings.set(context, MicroSettings.GLOW_COLOR, c) },
                label = {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(18.dp).background(Color(c), androidx.compose.foundation.shape.CircleShape)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceRow(key: MicroSettings.SetKey, options: List<Pair<String, String>>) {
    val context = LocalContext.current
    val current = rememberMicro(key)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, text) ->
            FilterChip(
                selected = value in current,
                onClick = { MicroSettings.set(context, key, if (value in current) current - value else current + value) },
                label = { Text(text) }
            )
        }
    }
}

/** Reminders of new tasks: chips like in the task dialog (the one at the deadline is always added). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OffsetsSetting(label: String, lang: String) {
    val context = LocalContext.current
    val raw = rememberMicro(MicroSettings.TASK_DEFAULT_OFFSETS)
    val chosen = raw.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.toSet()
    Label(label)
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (listOf(15, 60, 180, 1440, 2880, 10080) + chosen).distinct().sorted().forEach { minutes ->
            FilterChip(
                selected = minutes in chosen,
                onClick = {
                    val next = if (minutes in chosen) chosen - minutes else chosen + minutes
                    MicroSettings.set(context, MicroSettings.TASK_DEFAULT_OFFSETS, next.sortedDescending().joinToString(","))
                },
                label = { Text(durationLabel(minutes, lang)) }
            )
        }
    }
}

/** A time of day (minutes after midnight) with a clock dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSetting(label: String, key: MicroSettings.IntKey, lang: String, hint: String? = null) {
    val context = LocalContext.current
    val minutes = rememberMicro(key)
    var pick by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = if (hint != null) ({ Text(hint) }) else null,
        trailingContent = { Text("%02d:%02d".format(minutes / 60, minutes % 60), style = MaterialTheme.typography.titleMedium) },
        modifier = Modifier.clickable { pick = true },
        colors = transparent()
    )
    if (pick) {
        val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pick = false },
            title = { Text(label) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { MicroSettings.set(context, key, state.hour * 60 + state.minute); pick = false }) { Text(SetStr.save.of(lang)) }
            },
            dismissButton = { TextButton(onClick = { pick = false }) { Text(SetStr.cancel.of(lang)) } }
        )
    }
}
