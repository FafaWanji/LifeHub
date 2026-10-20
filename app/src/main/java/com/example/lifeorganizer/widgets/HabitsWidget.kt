package com.example.lifeorganizer.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.lifeorganizer.MainActivity
import com.example.lifeorganizer.R
import com.example.lifeorganizer.core.settings.AppModule
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.core.settings.enabledModules
import com.example.lifeorganizer.habits.TaskActions
import com.example.lifeorganizer.habits.data.HabitCheck
import com.example.lifeorganizer.habits.data.HabitDatabase
import com.example.lifeorganizer.habits.domain.HabitSchedule
import com.example.lifeorganizer.habits.domain.TaskDeadline
import com.example.lifeorganizer.habits.reminder.HabitReminders
import com.example.lifeorganizer.habits.ui.HabitsStr
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the widget lists; tapping the chip in its header cycles through these. */
enum class WidgetFilter { ALL, TASKS, HABITS }

/** One line of the widget: a habit due today or an open task. */
private data class WidgetRow(
    val kind: String, val id: Long, val title: String, val done: Boolean,
    val trailing: String?, val overdue: Boolean
)

/**
 * The next habits and tasks at a glance. Rows can be ticked off right on the home screen; the chip in the
 * header switches between all, tasks only and habits only (remembered per widget).
 */
class HabitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = HabitDatabase.getDatabase(context).habitDao()
        val settings = SettingsManager(context)
        provideContent {
            // Flows keep the widget current while it is shown; the update period moves "today" past midnight.
            val today = remember { LocalDate.now() }
            val habits by remember { dao.habits() }.collectAsState(emptyList())
            val checks by remember { dao.checksSince(today.toEpochDay()) }.collectAsState(emptyList())
            val tasks by remember { dao.tasks() }.collectAsState(emptyList())
            val lang by remember { settings.languageCode }.collectAsState("en")
            val modules by remember { settings.enabledModules }.collectAsState(AppModule.entries.toSet())
            val filter = currentState(FILTER)?.let { runCatching { WidgetFilter.valueOf(it) }.getOrNull() } ?: WidgetFilter.ALL

            val doneToday = checks.filter { it.epochDay == today.toEpochDay() }.map { it.habitId }.toSet()
            val habitRows = habits.filter { HabitSchedule.isDue(it.weekdays, today) }
                .sortedWith(compareBy({ it.id in doneToday }, { it.reminderMinutes ?: Int.MAX_VALUE }))
                .map { h ->
                    WidgetRow("habit", h.id, h.title, h.id in doneToday, h.reminderMinutes?.let { "%02d:%02d".format(it / 60, it % 60) }, false)
                }
            // Overdue by the deadline time, not only by the day
            val now = LocalDateTime.now()
            val taskRows = tasks.filter { it.doneAt == null }.map { t ->
                val deadline = TaskDeadline.deadline(t.dueEpochDay, t.dueMinute)
                WidgetRow("task", t.id, t.title, false, deadline?.let { dueLabel(it, today, lang) }, TaskDeadline.isMissed(deadline, now))
            }
            val rows = when (filter) {
                WidgetFilter.HABITS -> habitRows
                WidgetFilter.TASKS -> taskRows
                // Open things first: habits still to do, then tasks by deadline, then habits already done today
                WidgetFilter.ALL -> habitRows.filter { !it.done } + taskRows + habitRows.filter { it.done }
            }.take(MAX_ROWS)

            Content(context, lang, filter, rows, AppModule.HABITS in modules)
        }
    }

    @Composable
    private fun Content(context: Context, lang: String, filter: WidgetFilter, rows: List<WidgetRow>, moduleOn: Boolean) {
        val surface = ColorProvider(R.color.widget_surface)
        val accent = ColorProvider(R.color.widget_accent)
        val muted = ColorProvider(R.color.widget_muted)
        val onSurface = ColorProvider(R.color.widget_on_surface)
        val open = actionStartActivity(openApp(context))

        Column(modifier = GlanceModifier.fillMaxSize().background(surface).cornerRadius(20.dp).padding(14.dp)) {
            Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    HabitsStr.today.of(lang),
                    style = TextStyle(color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight().clickable(open)
                )
                if (moduleOn) {
                    Text(
                        when (filter) {
                            WidgetFilter.ALL -> HabitsStr.all
                            WidgetFilter.TASKS -> HabitsStr.tasks
                            WidgetFilter.HABITS -> HabitsStr.habits
                        }.of(lang) + " ▾",
                        style = TextStyle(color = onSurface, fontSize = 12.sp),
                        modifier = GlanceModifier.background(ColorProvider(R.color.widget_chip)).cornerRadius(12.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .clickable(actionRunCallback<CycleWidgetFilter>())
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        "+",
                        style = TextStyle(color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.padding(horizontal = 4.dp).clickable(open)
                    )
                }
            }
            when {
                !moduleOn -> Text(HabitsStr.moduleOff.of(lang), style = TextStyle(color = muted, fontSize = 13.sp))
                rows.isEmpty() -> Text(
                    HabitsStr.allDone.of(lang), style = TextStyle(color = muted, fontSize = 14.sp),
                    modifier = GlanceModifier.clickable(open)
                )
                else -> LazyColumn {
                    items(rows, itemId = { (if (it.kind == "habit") 1L shl 40 else 0L) + it.id }) { row ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp)
                                .clickable(actionRunCallback<ToggleWidgetRow>(actionParametersOf(KIND to row.kind, ID to row.id))),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (row.done) "✓" else "○",
                                style = TextStyle(color = if (row.done) muted else accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(GlanceModifier.width(10.dp))
                            Text(
                                row.title,
                                maxLines = 1,
                                style = TextStyle(
                                    color = if (row.done) muted else onSurface, fontSize = 14.sp,
                                    textDecoration = if (row.done) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            row.trailing?.let {
                                Text(
                                    it,
                                    style = TextStyle(color = if (row.overdue) ColorProvider(R.color.widget_error) else muted, fontSize = 12.sp),
                                    modifier = GlanceModifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openApp(context: Context) = Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_HABITS
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    companion object {
        private const val MAX_ROWS = 12
        val FILTER = stringPreferencesKey("filter")
        val KIND = ActionParameters.Key<String>("kind")
        val ID = ActionParameters.Key<Long>("id")

        /** "Today", "Tomorrow" or a short date like "18. Okt.", with the time unless it is the end of the day. */
        private fun dueLabel(deadline: LocalDateTime, today: LocalDate, lang: String): String {
            val day = when (deadline.toLocalDate().toEpochDay() - today.toEpochDay()) {
                0L -> HabitsStr.today.of(lang)
                1L -> HabitsStr.tomorrow.of(lang)
                else -> deadline.toLocalDate().format(
                    DateTimeFormatter.ofPattern(if (lang == "en") "d MMM" else "d. MMM", Locale.forLanguageTag(lang))
                )
            }
            val minute = deadline.hour * 60 + deadline.minute
            return if (minute == TaskDeadline.END_OF_DAY) day else "$day %02d:%02d".format(deadline.hour, deadline.minute)
        }

        suspend fun refresh(context: Context) = runCatching { HabitsWidget().updateAll(context) }
    }
}

/** Ticks a habit off for today (or back on) or marks a task done (points as in the app), then re-plans its reminder. */
class ToggleWidgetRow : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitsWidget.ID] ?: return
        val dao = HabitDatabase.getDatabase(context).habitDao()
        val today = LocalDate.now().toEpochDay()
        when (parameters[HabitsWidget.KIND]) {
            "habit" -> {
                val habit = dao.habitById(id) ?: return
                val check = HabitCheck(id, today)
                if (dao.checksSync().any { it.habitId == id && it.epochDay == today }) dao.deleteCheck(check) else dao.insertCheck(check)
                HabitReminders.scheduleHabit(context, habit)
            }
            "task" -> TaskActions.setDone(context, id, done = true)
        }
        HabitsWidget.refresh(context)
    }
}

class CycleWidgetFilter : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[HabitsWidget.FILTER]?.let { runCatching { WidgetFilter.valueOf(it) }.getOrNull() } ?: WidgetFilter.ALL
            prefs[HabitsWidget.FILTER] = WidgetFilter.entries[(current.ordinal + 1) % WidgetFilter.entries.size].name
        }
        HabitsWidget().update(context, glanceId)
    }
}

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}
