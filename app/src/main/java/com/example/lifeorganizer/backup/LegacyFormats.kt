package com.example.lifeorganizer.backup

import org.json.JSONArray
import org.json.JSONObject

/** Which app / format a file to import comes from. */
enum class ImportSource {
    LIFEORGANIZER_BACKUP, ICS_CALENDAR, WAYPOINTS_IRL, LIFEBASE, CHECKLIST, UNKNOWN
}

data class ImportedLabel(val name: String, val color: Int, val template: String = "")

data class ImportedNote(
    val title: String,
    val content: String,
    val colorLabel: Int? = null,
    val isPinned: Boolean = false,
    val labelNames: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

data class ImportedWaypoint(
    val name: String,
    val address: String,
    val notes: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val isPinned: Boolean = false,
    val lastUsed: Long = 0L,
    val lastEdited: Long = 0L,
    val labelNames: List<String> = emptyList()
)

data class ImportedWaypointLabel(val name: String, val colorHex: String)

/**
 * Detects and parses the export formats of the apps LifeOrganizer replaces:
 *  - PrivateCalendar2 → .ics export
 *  - Waypoints IRL   → waypoints_backup.json  { waypoints, labels, crossRefs }
 *  - LifeBase (web)  → { notes, labels } (localStorage "lb_offline_data")
 *  - Checklist (PC)  → checklist_data.json  { "<page>": { tasks, notes } }
 */
object LegacyFormats {

    fun detect(text: String): ImportSource {
        val trimmed = text.trimStart('﻿', ' ', '\n', '\r', '\t')
        if (trimmed.startsWith("BEGIN:VCALENDAR")) return ImportSource.ICS_CALENDAR
        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return ImportSource.UNKNOWN
        return when {
            json.optString("format") == BackupFormat.FORMAT_ID -> ImportSource.LIFEORGANIZER_BACKUP
            json.has("waypoints") && json.has("crossRefs") -> ImportSource.WAYPOINTS_IRL
            json.optJSONArray("notes") != null && json.optJSONArray("labels") != null -> ImportSource.LIFEBASE
            json.length() > 0 && json.keys().asSequence().all { key ->
                val page = json.opt(key)
                page is JSONArray || (page is JSONObject && page.has("tasks"))
            } -> ImportSource.CHECKLIST
            else -> ImportSource.UNKNOWN
        }
    }

    // ------------------------------------------------------------------ Waypoints IRL

    fun parseWaypointsIrl(text: String): Pair<List<ImportedWaypointLabel>, List<ImportedWaypoint>> {
        val json = JSONObject(text)
        val labelsById = json.optJSONArray("labels").objects().associate { l ->
            l.optLong("id") to ImportedWaypointLabel(l.optString("name"), l.optString("colorHex", "#CE93D8"))
        }
        val labelIdsByWaypoint = json.optJSONArray("crossRefs").objects()
            .groupBy({ it.optLong("waypointId") }, { it.optLong("labelId") })
        val waypoints = json.optJSONArray("waypoints").objects().mapNotNull { w ->
            val name = w.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ImportedWaypoint(
                name = name,
                address = w.optString("address"),
                notes = w.optString("notes"),
                lat = w.optDoubleOrNull("lat"),
                lng = w.optDoubleOrNull("lng"),
                isPinned = w.optBoolean("isPinned"),
                lastUsed = w.optLong("lastUsed"),
                lastEdited = w.optLong("lastEdited"),
                labelNames = labelIdsByWaypoint[w.optLong("id")].orEmpty().mapNotNull { labelsById[it]?.name }
            )
        }
        return labelsById.values.toList() to waypoints
    }

    // ------------------------------------------------------------------ LifeBase

    fun parseLifeBase(text: String): Pair<List<ImportedLabel>, List<ImportedNote>> {
        val json = JSONObject(text)
        val labels = json.optJSONArray("labels").objects().associate { l ->
            l.optString("id") to ImportedLabel(l.optString("name"), tailwindToColor(l.optString("color")))
        }
        val now = System.currentTimeMillis()
        val notes = json.optJSONArray("notes").objects().mapNotNull { n ->
            val title = n.optString("title")
            val content = n.optString("content")
            if (title.isBlank() && content.isBlank()) return@mapNotNull null
            val label = labels[n.optString("labelId")]
            val updated = n.optLong("updatedAt").takeIf { it > 0 } ?: now
            ImportedNote(
                title = title,
                content = content,
                // LifeBase notes look like sticky notes in their label colour – keep that.
                colorLabel = label?.color,
                isPinned = n.optBoolean("isPinned"),
                labelNames = listOfNotNull(label?.name),
                createdAt = parseIsoDate(n.optString("date")) ?: updated,
                updatedAt = updated,
                isDeleted = n.optBoolean("isDeleted")
            )
        }
        return labels.values.toList() to notes
    }

    private val tailwindColors = mapOf(
        "yellow" to 0xFFFFE082.toInt(), "amber" to 0xFFFFE082.toInt(), "orange" to 0xFFFFCC80.toInt(),
        "red" to 0xFFF48FB1.toInt(), "rose" to 0xFFF48FB1.toInt(), "pink" to 0xFFF48FB1.toInt(),
        "purple" to 0xFFCE93D8.toInt(), "violet" to 0xFFCE93D8.toInt(), "fuchsia" to 0xFFCE93D8.toInt(),
        "indigo" to 0xFF90CAF9.toInt(), "blue" to 0xFF90CAF9.toInt(), "sky" to 0xFF90CAF9.toInt(),
        "cyan" to 0xFF80CBC4.toInt(), "teal" to 0xFF80CBC4.toInt(),
        "green" to 0xFFA5D6A7.toInt(), "emerald" to 0xFFA5D6A7.toInt(), "lime" to 0xFFA5D6A7.toInt(),
        "gray" to 0xFFBCAAA4.toInt(), "slate" to 0xFFBCAAA4.toInt(), "stone" to 0xFFBCAAA4.toInt()
    )

    /** "bg-blue-200" or "#90caf9" → ARGB. */
    internal fun tailwindToColor(value: String): Int {
        if (value.startsWith("#")) return parseHexColor(value) ?: 0xFFCE93D8.toInt()
        val name = Regex("bg-([a-z]+)").find(value)?.groupValues?.get(1)
        return tailwindColors[name] ?: 0xFFCE93D8.toInt()
    }

    private fun parseIsoDate(value: String): Long? =
        runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching {
                java.time.LocalDate.parse(value.take(10)).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()

    // ------------------------------------------------------------------ Checklist (desktop)

    /** Each checklist page becomes one note with Markdown task items; counters show their progress. */
    fun parseChecklist(text: String): List<ImportedNote> {
        val json = JSONObject(text)
        val now = System.currentTimeMillis()
        return json.keys().asSequence().mapNotNull { page ->
            val value = json.opt(page)
            val tasks = when (value) {
                is JSONArray -> value
                is JSONObject -> value.optJSONArray("tasks") ?: JSONArray()
                else -> return@mapNotNull null
            }
            val extraNotes = (value as? JSONObject)?.optString("notes").orEmpty()
            val body = buildString {
                appendTasks(tasks, depth = 0)
                if (extraNotes.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(extraNotes.trim())
                }
            }.trim()
            if (body.isEmpty()) return@mapNotNull null // empty page
            ImportedNote(title = page, content = body, createdAt = now, updatedAt = now)
        }.toList()
    }

    private fun StringBuilder.appendTasks(tasks: JSONArray, depth: Int) {
        tasks.objects().forEach { task ->
            val indent = "  ".repeat(depth)
            val text = task.optString("text").ifBlank { task.optString("title") }
            when (task.optString("type")) {
                "counter" -> {
                    val current = task.optInt("current")
                    val target = task.optInt("target")
                    val done = task.optBoolean("done") || (target > 0 && current >= target)
                    append("$indent- [${if (done) "x" else " "}] $text ($current/$target)\n")
                }
                "group" -> {
                    val subtasks = task.optJSONArray("subtasks") ?: JSONArray()
                    if (text.isNotBlank()) append("$indent- [${if (task.optBoolean("done")) "x" else " "}] $text\n")
                    // Untitled groups only exist visually – keep them apart with a blank line.
                    else if (isNotEmpty() && !endsWith("\n\n")) append("\n")
                    appendTasks(subtasks, if (text.isNotBlank()) depth + 1 else depth)
                }
                else -> append("$indent- [${if (task.optBoolean("done")) "x" else " "}] $text\n")
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** "#RRGGBB" / "#AARRGGBB" → ARGB (pure Kotlin so it also works in unit tests). */
    internal fun parseHexColor(value: String): Int? {
        val hex = value.removePrefix("#")
        val parsed = hex.toLongOrNull(16) ?: return null
        return when (hex.length) {
            6 -> (0xFF000000 or parsed).toInt()
            8 -> parsed.toInt()
            else -> null
        }
    }

    internal fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    internal fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

    internal fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    internal fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key)
}
