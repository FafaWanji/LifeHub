package com.example.lifeorganizer.habits

import android.content.Context
import androidx.room.withTransaction
import com.example.lifeorganizer.habits.data.HabitDatabase
import com.example.lifeorganizer.habits.data.TaskItem
import com.example.lifeorganizer.habits.data.TaskPoints
import com.example.lifeorganizer.habits.domain.TaskDeadline
import com.example.lifeorganizer.habits.reminder.HabitReminders

/** Ticking tasks off and on, the same way from the app, the notification and the home-screen widget. */
object TaskActions {
    /**
     * Marks a task done (points by the deadline as set in the settings, 0 when late) or open again (its points are taken back).
     * Doing what is already the case changes nothing, so a double tap cannot earn twice. Returns the task as saved.
     */
    suspend fun setDone(context: Context, taskId: Long, done: Boolean, now: Long = System.currentTimeMillis()): TaskItem? {
        val app = context.applicationContext
        val db = HabitDatabase.getDatabase(app)
        val dao = db.habitDao()
        val saved = db.withTransaction {
            val task = dao.taskById(taskId) ?: return@withTransaction null
            if ((task.doneAt != null) == done) return@withTransaction task
            val updated = task.copy(doneAt = if (done) now else null)
            dao.updateTask(updated)
            if (done) {
                val deadline = TaskDeadline.deadline(task.dueEpochDay, task.dueMinute)
                val settings = TaskSettings.read(app)
                val points = TaskDeadline.points(deadline, TaskDeadline.toLocal(now), settings.points, settings.graceMinutes)
                dao.insertPoints(TaskPoints(taskId = task.id, title = task.title, points = points, earnedAt = now))
            } else {
                dao.deletePointsOf(task.id)
            }
            updated
        } ?: return null
        HabitReminders.scheduleTask(app, saved)
        HabitsIntegration.changed(app)
        return saved
    }

    /** Only late tasks need it: open past the deadline, or ticked off after it. */
    fun canClaimOnTime(task: TaskItem, settings: TaskSettings, now: Long = System.currentTimeMillis()): Boolean = settings.showPoints &&
        TaskDeadline.canClaimOnTime(
            TaskDeadline.deadline(task.dueEpochDay, task.dueMinute), task.doneAt?.let { TaskDeadline.toLocal(it) }, TaskDeadline.toLocal(now),
            settings.points, settings.graceMinutes
        )

    /**
     * "I did it on time but forgot to tick it off": done at the deadline, with the points for it.
     * Works for open tasks past their deadline and for tasks ticked off late.
     */
    suspend fun claimOnTime(context: Context, taskId: Long): TaskItem? {
        val app = context.applicationContext
        val db = HabitDatabase.getDatabase(app)
        val dao = db.habitDao()
        val saved = db.withTransaction {
            val task = dao.taskById(taskId) ?: return@withTransaction null
            val settings = TaskSettings.read(app)
            if (!canClaimOnTime(task, settings)) return@withTransaction task
            val deadline = TaskDeadline.deadline(task.dueEpochDay, task.dueMinute) ?: return@withTransaction task
            val doneAt = TaskDeadline.toMillis(deadline)
            val updated = task.copy(doneAt = doneAt)
            dao.updateTask(updated)
            // Replaces a 0 from ticking off late (one entry per task)
            dao.insertPoints(TaskPoints(taskId = task.id, title = task.title, points = settings.points, earnedAt = doneAt))
            updated
        } ?: return null
        HabitReminders.scheduleTask(app, saved)
        HabitsIntegration.changed(app)
        return saved
    }
}
