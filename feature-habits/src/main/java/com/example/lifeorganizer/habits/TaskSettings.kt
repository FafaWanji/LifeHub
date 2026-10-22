package com.example.lifeorganizer.habits

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.core.settings.rememberMicro
import com.example.lifeorganizer.habits.domain.TaskDeadline

/** The habits & tasks part of the settings, read together. */
data class TaskSettings(
    val defaultOffsets: String,
    val defaultMinute: Int,
    /** Fixed: [TaskDeadline.POINTS_ON_TIME]. */
    val points: Int,
    val showPoints: Boolean,
    /** 0 = all. */
    val doneShown: Int,
    val quiet: TaskDeadline.QuietHours?,
    val soonDays: Int,
    val repeatAfter: Int,
    val snoozeMinutes: Int,
    val confirmDelete: Boolean
) {
    companion object {
        fun read(context: Context): TaskSettings {
            fun <T> get(key: MicroSettings.Key<T>) = MicroSettings.get(context, key)
            return TaskSettings(
                defaultOffsets = get(MicroSettings.TASK_DEFAULT_OFFSETS),
                defaultMinute = get(MicroSettings.TASK_DEFAULT_MINUTE).coerceIn(0, TaskDeadline.END_OF_DAY),
                points = TaskDeadline.POINTS_ON_TIME,
                showPoints = get(MicroSettings.SHOW_POINTS),
                doneShown = get(MicroSettings.DONE_TASKS_SHOWN).coerceAtLeast(0),
                quiet = if (get(MicroSettings.QUIET_HOURS)) TaskDeadline.QuietHours(get(MicroSettings.QUIET_START), get(MicroSettings.QUIET_END)) else null,
                soonDays = get(MicroSettings.SOON_DAYS).coerceAtLeast(1),
                repeatAfter = get(MicroSettings.DEADLINE_REPEAT).coerceAtLeast(0),
                snoozeMinutes = get(MicroSettings.SNOOZE_MINUTES).coerceAtLeast(1),
                confirmDelete = get(MicroSettings.CONFIRM_DELETE_TASK)
            )
        }
    }
}

/** [TaskSettings] as Compose state; follows changes in the settings screen. */
@Composable
fun rememberTaskSettings(): TaskSettings {
    val quietOn = rememberMicro(MicroSettings.QUIET_HOURS)
    val quietStart = rememberMicro(MicroSettings.QUIET_START)
    val quietEnd = rememberMicro(MicroSettings.QUIET_END)
    return TaskSettings(
        defaultOffsets = rememberMicro(MicroSettings.TASK_DEFAULT_OFFSETS),
        defaultMinute = rememberMicro(MicroSettings.TASK_DEFAULT_MINUTE).coerceIn(0, TaskDeadline.END_OF_DAY),
        points = TaskDeadline.POINTS_ON_TIME,
        showPoints = rememberMicro(MicroSettings.SHOW_POINTS),
        doneShown = rememberMicro(MicroSettings.DONE_TASKS_SHOWN).coerceAtLeast(0),
        quiet = if (quietOn) TaskDeadline.QuietHours(quietStart, quietEnd) else null,
        soonDays = rememberMicro(MicroSettings.SOON_DAYS).coerceAtLeast(1),
        repeatAfter = rememberMicro(MicroSettings.DEADLINE_REPEAT).coerceAtLeast(0),
        snoozeMinutes = rememberMicro(MicroSettings.SNOOZE_MINUTES).coerceAtLeast(1),
        confirmDelete = rememberMicro(MicroSettings.CONFIRM_DELETE_TASK)
    )
}
