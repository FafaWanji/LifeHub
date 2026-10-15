package com.example.lifeorganizer.habits.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeorganizer.habits.data.Habit
import com.example.lifeorganizer.habits.data.HabitCheck
import com.example.lifeorganizer.habits.data.HabitDatabase
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.domain.HabitSchedule
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

    fun toggleHabit(row: HabitRow) = viewModelScope.launch {
        val check = HabitCheck(row.habit.id, today.toEpochDay())
        if (row.doneToday) dao.deleteCheck(check) else dao.insertCheck(check)
        // Done before the reminder time: today's reminder is skipped (and comes back when unticked)
        HabitReminders.scheduleHabit(app, row.habit)
    }

    fun saveHabit(habit: Habit) = viewModelScope.launch {
        val saved = if (habit.id == 0L) {
            habit.copy(id = dao.insertHabit(habit.copy(sortOrder = dao.habitsSync().size)))
        } else {
            dao.updateHabit(habit); habit
        }
        HabitReminders.scheduleHabit(app, saved)
    }

    fun deleteHabit(habit: Habit) = viewModelScope.launch {
        HabitReminders.cancelHabit(app, habit.id)
        dao.deleteHabit(habit)
    }

    fun saveTask(task: TaskItem) = viewModelScope.launch {
        val saved = if (task.id == 0L) task.copy(id = dao.insertTask(task)) else { dao.updateTask(task); task }
        HabitReminders.scheduleTask(app, saved)
    }

    fun toggleTask(task: TaskItem) = saveTask(task.copy(doneAt = if (task.doneAt == null) System.currentTimeMillis() else null))

    fun deleteTask(task: TaskItem) = viewModelScope.launch {
        HabitReminders.cancelTask(app, task.id)
        dao.deleteTask(task)
    }

    /** Undo after a delete: the task comes back with its id, so its reminder too. */
    fun restoreTask(task: TaskItem) = viewModelScope.launch {
        dao.insertTask(task)
        HabitReminders.scheduleTask(app, task)
    }
}
