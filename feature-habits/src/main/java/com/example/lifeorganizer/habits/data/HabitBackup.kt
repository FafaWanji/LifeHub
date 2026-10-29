package com.example.lifeorganizer.habits.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Habits part of the LifeHub backup. Import merges habits by title. Tasks and points of older backups are
 * skipped – to-dos live in the notes now.
 */
object HabitBackup {
    suspend fun export(dao: HabitDao): JSONObject {
        val checks = dao.checksSync().groupBy({ it.habitId }, { it.epochDay })
        return JSONObject()
            .put("habits", JSONArray(dao.habitsSync().map { h ->
                JSONObject().put("title", h.title).put("weekdays", h.weekdays).put("reminderMinutes", h.reminderMinutes)
                    .put("sortOrder", h.sortOrder).put("createdAt", h.createdAt)
                    .put("doneDays", JSONArray(checks[h.id].orEmpty()))
            }))
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    /** Returns the number of habits added. */
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
        return added
    }
}
