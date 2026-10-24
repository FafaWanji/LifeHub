package com.example.lifeorganizer.core.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Small per-module settings (the sub-sections of the settings screen). Kept in SharedPreferences so
 * alarm receivers, widgets and dialogs can read them at once, without waiting for DataStore.
 */
object MicroSettings {
    private const val PREFS = "micro_settings"

    sealed class Key<out T>(val name: String, val default: T)
    class IntKey(name: String, default: Int) : Key<Int>(name, default)
    class BoolKey(name: String, default: Boolean) : Key<Boolean>(name, default)
    class StringKey(name: String, default: String) : Key<String>(name, default)
    class SetKey(name: String, default: Set<String>) : Key<Set<String>>(name, default)

    // Habits & tasks
    /** Reminders of a new task, minutes before the deadline. */
    val TASK_DEFAULT_OFFSETS = StringKey("task_default_offsets", "1440,60")
    /** Deadline time of a new task when none is given, minutes after midnight. */
    val TASK_DEFAULT_MINUTE = IntKey("task_default_minute", 23 * 60 + 59)
    val SHOW_POINTS = BoolKey("show_points", true)
    /** Done tasks listed in the tasks tab, 0 = all. */
    val DONE_TASKS_SHOWN = IntKey("done_tasks_shown", 30)
    val QUIET_HOURS = BoolKey("quiet_hours", false)
    val QUIET_START = IntKey("quiet_start", 22 * 60)
    val QUIET_END = IntKey("quiet_end", 7 * 60)
    /** What "Next … days" covers in the tasks tab. */
    val SOON_DAYS = IntKey("soon_days", 7)
    val WIDGET_DONE_HABITS = BoolKey("widget_done_habits", true)
    val CONFIRM_DELETE_TASK = BoolKey("confirm_delete_task", false)

    // Notifications
    /** One more push this many minutes after a missed deadline, 0 = off. */
    val DEADLINE_REPEAT = IntKey("deadline_repeat", 0)
    /** "Snooze" in reminders of all modules. */
    val SNOOZE_MINUTES = IntKey("snooze_minutes", 10)

    // Calendar
    /** "system", "MONDAY", "SUNDAY" or "SATURDAY". */
    val WEEK_START = StringKey("week_start", "system")
    val EVENT_LENGTH = IntKey("event_length", 60)
    /**
     * Default reminder in minutes before, for events without a reminder of their own (not all-day ones or birthdays;
     * of several events at one place on a day only the first). -1 = off.
     */
    val EVENT_REMINDER = IntKey("event_reminder", 120)
    val WEEK_NUMBERS = BoolKey("week_numbers", false)
    val WIDGET_BIRTHDAYS = BoolKey("widget_birthdays", true)

    // Notes
    val NOTE_CHECKLIST = BoolKey("note_checklist", false)
    val NOTE_DONE_BOTTOM = BoolKey("note_done_bottom", true)
    /** Glow around dialog windows: "off", "theme" (design colours) or "custom" ([GLOW_COLOR]). */
    val GLOW_MODE = StringKey("glow_mode", "off")
    /** Where the glow shines: "window" (around dialogs), "edge" (screen edge) or "both". */
    val GLOW_PLACE = StringKey("glow_place", "edge")
    /** Release radar: rank titles in tiers S … F. */
    val RELEASE_TIERS = BoolKey("release_tiers", false)
    /** Release radar order: "next", "oldest" or "name". */
    val RELEASE_SORT = StringKey("release_sort", "next")
    /** Colour of the release dots in the calendar (ARGB); 0 = the design's colour. */
    val RELEASE_COLOR = IntKey("release_color", 0)
    /** Colour of the custom glow (ARGB); 0 = rainbow. */
    val GLOW_COLOR = IntKey("glow_color", 0)
    /** Editor text size: 0.85, 1.0 or 1.2 (stored as percent). */
    val NOTE_FONT = IntKey("note_font", 100)
    /** Notes in the trash are deleted after this many days, 0 = never. */
    val TRASH_DAYS = IntKey("trash_days", 0)

    // Finance
    val CURRENCY = StringKey("currency", "EUR")
    /** The finance month starts on this day (e.g. 25 = payday). */
    val MONTH_START = IntKey("month_start", 1)
    /** Budget bar turns yellow from this percentage. */
    val BUDGET_WARN = IntKey("budget_warn", 80)

    // Appearance
    val COMPACT = BoolKey("compact", false)
    /** ActiveView name the app opens with. */
    val START_SCREEN = StringKey("start_screen", "CALENDAR")
    /** "language", "dmy", "mdy" or "ymd". */
    val DATE_ORDER = StringKey("date_order", "language")

    // Privacy & backup
    /** App lock covers "app" (everything) or "modules" (only [LOCKED_VIEWS]). */
    val LOCK_SCOPE = StringKey("lock_scope", "app")
    val LOCKED_VIEWS = SetKey("locked_views", setOf("MONEY"))
    val BACKUP_KEEP = IntKey("backup_keep", 5)
    val BACKUP_WIFI_ONLY = BoolKey("backup_wifi_only", false)

    /** Set by the app: re-plans the auto backup after its settings changed. */
    @Volatile
    var onBackupChanged: ((Context) -> Unit)? = null

    /** First day of the week from [WEEK_START]; "system" follows the phone's region. */
    fun firstDayOfWeek(setting: String): java.time.DayOfWeek =
        runCatching { java.time.DayOfWeek.valueOf(setting) }.getOrNull()
            ?: java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek

    /** Week number of [date] for that first weekday (a week belongs to the year that has 4 of its days). */
    fun weekNumber(date: java.time.LocalDate, firstDay: java.time.DayOfWeek): Int =
        date.get(java.time.temporal.WeekFields.of(firstDay, 4).weekOfWeekBasedYear())

    fun prefs(context: Context): SharedPreferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Suppress("UNCHECKED_CAST")
    fun <T> get(context: Context, key: Key<T>): T {
        val p = prefs(context)
        return when (key) {
            is IntKey -> p.getInt(key.name, key.default)
            is BoolKey -> p.getBoolean(key.name, key.default)
            is StringKey -> p.getString(key.name, key.default) ?: key.default
            is SetKey -> p.getStringSet(key.name, key.default)?.toSet() ?: key.default
        } as T
    }

    fun <T> set(context: Context, key: Key<T>, value: T) {
        prefs(context).edit().apply {
            when (key) {
                is IntKey -> putInt(key.name, value as Int)
                is BoolKey -> putBoolean(key.name, value as Boolean)
                is StringKey -> putString(key.name, value as String)
                is SetKey -> putStringSet(key.name, (value as Set<*>).filterIsInstance<String>().toSet())
            }
        }.apply()
    }

    /** Every key, for the backup. */
    val all: List<Key<*>> = listOf(
        TASK_DEFAULT_OFFSETS, TASK_DEFAULT_MINUTE, SHOW_POINTS, DONE_TASKS_SHOWN,
        QUIET_HOURS, QUIET_START, QUIET_END, SOON_DAYS, WIDGET_DONE_HABITS, CONFIRM_DELETE_TASK,
        DEADLINE_REPEAT, SNOOZE_MINUTES,
        WEEK_START, EVENT_LENGTH, EVENT_REMINDER, WEEK_NUMBERS, WIDGET_BIRTHDAYS,
        NOTE_CHECKLIST, NOTE_DONE_BOTTOM, GLOW_MODE, GLOW_PLACE, GLOW_COLOR, NOTE_FONT, TRASH_DAYS,
        CURRENCY, MONTH_START, BUDGET_WARN,
        COMPACT, START_SCREEN, DATE_ORDER,
        LOCK_SCOPE, LOCKED_VIEWS, BACKUP_KEEP, BACKUP_WIFI_ONLY, RELEASE_TIERS, RELEASE_SORT, RELEASE_COLOR
    )

    /** Only values that differ from the default, as name → value (Int, Boolean, String or a list of strings). */
    fun export(context: Context): Map<String, Any> = all.mapNotNull { key ->
        val value = get(context, key)
        if (value == key.default) null else key.name to (if (value is Set<*>) value.toList() else value as Any)
    }.toMap()

    /** Takes over known keys of a matching type; unknown or broken ones are skipped. */
    fun import(context: Context, values: Map<String, Any?>) {
        all.forEach { key ->
            val v = values[key.name] ?: return@forEach
            when (key) {
                is IntKey -> (v as? Number)?.let { set(context, key, it.toInt()) }
                is BoolKey -> (v as? Boolean)?.let { set(context, key, it) }
                is StringKey -> (v as? String)?.let { set(context, key, it) }
                is SetKey -> (v as? Collection<*>)?.let { c -> set(context, key, c.mapNotNull { it as? String }.toSet()) }
            }
        }
        onBackupChanged?.invoke(context.applicationContext)
    }
}

/** A setting as Compose state that follows changes made anywhere in the app. */
@Composable
fun <T> rememberMicro(key: MicroSettings.Key<T>): T {
    val context = LocalContext.current.applicationContext
    var value by remember(key) { mutableStateOf(MicroSettings.get(context, key)) }
    DisposableEffect(key) {
        val prefs = MicroSettings.prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key.name) value = MicroSettings.get(context, key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}
