package com.example.lifeorganizer.backup

import org.json.JSONArray
import org.json.JSONObject

/** Which format a file to import has. */
enum class ImportSource {
    LIFEORGANIZER_BACKUP, ICS_CALENDAR, UNKNOWN
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

/** Detects what an imported file is (LifeHub backup or .ics calendar) and offers JSON helpers. */
object LegacyFormats {

    fun detect(text: String): ImportSource {
        val trimmed = text.trimStart('﻿', ' ', '\n', '\r', '\t')
        if (trimmed.startsWith("BEGIN:VCALENDAR")) return ImportSource.ICS_CALENDAR
        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return ImportSource.UNKNOWN
        return if (json.optString("format") == BackupFormat.FORMAT_ID) ImportSource.LIFEORGANIZER_BACKUP else ImportSource.UNKNOWN
    }

    // ------------------------------------------------------------------ helpers

    internal fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    internal fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

    internal fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    internal fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key)
}
