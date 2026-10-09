package com.example.lifeorganizer.web

import android.content.Context
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.money.data.MoneyDatabase
import com.example.lifeorganizer.money.data.MoneyRepository
import com.example.lifeorganizer.money.data.MoneyTransaction
import com.example.lifeorganizer.money.domain.MoneyMath
import com.example.lifeorganizer.notes.data.Note
import com.example.lifeorganizer.notes.data.NotesDatabase
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

data class ApiResponse(val status: Int, val body: String) {
    companion object {
        fun ok(json: Any) = ApiResponse(200, json.toString())
        fun error(status: Int, code: String) = ApiResponse(status, JSONObject().put("error", code).toString())
    }
}

/**
 * The JSON API the browser page uses. Kept free of HTTP details (the server only adapts requests) so the
 * same calls could later be served by a cloud backend.
 */
class WebApi(context: Context) {
    private val app = context.applicationContext
    private val settings = SettingsManager(app)
    private val calendar = CalendarAccess(app)
    private val noteDao = NotesDatabase.getDatabase(app).noteDao()
    private val moneyDao = MoneyDatabase.getDatabase(app).moneyDao()
    private val money = MoneyRepository.get(app)
    val changes = ChangeTracker(app)

    private suspend fun lang() = settings.languageCode.first()

    suspend fun info(): ApiResponse = ApiResponse.ok(JSONObject().put("app", "LifeHub").put("lang", lang()))

    suspend fun handle(method: String, path: String, query: Map<String, String>, body: String): ApiResponse = runCatching {
        val parts = path.removePrefix("/api/").trim('/').split('/')
        val id = parts.getOrNull(2)?.toLongOrNull()
        when {
            parts == listOf("changes") -> ApiResponse.ok(JSONObject().put("version", changes.version.get()))
            parts[0] == "calendar" -> calendar(method, parts, id, query, body)
            parts[0] == "notes" -> notes(method, parts.getOrNull(1)?.toLongOrNull(), body)
            parts[0] == "money" -> money(method, parts, id, query, body)
            else -> ApiResponse.error(404, "not_found")
        }
    }.getOrElse { e ->
        if (e is org.json.JSONException || e is IllegalArgumentException) ApiResponse.error(400, "bad_request")
        else ApiResponse.error(500, "server_error")
    }

    fun close() = changes.close()

    // ------------------------------------------------------------------ calendar

    private suspend fun calendar(method: String, parts: List<String>, id: Long?, query: Map<String, String>, body: String): ApiResponse =
        when {
            parts.getOrNull(1) == "categories" && method == "GET" ->
                ApiResponse.ok(JSONArray(calendar.categories().map { JSONObject().put("id", it.id).put("name", it.name).put("color", it.color) }))
            parts.getOrNull(1) != "events" -> ApiResponse.error(404, "not_found")
            method == "GET" -> {
                val from = LocalDate.parse(query["from"] ?: throw IllegalArgumentException())
                val to = LocalDate.parse(query["to"] ?: throw IllegalArgumentException())
                require(!to.isBefore(from) && to.toEpochDay() - from.toEpochDay() <= 62)
                ApiResponse.ok(JSONArray(calendar.occurrences(from, to).map { o ->
                    JSONObject().put("id", o.event.id).put("title", o.event.title).put("description", o.event.description)
                        .put("start", o.start).put("end", o.end ?: JSONObject.NULL).put("allDay", o.event.isAllDay)
                        .put("categoryId", o.event.categoryId ?: JSONObject.NULL).put("color", o.event.color ?: JSONObject.NULL)
                        .put("recurrence", o.event.recurrenceRule ?: JSONObject.NULL).put("location", o.event.targetAddress ?: JSONObject.NULL)
                        .put("seriesStart", o.event.startTimeMillis).put("seriesEnd", o.event.endTimeMillis ?: JSONObject.NULL)
                        .put("isBirthday", o.event.isBirthday)
                        .put("reminders", JSONArray(o.reminderMinutes))
                }))
            }
            method == "POST" && id == null -> ApiResponse.ok(JSONObject().put("id", calendar.create(eventInput(body), lang())))
            method == "PUT" && id != null ->
                if (calendar.update(id, eventInput(body), lang())) ApiResponse.ok(JSONObject().put("id", id)) else ApiResponse.error(404, "not_found")
            method == "DELETE" && id != null ->
                if (calendar.delete(id)) ApiResponse.ok(JSONObject().put("id", id)) else ApiResponse.error(404, "not_found")
            else -> ApiResponse.error(405, "method_not_allowed")
        }

    private fun eventInput(body: String): CalendarAccess.Input {
        val j = JSONObject(body)
        val reminders = j.optJSONArray("reminders")
        return CalendarAccess.Input(
            title = j.getString("title").take(500),
            description = j.optString("description").take(10_000),
            start = j.getLong("start"),
            end = if (j.isNull("end") || !j.has("end")) null else j.getLong("end"),
            allDay = j.optBoolean("allDay"),
            categoryId = if (j.isNull("categoryId") || !j.has("categoryId")) null else j.getLong("categoryId"),
            recurrence = j.optString("recurrence").takeIf { it.isNotBlank() && it != "null" }?.take(300),
            location = j.optString("location").takeIf { it.isNotBlank() && it != "null" }?.take(500),
            reminderMinutes = if (reminders == null) emptyList() else (0 until reminders.length()).map { reminders.getInt(it) }.filter { it in 0..60 * 24 * 30 }
        )
    }

    // ------------------------------------------------------------------ notes

    private suspend fun notes(method: String, id: Long?, body: String): ApiResponse {
        val now = System.currentTimeMillis()
        return when {
            method == "GET" && id == null -> ApiResponse.ok(JSONArray(
                noteDao.getAllNotesWithLabelsSync().filter { !it.note.isDeleted }
                    .sortedWith(compareByDescending<com.example.lifeorganizer.notes.data.NoteWithLabels> { it.note.isPinned }.thenByDescending { it.note.updatedAt })
                    .map { n ->
                        JSONObject().put("id", n.note.id).put("title", n.note.title).put("content", n.note.content)
                            .put("isPinned", n.note.isPinned).put("updatedAt", n.note.updatedAt)
                            .put("labels", JSONArray(n.labels.map { it.name }))
                    }
            ))
            method == "POST" && id == null -> {
                val j = JSONObject(body)
                val content = j.optString("content").take(200_000)
                val newId = noteDao.insertNote(
                    Note(
                        title = j.optString("title").take(500), content = content, createdAt = now, updatedAt = now,
                        isPinned = j.optBoolean("isPinned"), isChecklist = content.contains("- [ ]") || content.contains("- [x]")
                    )
                )
                ApiResponse.ok(JSONObject().put("id", newId))
            }
            method == "PUT" && id != null -> {
                val existing = noteDao.getNoteWithLabels(id)?.note?.takeIf { !it.isDeleted } ?: return ApiResponse.error(404, "not_found")
                val j = JSONObject(body)
                noteDao.updateNote(
                    existing.copy(
                        title = j.optString("title", existing.title).take(500),
                        content = j.optString("content", existing.content).take(200_000),
                        isPinned = j.optBoolean("isPinned", existing.isPinned),
                        updatedAt = now
                    )
                )
                ApiResponse.ok(JSONObject().put("id", id))
            }
            method == "DELETE" && id != null -> {
                noteDao.getNoteWithLabels(id) ?: return ApiResponse.error(404, "not_found")
                noteDao.softDeleteNote(id)
                ApiResponse.ok(JSONObject().put("id", id))
            }
            else -> ApiResponse.error(405, "method_not_allowed")
        }
    }

    // ------------------------------------------------------------------ money

    private suspend fun money(method: String, parts: List<String>, id: Long?, query: Map<String, String>, body: String): ApiResponse {
        money.ensureDefaults(lang())
        return when {
            parts.getOrNull(1) == "categories" && method == "GET" -> ApiResponse.ok(JSONArray(moneyDao.categoriesSync().map { c ->
                JSONObject().put("id", c.id).put("name", c.name).put("color", c.color).put("icon", c.icon)
                    .put("kind", c.kind.name).put("budget", c.monthlyBudgetCents ?: JSONObject.NULL).put("isFallback", c.isFallback)
            }))
            parts.getOrNull(1) == "month" && method == "GET" -> {
                money.bookDueRecurring()
                val month = YearMonth.parse(query["month"] ?: YearMonth.now().toString())
                val txs = money.transactionsIn(month).first()
                val summary = MoneyMath.summarize(txs.map { it.amountCents })
                val expected = if (month == YearMonth.now()) MoneyRepository.expectedAfter(LocalDate.now(), moneyDao.recurringSync()) else 0L
                ApiResponse.ok(
                    JSONObject().put("month", month.toString()).put("income", summary.incomeCents).put("expense", summary.expenseCents)
                        .put("rest", summary.restCents).put("expected", expected)
                        .put("byCategory", JSONArray(MoneyMath.spendingByCategory(txs.map { it.categoryId to it.amountCents }).map { (cat, cents) ->
                            JSONObject().put("categoryId", cat ?: JSONObject.NULL).put("cents", cents)
                        }))
                        .put("transactions", JSONArray(txs.map { t ->
                            JSONObject().put("id", t.id).put("epochDay", t.epochDay).put("date", LocalDate.ofEpochDay(t.epochDay).toString())
                                .put("amount", t.amountCents).put("title", t.title).put("note", t.note)
                                .put("categoryId", t.categoryId ?: JSONObject.NULL).put("source", t.source.name)
                        }))
                )
            }
            parts.getOrNull(1) != "transactions" -> ApiResponse.error(404, "not_found")
            method == "POST" && id == null -> {
                val j = JSONObject(body)
                val amount = j.getLong("amount")
                require(amount != 0L)
                val title = j.optString("title").take(200)
                val categoryId = if (j.isNull("categoryId") || !j.has("categoryId")) money.categoryFor(title, amount) else j.getLong("categoryId")
                val newId = money.saveTransaction(
                    MoneyTransaction(epochDay = j.getLong("epochDay"), amountCents = amount, title = title, note = j.optString("note").take(2000), categoryId = categoryId)
                )
                ApiResponse.ok(JSONObject().put("id", newId))
            }
            method == "PUT" && id != null -> {
                val existing = moneyDao.transactionById(id) ?: return ApiResponse.error(404, "not_found")
                val j = JSONObject(body)
                money.saveTransaction(
                    existing.copy(
                        epochDay = j.optLong("epochDay", existing.epochDay), amountCents = j.optLong("amount", existing.amountCents),
                        title = j.optString("title", existing.title).take(200), note = j.optString("note", existing.note).take(2000),
                        categoryId = if (j.has("categoryId") && !j.isNull("categoryId")) j.getLong("categoryId") else existing.categoryId
                    )
                )
                ApiResponse.ok(JSONObject().put("id", id))
            }
            method == "DELETE" && id != null -> {
                val existing = moneyDao.transactionById(id) ?: return ApiResponse.error(404, "not_found")
                money.deleteTransaction(existing)
                ApiResponse.ok(JSONObject().put("id", id))
            }
            else -> ApiResponse.error(405, "method_not_allowed")
        }
    }
}
