package com.example.lifeorganizer.habits.data

import com.example.lifeorganizer.habits.domain.TaskDeadline
import org.json.JSONArray
import org.json.JSONObject

/**
 * Habits & tasks part of the LifeHub backup. Import merges: habits by title, tasks by title + creation time,
 * points by title + time earned (points of deleted tasks come along too).
 */
object HabitBackup {
    suspend fun export(dao: HabitDao): JSONObject {
        val checks = dao.checksSync().groupBy({ it.habitId }, { it.epochDay })
        val tasks = dao.tasksSync()
        val createdById = tasks.associate { it.id to it.createdAt }
        return JSONObject()
            .put("habits", JSONArray(dao.habitsSync().map { h ->
                JSONObject().put("title", h.title).put("weekdays", h.weekdays).put("reminderMinutes", h.reminderMinutes)
                    .put("sortOrder", h.sortOrder).put("createdAt", h.createdAt)
                    .put("doneDays", JSONArray(checks[h.id].orEmpty()))
            }))
            .put("tasks", JSONArray(tasks.map { t ->
                JSONObject().put("title", t.title).put("note", t.note).put("due", t.dueEpochDay).put("dueMinute", t.dueMinute)
                    .put("reminderOffsets", t.reminderOffsets)
                    .put("reminderAt", t.reminderAt).put("doneAt", t.doneAt).put("createdAt", t.createdAt)
            }))
            .put("points", JSONArray(dao.pointsSync().map { p ->
                JSONObject().put("title", p.title).put("points", p.points).put("earnedAt", p.earnedAt)
                    .put("taskCreatedAt", p.taskId?.let { createdById[it] })
            }))
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    /** Returns the number of habits and tasks added. */
    suspend fun import(dao: HabitDao, json: JSONObject): Int {
        var added = 0
        val habits = dao.habitsSync()
        json.optJSONArray("habits").objects().forEach { h ->
            val title = h.optString("title").takeIf { it.isNotBlank() } ?: return@forEach
            val id = habits.firstOrNull { it.title.equals(title, true) }?.id ?: dao.insertHabit(
                Habit(
                    title = title, weekdays = h.optInt("weekdays", 0b1111111),
                    reminderMinutes = h.longOrNull("reminderMinutes")?.toInt(), sortOrder = h.optInt("sortOrder"),
                    createdAt = h.optLong("createdAt", System.currentTimeMillis())
                )
            ).also { added++ }
            val days = h.optJSONArray("doneDays")
            if (days != null) for (i in 0 until days.length()) dao.insertCheck(HabitCheck(id, days.optLong(i)))
        }
        val known = dao.tasksSync().map { it.title to it.createdAt }.toHashSet()
        json.optJSONArray("tasks").objects().forEach { t ->
            val key = t.optString("title") to t.optLong("createdAt")
            if (key.first.isBlank() || key in known) return@forEach
            dao.insertTask(
                TaskItem(
                    title = key.first, note = t.optString("note"), dueEpochDay = t.longOrNull("due"),
                    dueMinute = t.optInt("dueMinute", TaskDeadline.END_OF_DAY).coerceIn(0, TaskDeadline.END_OF_DAY),
                    reminderOffsets = if (t.has("reminderOffsets")) TaskDeadline.formatOffsets(TaskDeadline.parseOffsets(t.optString("reminderOffsets")))
                    else TaskDeadline.DEFAULT_OFFSETS,
                    reminderAt = t.longOrNull("reminderAt"), doneAt = t.longOrNull("doneAt"), createdAt = key.second,
                    // Reminders that passed before the import are not caught up
                    lastReminderAt = System.currentTimeMillis()
                )
            )
            added++
        }
        val taskIds = dao.tasksSync().associate { (it.title to it.createdAt) to it.id }
        val existing = dao.pointsSync()
        val knownPoints = existing.map { it.title to it.earnedAt }.toHashSet()
        val withPoints = existing.mapNotNull { it.taskId }.toHashSet()
        json.optJSONArray("points").objects().forEach { p ->
            val key = p.optString("title") to p.optLong("earnedAt")
            if (key in knownPoints) return@forEach
            // Linked to its task if that is (now) here; one entry per task
            val taskId = p.longOrNull("taskCreatedAt")?.let { taskIds[key.first to it] }?.takeIf { it !in withPoints }
            dao.insertPoints(TaskPoints(taskId = taskId, title = key.first, points = p.optInt("points"), earnedAt = key.second))
            knownPoints += key
            taskId?.let { withPoints += it }
        }
        return added
    }
}
