package com.example.lifeorganizer.habits.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.habits.HabitsIntegration
import com.example.lifeorganizer.habits.TaskActions
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.HabitCheck
import com.example.lifeorganizer.habits.data.HabitDatabase
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.domain.HabitSchedule
import com.example.lifeorganizer.habits.domain.TaskQuickParse
import com.example.lifeorganizer.habits.reminder.HabitReminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A habit with what the screens need: done today, due today and the current streak. */
data class HabitRow(val habit: Habit, val doneToday: Boolean, val dueToday: Boolean, val streak: Int)

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val dao = HabitDatabase.getDatabase(application).habitDao()

    private val today: LocalDate get() = LocalDate.now()

    // Streaks only look back a year, so older checks are not needed here
    val habits: StateFlow<List<HabitRow>> = combine(dao.habits(), dao.checksSince(LocalDate.now().minusDays(400).toEpochDay())) { habits, checks ->
        val byHabit = checks.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() }
        val day = today
        habits.map { h ->
            val done = byHabit[h.id].orEmpty()
            HabitRow(h, day.toEpochDay() in done, HabitSchedule.isDue(h.weekdays, day), HabitSchedule.streak(h.weekdays, done, day))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskItem>> = dao.tasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All points ever earned with tasks, including deleted ones. */
    val totalPoints: StateFlow<Int> = dao.totalPoints().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleHabit(row: HabitRow) = viewModelScope.launch {
        val check = HabitCheck(row.habit.id, today.toEpochDay())
        if (row.doneToday) dao.deleteCheck(check) else dao.insertCheck(check)
        // Done before the reminder time: today's reminder is skipped (and comes back when unticked)
        HabitReminders.scheduleHabit(app, row.habit)
        HabitsIntegration.changed(app)
    }

    fun saveHabit(habit: Habit) = viewModelScope.launch {
        val saved = if (habit.id == 0L) {
            habit.copy(id = dao.insertHabit(habit.copy(sortOrder = dao.habitsSync().size)))
        } else {
            dao.updateHabit(habit); habit
        }
        HabitReminders.scheduleHabit(app, saved)
        HabitsIntegration.changed(app)
    }

    fun deleteHabit(habit: Habit) = viewModelScope.launch {
        HabitReminders.cancelHabit(app, habit.id)
        dao.deleteHabit(habit)
        HabitsIntegration.changed(app)
    }

    fun saveTask(task: TaskItem) = viewModelScope.launch {
        // Reminder times before this save were planned differently, so they are not caught up after a reboot
        val edited = task.copy(lastReminderAt = System.currentTimeMillis())
        val saved = if (edited.id == 0L) edited.copy(id = dao.insertTask(edited)) else { dao.updateTask(edited); edited }
        HabitReminders.scheduleTask(app, saved)
        HabitsIntegration.changed(app)
    }

    /** "Exercise 5 tomorrow 14:00" → a task with that deadline and the default reminders. */
    fun quickAddTask(text: String) {
        val parsed = TaskQuickParse.parse(text) ?: return
        saveTask(TaskItem(title = parsed.title, dueEpochDay = parsed.dueEpochDay, dueMinute = parsed.dueMinute))
    }

    /** Ticks off (points if in time) or back on (points taken back); see [TaskActions]. */
    fun toggleTask(task: TaskItem) = viewModelScope.launch { TaskActions.setDone(app, task.id, done = task.doneAt == null) }

    private val settings = SettingsManager(application)

    /** Ask before "I did it on time"; the very first time twice. Switched back on in the settings. */
    val confirmOnTimeClaim: StateFlow<Boolean> = settings.confirmOnTimeClaim.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val onTimeClaimUsed: StateFlow<Boolean> = settings.onTimeClaimUsed.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** "I did it on time but forgot to tick it off"; [dontAskAgain] comes from the box in the dialog. */
    fun claimOnTime(task: TaskItem, dontAskAgain: Boolean = false) = viewModelScope.launch {
        TaskActions.claimOnTime(app, task.id)
        settings.saveOnTimeClaimUsed()
        if (dontAskAgain) settings.saveConfirmOnTimeClaim(false)
    }

    /** After notifications or exact alarms were allowed in the system settings. */
    fun rescheduleAll() = viewModelScope.launch { HabitReminders.scheduleAll(app) }

    fun deleteTask(task: TaskItem) = viewModelScope.launch {
        HabitReminders.cancelTask(app, task.id)
        dao.deleteTask(task)
        HabitsIntegration.changed(app)
    }

    /** Undo after a delete: the task comes back with its id, so its reminder too. */
    fun restoreTask(task: TaskItem) = viewModelScope.launch {
        dao.insertTask(task)
        HabitReminders.scheduleTask(app, task)
        HabitsIntegration.changed(app)
    }
}
