package com.example.lifeorganizer.backup

import kotlinx.coroutines.flow.first
import android.content.Context
import android.net.Uri
import com.example.lifeorganizer.backup.LegacyFormats.objects
import com.example.lifeorganizer.backup.LegacyFormats.optDoubleOrNull
import com.example.lifeorganizer.backup.LegacyFormats.optLongOrNull
import com.example.lifeorganizer.backup.LegacyFormats.optStringOrNull
import com.example.lifeorganizer.calendar.alarm.AlarmScheduler
import com.example.lifeorganizer.calendar.data.Category
import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.calendar.data.EventTemplate
import com.example.lifeorganizer.calendar.data.Reminder
import com.example.lifeorganizer.calendar.util.IcsImporter
import com.example.lifeorganizer.documents.data.local.Document
import com.example.lifeorganizer.notes.data.Note
import com.example.lifeorganizer.notes.data.NoteLabel
import com.example.lifeorganizer.notes.data.NoteLabelCrossRef
import com.example.lifeorganizer.notes.data.NoteTemplate
import com.example.lifeorganizer.waypoints.data.LabelEntity
import com.example.lifeorganizer.waypoints.data.WaypointEntity
import com.example.lifeorganizer.waypoints.data.WaypointLabelCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.lifeorganizer.calendar.data.AppDatabase as CalendarDatabase
import com.example.lifeorganizer.documents.data.local.AppDatabase as DocumentsDatabase
import com.example.lifeorganizer.notes.data.NotesDatabase
import com.example.lifeorganizer.waypoints.data.AppDatabase as WaypointsDatabase

/** Case- and whitespace-insensitive key for duplicate detection ("Berlin " == "berlin"). */
internal fun norm(value: String) = value.trim().replace(Regex("\\s+"), " ").lowercase()

object BackupFormat {
    const val FORMAT_ID = "lifeorganizer-backup"
    const val VERSION = 1
}

/** What an import added; duplicates that already existed are counted as skipped. */
data class ImportSummary(
    val source: ImportSource,
    val events: Int = 0,
    val notes: Int = 0,
    val waypoints: Int = 0,
    val documents: Int = 0,
    val transactions: Int = 0,
    val skipped: Int = 0
) {
    val total get() = events + notes + waypoints + documents + transactions
}

data class ExportSummary(val events: Int, val notes: Int, val waypoints: Int, val documents: Int, val transactions: Int = 0)

/**
 * One backup file for all modules plus importers for the apps LifeOrganizer replaced.
 * Imports always merge: nothing existing is overwritten, labels/categories are matched by name.
 */
class BackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val eventDao = CalendarDatabase.getDatabase(appContext).eventDao()
    private val noteDao = NotesDatabase.getDatabase(appContext).noteDao()
    private val waypointDao = WaypointsDatabase.getDatabase(appContext).waypointDao()
    private val documentDao = DocumentsDatabase.getDatabase(appContext).documentDao()
    private val moneyDao = com.example.lifeorganizer.money.data.MoneyDatabase.getDatabase(appContext).moneyDao()

    // ================================================================== export

    suspend fun export(uri: Uri): ExportSummary = withContext(Dispatchers.IO) {
        appContext.contentResolver.openOutputStream(uri, "wt")?.use { export(it) } ?: error("Cannot write to $uri")
    }

    suspend fun export(out: java.io.OutputStream): ExportSummary = withContext(Dispatchers.IO) {
        val events = eventDao.getEventsWithRemindersSync()
        val categories = eventDao.getCategoriesSync()
        val notes = noteDao.getAllNotesWithLabelsSync()
        val noteLabelNames = noteDao.getLabelsSync().associate { it.id to it.name }
        val waypoints = waypointDao.getAllWaypointsWithLabelsSync()
        val documents = documentDao.getAllDocumentsSync()
        val documentTitles = documents.associate { it.id to it.title }

        val root = JSONObject()
            .put("format", BackupFormat.FORMAT_ID)
            .put("version", BackupFormat.VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("calendar", JSONObject()
                .put("categories", JSONArray(categories.map { c ->
                    JSONObject().put("id", c.id).put("name", c.name).put("color", c.color).put("isDefault", c.isDefault)
                }))
                .put("events", JSONArray(events.map { e ->
                    eventToJson(e.event).put("documentTitle", documentTitles[e.event.documentId]).put("reminders", JSONArray(e.reminders.map { r ->
                        JSONObject().put("reminderTimeMillis", r.reminderTimeMillis).put("type", r.type).put("timezone", r.timezone)
                    }))
                }))
                .put("templates", JSONArray(eventDao.getEventTemplatesSync().map { t ->
                    JSONObject().put("name", t.name).put("title", t.title).put("description", t.description)
                        .put("isAllDay", t.isAllDay).put("targetAddress", t.targetAddress)
                        .put("arrivalBufferMinutes", t.arrivalBufferMinutes).put("alarmLeadMinutes", t.alarmLeadMinutes)
                        .put("categoryId", t.categoryId).put("recurrenceRule", t.recurrenceRule)
                })))
            .put("notes", JSONObject()
                .put("labels", JSONArray(noteDao.getLabelsSync().map { l -> JSONObject().put("name", l.name).put("color", l.color).put("template", l.template) }))
                .put("notes", JSONArray(notes.map { n ->
                    val note = n.note
                    JSONObject().put("id", note.id).put("title", note.title).put("content", note.content)
                        .put("createdAt", note.createdAt).put("updatedAt", note.updatedAt)
                        .put("colorLabel", note.colorLabel).put("isPinned", note.isPinned)
                        .put("isChecklist", note.isChecklist).put("pinnedToDate", note.pinnedToDate)
                        .put("isDeleted", note.isDeleted).put("deletedAt", note.deletedAt)
                        .put("labels", JSONArray(n.labels.map { it.name }))
                        .put("documentTitle", documentTitles[note.documentId])
                }))
                .put("templates", JSONArray(noteDao.getTemplatesSync().map { t ->
                    JSONObject().put("name", t.name).put("content", t.content).put("isChecklist", t.isChecklist)
                        .put("label", noteLabelNames[t.labelId])
                })))
            .put("waypoints", JSONObject()
                .put("labels", JSONArray(waypointDao.getAllLabelsSync().map { l -> JSONObject().put("name", l.name).put("colorHex", l.colorHex) }))
                .put("waypoints", JSONArray(waypoints.map { w ->
                    val wp = w.waypoint
                    JSONObject().put("name", wp.name).put("address", wp.address).put("notes", wp.notes)
                        .put("lat", wp.lat).put("lng", wp.lng).put("isPinned", wp.isPinned)
                        .put("lastUsed", wp.lastUsed).put("lastEdited", wp.lastEdited)
                        .put("labels", JSONArray(w.labels.map { it.name }))
                })))
            .put("money", com.example.lifeorganizer.money.data.MoneyBackup.export(moneyDao))
            .put("settings", exportSettings(categories.associate { it.id to it.name }))
            .put("documents", JSONObject()
                .put("documents", JSONArray(documents.map { d ->
                    JSONObject().put("title", d.title).put("uri", d.uri).put("category", d.category)
                        .put("expiryDate", d.expiryDate).put("isPinned", d.isPinned).put("isPdf", d.isPdf)
                        .put("dateAdded", d.dateAdded)
                })))

        val writer = out.bufferedWriter()
        writer.write(root.toString(2))
        writer.flush()
        ExportSummary(events.size, notes.size, waypoints.size, documents.size, moneyDao.allTransactionsSync().size)
    }

    // ================================================================== settings

    private val settings = com.example.lifeorganizer.core.settings.SettingsManager(appContext)
    private fun prefs(name: String) = appContext.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)

    /** App settings without secrets (the Groq API key is never exported). Categories are stored by name. */
    private suspend fun exportSettings(categoryNames: Map<Long, String>): JSONObject {
        val filter = prefs("calendar_filter")
        return JSONObject()
            .put("designStyle", settings.designStyle.first())
            .put("isDarkTheme", settings.isDarkTheme.first())
            .put("accentColor", settings.accentColor.first())
            .put("languageCode", settings.languageCode.first())
            .put("homeAddress", settings.homeAddress.first())
            .put("travelMode", settings.travelMode.first())
            .put("useHomeAsOrigin", settings.useHomeAsOrigin.first())
            .put("enableSmartAlarms", settings.enableSmartAlarms.first())
            .put("useSystemAlarm", settings.useSystemAlarmForReminders.first())
            .put("hideRecurring", filter.getBoolean("hide_recurring", false))
            .put("hiddenCategories", JSONArray(filter.getStringSet("hidden_categories", emptySet()).orEmpty()
                .mapNotNull { it.toLongOrNull()?.let(categoryNames::get) }))
            .put("whatsNew", prefs("whats_new").getBoolean("enabled", true))
            .put("autoBackup", prefs("auto_backup").getBoolean("enabled", true))
    }

    private suspend fun importSettings(j: JSONObject) {
        j.optStringOrNull("designStyle")?.let { settings.saveDesignStyle(it) }
        if (j.has("isDarkTheme")) settings.saveIsDarkTheme(if (j.isNull("isDarkTheme")) null else j.optBoolean("isDarkTheme"))
        if (j.has("accentColor")) settings.saveAccentColor(j.optLongOrNull("accentColor")?.toInt())
        j.optStringOrNull("languageCode")?.let { settings.saveLanguageCode(it) }
        j.optStringOrNull("homeAddress")?.takeIf { it.isNotBlank() }?.let { settings.saveHomeAddress(it) }
        j.optStringOrNull("travelMode")?.let { settings.saveTravelMode(it) }
        if (j.has("useHomeAsOrigin")) settings.saveUseHomeAsOrigin(j.optBoolean("useHomeAsOrigin"))
        if (j.has("enableSmartAlarms")) settings.saveEnableSmartAlarms(j.optBoolean("enableSmartAlarms"))
        if (j.has("useSystemAlarm")) settings.saveUseSystemAlarmForReminders(j.optBoolean("useSystemAlarm"))
        val byName = eventDao.getCategoriesSync().associate { it.name.lowercase() to it.id }
        val hidden = j.optJSONArray("hiddenCategories")?.let { a -> (0 until a.length()).mapNotNull { byName[a.optString(it).lowercase()]?.toString() } }.orEmpty()
        prefs("calendar_filter").edit().putBoolean("hide_recurring", j.optBoolean("hideRecurring")).putStringSet("hidden_categories", hidden.toSet()).apply()
        if (j.has("whatsNew")) prefs("whats_new").edit().putBoolean("enabled", j.optBoolean("whatsNew")).apply()
        if (j.has("autoBackup")) prefs("auto_backup").edit().putBoolean("enabled", j.optBoolean("autoBackup")).apply()
    }

    private fun eventToJson(e: Event) = JSONObject()
        .put("id", e.id).put("title", e.title).put("description", e.description)
        .put("startTimeMillis", e.startTimeMillis).put("endTimeMillis", e.endTimeMillis)
        .put("isAllDay", e.isAllDay).put("targetAddress", e.targetAddress)
        .put("arrivalBufferMinutes", e.arrivalBufferMinutes).put("alarmLeadMinutes", e.alarmLeadMinutes)
        .put("color", e.color).put("recurrenceRule", e.recurrenceRule).put("timezone", e.timezone)
        .put("isBirthday", e.isBirthday).put("birthYear", e.birthYear)
        .put("categoryId", e.categoryId).put("linkedNoteId", e.linkedNoteId).put("exDates", e.exDates)

    // ================================================================== import

    suspend fun import(uri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val text = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Cannot read $uri")
        importText(text)
    }

    /** Also used for pasting from the clipboard (e.g. LifeBase data copied in the browser). */
    suspend fun importText(text: String): ImportSummary = withContext(Dispatchers.IO) {
        when (val source = LegacyFormats.detect(text)) {
            ImportSource.ICS_CALENDAR -> importIcs(text)
            ImportSource.LIFEORGANIZER_BACKUP -> importBackup(JSONObject(text))
            ImportSource.UNKNOWN -> ImportSummary(source)
        }
    }

    // ---------------------------------------------------------------- calendar

    private suspend fun importIcs(text: String): ImportSummary {
        var added = 0
        var skipped = 0
        IcsImporter.parseIcs(text).forEach { parsed ->
            if (insertEvent(parsed.event.copy(id = 0), parsed.reminders)) added++ else skipped++
        }
        return ImportSummary(ImportSource.ICS_CALENDAR, events = added, skipped = skipped)
    }

    /** Inserts unless an event with the same title and start already exists; schedules future reminders. */
    private suspend fun insertEvent(event: Event, reminders: List<Reminder>): Boolean {
        val exists = existingEventKeys().contains(event.title to event.startTimeMillis)
        if (exists) return false
        val id = eventDao.insertEvent(event.copy(id = 0, targetAddress = com.example.lifeorganizer.core.util.Places.clean(event.targetAddress)))
        eventDao.insertReminders(reminders.map { it.copy(id = 0, eventId = id) })
        val saved = event.copy(id = id)
        val now = System.currentTimeMillis()
        eventDao.getRemindersForEvent(id).filter { it.reminderTimeMillis > now }.forEach {
            AlarmScheduler(appContext).schedule(saved, it)
        }
        eventKeys?.add(event.title to event.startTimeMillis)
        return true
    }

    private var eventKeys: MutableSet<Pair<String, Long>>? = null
    private suspend fun existingEventKeys(): MutableSet<Pair<String, Long>> =
        eventKeys ?: eventDao.getEventsWithRemindersSync()
            .map { it.event.title to it.event.startTimeMillis }.toMutableSet().also { eventKeys = it }

    // ---------------------------------------------------------------- notes

    /** Returns the summary plus a map of (title, createdAt) → note id, used to relink note reminders. */
    private suspend fun insertNotes(
        labels: List<ImportedLabel>,
        notes: List<ImportedNote>,
        source: ImportSource
    ): Pair<ImportSummary, Map<Pair<String, Long>, Long>> {
        val labelIds = noteDao.getLabelsSync().associate { it.name.lowercase() to it.id }.toMutableMap()
        suspend fun labelId(name: String, color: Int, template: String = ""): Long =
            labelIds[name.lowercase()] ?: noteDao.insertLabel(NoteLabel(name = name, color = color, template = template)).also { labelIds[name.lowercase()] = it }
        labels.forEach { labelId(it.name, it.color, it.template) }

        val current = noteDao.getAllNotesWithLabelsSync()
        val existing = current.associateBy({ it.note.title to it.note.createdAt }, { it.note.id })
        val existingContent = current.map { norm(it.note.title) to norm(it.note.content) }.toMutableSet()
        val ids = mutableMapOf<Pair<String, Long>, Long>()
        var added = 0
        var skipped = 0
        notes.forEach { n ->
            val key = n.title to n.createdAt
            val already = existing[key]
            if (already != null || !existingContent.add(norm(n.title) to norm(n.content))) {
                already?.let { ids[key] = it }
                skipped++
                return@forEach
            }
            val id = noteDao.insertNote(
                Note(
                    title = n.title,
                    content = n.content,
                    createdAt = n.createdAt,
                    updatedAt = n.updatedAt,
                    colorLabel = n.colorLabel,
                    isPinned = n.isPinned,
                    isChecklist = n.content.contains(Regex("(?m)^\\s*[-*+]\\s+\\[[ xX]]")),
                    isDeleted = n.isDeleted,
                    deletedAt = if (n.isDeleted) System.currentTimeMillis() else null
                )
            )
            n.labelNames.forEach { name -> noteDao.insertCrossRef(NoteLabelCrossRef(id, labelId(name, 0xFFCE93D8.toInt()))) }
            ids[key] = id
            added++
        }
        return ImportSummary(source, notes = added, skipped = skipped) to ids
    }

    // ---------------------------------------------------------------- waypoints

    private fun insertWaypoints(
        labels: List<ImportedWaypointLabel>,
        waypoints: List<ImportedWaypoint>,
        source: ImportSource
    ): ImportSummary {
        val labelIds = waypointDao.getAllLabelsSync().associate { it.name.lowercase() to it.id }.toMutableMap()
        fun labelId(name: String, colorHex: String) =
            labelIds.getOrPut(name.lowercase()) { waypointDao.insertLabel(LabelEntity(name = name, colorHex = colorHex)) }
        labels.forEach { labelId(it.name, it.colorHex) }

        val existing = waypointDao.getAllWaypointsWithLabelsSync()
            .map { norm(it.waypoint.name) to norm(it.waypoint.address) }.toMutableSet()
        var added = 0
        var skipped = 0
        waypoints.forEach { w ->
            if (!existing.add(norm(w.name) to norm(w.address))) {
                skipped++
                return@forEach
            }
            val id = waypointDao.insertWaypoint(
                WaypointEntity(
                    name = w.name, address = w.address, notes = w.notes, lat = w.lat, lng = w.lng,
                    isPinned = w.isPinned, lastUsed = w.lastUsed, lastEdited = w.lastEdited
                )
            )
            w.labelNames.forEach { waypointDao.insertWaypointLabelCrossRef(WaypointLabelCrossRef(id, labelId(it, "#CE93D8"))) }
            added++
        }
        return ImportSummary(source, waypoints = added, skipped = skipped)
    }

    // ---------------------------------------------------------------- full backup

    private suspend fun importBackup(root: JSONObject): ImportSummary {
        val notesJson = root.optJSONObject("notes")
        val noteLabels = notesJson?.optJSONArray("labels").objects().map { ImportedLabel(it.optString("name"), it.optInt("color"), it.optString("template")) }
        val backupNotes = notesJson?.optJSONArray("notes").objects()
        val (noteSummary, noteIds) = insertNotes(
            noteLabels,
            backupNotes.map { n ->
                ImportedNote(
                    title = n.optString("title"),
                    content = n.optString("content"),
                    colorLabel = n.optLongOrNull("colorLabel")?.toInt(),
                    isPinned = n.optBoolean("isPinned"),
                    labelNames = n.optJSONArray("labels")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty(),
                    createdAt = n.optLong("createdAt"),
                    updatedAt = n.optLong("updatedAt"),
                    isDeleted = n.optBoolean("isDeleted")
                )
            },
            ImportSource.LIFEORGANIZER_BACKUP
        )
        // Notes pinned to a calendar day keep that day.
        backupNotes.forEach { n ->
            val pinned = n.optLongOrNull("pinnedToDate") ?: return@forEach
            val id = noteIds[n.optString("title") to n.optLong("createdAt")] ?: return@forEach
            noteDao.getNoteWithLabels(id)?.note?.takeIf { it.pinnedToDate == null }?.let { noteDao.updateNote(it.copy(pinnedToDate = pinned)) }
        }
        val oldNoteIdToNew = backupNotes.mapNotNull { n ->
            noteIds[n.optString("title") to n.optLong("createdAt")]?.let { n.optLong("id") to it }
        }.toMap()
        notesJson?.optJSONArray("templates").objects().forEach { t ->
            val name = t.optString("name")
            if (noteDao.getTemplatesSync().none { it.name == name }) {
                noteDao.insertTemplate(NoteTemplate(name = name, content = t.optString("content"), isChecklist = t.optBoolean("isChecklist"),
                    labelId = t.optString("label").takeIf { it.isNotBlank() && it != "null" }?.let { ln -> noteDao.getLabelsSync().firstOrNull { it.name.equals(ln, true) }?.id }))
            }
        }

        // Calendar: categories by name, then events with remapped category/note links.
        val calendar = root.optJSONObject("calendar")
        // Attached documents are linked after the documents themselves are imported (by title).
        val pendingDocuments = mutableListOf<Triple<String, Long, String>>()
        val categoryIds = eventDao.getCategoriesSync().associate { it.name.lowercase() to it.id }.toMutableMap()
        val oldCategoryToNew = calendar?.optJSONArray("categories").objects().associate { c ->
            val name = c.optString("name")
            c.optLong("id") to categoryIds.getOrPut(name.lowercase()) {
                eventDao.insertCategory(Category(name = name, color = c.optInt("color"), isDefault = c.optBoolean("isDefault")))
            }
        }
        var events = 0
        var skipped = noteSummary.skipped
        calendar?.optJSONArray("events").objects().forEach { e ->
            val event = Event(
                title = e.optString("title"),
                description = e.optString("description"),
                startTimeMillis = e.optLong("startTimeMillis"),
                endTimeMillis = e.optLongOrNull("endTimeMillis"),
                isAllDay = e.optBoolean("isAllDay"),
                targetAddress = e.optStringOrNull("targetAddress"),
                arrivalBufferMinutes = e.optInt("arrivalBufferMinutes"),
                alarmLeadMinutes = e.optInt("alarmLeadMinutes"),
                color = e.optLongOrNull("color")?.toInt(),
                recurrenceRule = e.optStringOrNull("recurrenceRule"),
                timezone = e.optStringOrNull("timezone") ?: java.time.ZoneId.systemDefault().id,
                isBirthday = e.optBoolean("isBirthday"),
                birthYear = e.optLongOrNull("birthYear")?.toInt(),
                categoryId = e.optLongOrNull("categoryId")?.let { oldCategoryToNew[it] },
                linkedNoteId = e.optLongOrNull("linkedNoteId")?.let { oldNoteIdToNew[it] },
                exDates = e.optString("exDates").takeIf { it.isNotBlank() && it != "null" }
            )
            val reminders = e.optJSONArray("reminders").objects().map { r ->
                Reminder(
                    eventId = 0,
                    reminderTimeMillis = r.optLong("reminderTimeMillis"),
                    type = r.optString("type"),
                    timezone = r.optStringOrNull("timezone") ?: event.timezone
                )
            }
            if (insertEvent(event, reminders)) events++ else skipped++
            e.optString("documentTitle").takeIf { it.isNotBlank() && it != "null" }
                ?.let { pendingDocuments += Triple(event.title, event.startTimeMillis, it) }
        }
        calendar?.optJSONArray("templates").objects().forEach { t ->
            val name = t.optString("name")
            if (eventDao.getEventTemplatesSync().none { it.name == name }) {
                eventDao.insertEventTemplate(
                    EventTemplate(
                        name = name, title = t.optString("title"), description = t.optString("description"),
                        isAllDay = t.optBoolean("isAllDay"), targetAddress = t.optStringOrNull("targetAddress"),
                        arrivalBufferMinutes = t.optInt("arrivalBufferMinutes"), alarmLeadMinutes = t.optInt("alarmLeadMinutes"),
                        categoryId = t.optLongOrNull("categoryId")?.let { oldCategoryToNew[it] },
                        recurrenceRule = t.optStringOrNull("recurrenceRule")
                    )
                )
            }
        }

        // Waypoints
        val wp = root.optJSONObject("waypoints")
        val wpSummary = insertWaypoints(
            wp?.optJSONArray("labels").objects().map { ImportedWaypointLabel(it.optString("name"), it.optString("colorHex", "#CE93D8")) },
            wp?.optJSONArray("waypoints").objects().map { w ->
                ImportedWaypoint(
                    name = w.optString("name"), address = w.optString("address"), notes = w.optString("notes"),
                    lat = w.optDoubleOrNull("lat"), lng = w.optDoubleOrNull("lng"), isPinned = w.optBoolean("isPinned"),
                    lastUsed = w.optLong("lastUsed"), lastEdited = w.optLong("lastEdited"),
                    labelNames = w.optJSONArray("labels")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
                )
            },
            ImportSource.LIFEORGANIZER_BACKUP
        )
        skipped += wpSummary.skipped

        // Documents (file links; they open as long as the file still exists and access was granted on this device)
        val knownUris = documentDao.getAllDocumentsSync().map { it.uri }.toSet()
        var documents = 0
        root.optJSONObject("documents")?.optJSONArray("documents").objects().forEach { d ->
            val uri = d.optString("uri")
            if (uri in knownUris) { skipped++; return@forEach }
            documentDao.insertDocument(
                Document(
                    title = d.optString("title"), uri = uri, category = d.optString("category"),
                    expiryDate = d.optLongOrNull("expiryDate"), isPinned = d.optBoolean("isPinned"),
                    isPdf = d.optBoolean("isPdf"), dateAdded = d.optLong("dateAdded", System.currentTimeMillis())
                )
            )
            documents++
        }
        // Notes with an attached document (matched by title + creation time, document by title)
        val docsByTitle = documentDao.getAllDocumentsSync().associateBy { it.title }
        backupNotes.forEach { n ->
            val docTitle = n.optString("documentTitle").takeIf { it.isNotBlank() && it != "null" } ?: return@forEach
            val doc = docsByTitle[docTitle] ?: return@forEach
            val id = noteIds[n.optString("title") to n.optLong("createdAt")] ?: return@forEach
            noteDao.getNoteWithLabels(id)?.note?.takeIf { it.documentId == null }?.let { noteDao.updateNote(it.copy(documentId = doc.id)) }
        }
        val transactions = root.optJSONObject("money")?.let { com.example.lifeorganizer.money.data.MoneyBackup.import(moneyDao, it) } ?: 0
        if (root.has("money")) com.example.lifeorganizer.money.FixedCostCalendarSync(appContext)
            .syncAll(com.example.lifeorganizer.core.settings.SettingsManager(appContext).languageCode.first())
        root.optJSONObject("settings")?.let { importSettings(it) }

        if (pendingDocuments.isNotEmpty()) {
            val docs = documentDao.getAllDocumentsSync()
            val stored = eventDao.getEventsWithRemindersSync().map { it.event }
            pendingDocuments.forEach { (title, start, docTitle) ->
                val doc = docs.firstOrNull { it.title == docTitle } ?: return@forEach
                stored.firstOrNull { it.title == title && it.startTimeMillis == start && it.documentId == null }
                    ?.let { eventDao.updateEvent(it.copy(documentId = doc.id)) }
            }
        }

        return ImportSummary(
            ImportSource.LIFEORGANIZER_BACKUP,
            events = events,
            notes = noteSummary.notes,
            waypoints = wpSummary.waypoints,
            documents = documents,
            transactions = transactions,
            skipped = skipped
        )
    }
}
