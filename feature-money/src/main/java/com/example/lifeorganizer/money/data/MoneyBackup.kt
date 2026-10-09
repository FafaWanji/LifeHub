package com.example.lifeorganizer.money.data

import com.example.lifeorganizer.money.domain.RecurringInterval
import org.json.JSONArray
import org.json.JSONObject

/** Finance part of the LifeHub backup. Import merges: categories by name, nothing existing is overwritten. */
object MoneyBackup {
    suspend fun export(dao: MoneyDao): JSONObject = JSONObject()
        .put("categories", JSONArray(dao.categoriesSync().map { c ->
            JSONObject().put("id", c.id).put("name", c.name).put("color", c.color).put("icon", c.icon)
                .put("kind", c.kind.name).put("budget", c.monthlyBudgetCents).put("sortOrder", c.sortOrder).put("isFallback", c.isFallback)
        }))
        .put("recurring", JSONArray(dao.recurringSync().map { r ->
            JSONObject().put("id", r.id).put("title", r.title).put("amount", r.amountCents).put("categoryId", r.categoryId)
                .put("interval", r.interval.name).put("day", r.dayOfMonth).put("start", r.startEpochDay)
                .put("nextDue", r.nextDueEpochDay).put("showInCalendar", r.showInCalendar)
        }))
        .put("transactions", JSONArray(dao.allTransactionsSync().map { t ->
            JSONObject().put("day", t.epochDay).put("amount", t.amountCents).put("title", t.title).put("note", t.note)
                .put("categoryId", t.categoryId).put("source", t.source.name).put("recurringId", t.recurringId)
                .put("recurringDueDay", t.recurringDueDay).put("importHash", t.importHash)
        }))
        .put("rules", JSONArray(dao.rulesSync().map { JSONObject().put("payee", it.normalizedPayee).put("categoryId", it.categoryId) }))

    private fun JSONObject.longOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
    private fun JSONObject.stringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)
    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    suspend fun import(dao: MoneyDao, json: JSONObject): Int {
        val existing = dao.categoriesSync()
        val categoryIds = json.optJSONArray("categories").objects().associate { c ->
            val kind = runCatching { CategoryKind.valueOf(c.optString("kind")) }.getOrDefault(CategoryKind.EXPENSE)
            val name = c.optString("name")
            val match = existing.firstOrNull { it.kind == kind && (it.name.equals(name, true) || (it.isFallback && c.optBoolean("isFallback"))) }
            c.optLong("id") to (match?.id ?: dao.insertCategory(
                MoneyCategory(
                    name = name, color = c.optInt("color"), icon = c.optString("icon", "other"), kind = kind,
                    monthlyBudgetCents = c.longOrNull("budget"), sortOrder = c.optInt("sortOrder"), isFallback = false
                )
            ))
        }

        val known = dao.recurringSync()
        val recurringIds = json.optJSONArray("recurring").objects().associate { r ->
            val interval = runCatching { RecurringInterval.valueOf(r.optString("interval")) }.getOrDefault(RecurringInterval.MONTHLY)
            val match = known.firstOrNull { it.title == r.optString("title") && it.amountCents == r.optLong("amount") && it.dayOfMonth == r.optInt("day") && it.interval == interval }
            r.optLong("id") to (match?.id ?: dao.insertRecurring(
                Recurring(
                    title = r.optString("title"), amountCents = r.optLong("amount"),
                    categoryId = r.longOrNull("categoryId")?.let { categoryIds[it] }, interval = interval,
                    dayOfMonth = r.optInt("day", 1), startEpochDay = r.optLong("start"), nextDueEpochDay = r.optLong("nextDue"),
                    // The calendar entry is rebuilt by the app after the import
                    showInCalendar = r.optBoolean("showInCalendar"), calendarEventId = null
                )
            ))
        }

        val hashes = dao.importHashes().toHashSet()
        val keys = dao.allTransactionsSync().map { Triple(it.epochDay, it.amountCents, it.title) }.toHashSet()
        var added = 0
        json.optJSONArray("transactions").objects().forEach { t ->
            val hash = t.stringOrNull("importHash")
            val key = Triple(t.optLong("day"), t.optLong("amount"), t.optString("title"))
            if ((hash != null && hash in hashes) || (hash == null && key in keys)) return@forEach
            val id = dao.insertTransaction(
                MoneyTransaction(
                    epochDay = key.first, amountCents = key.second, title = key.third, note = t.optString("note"),
                    categoryId = t.longOrNull("categoryId")?.let { categoryIds[it] },
                    source = runCatching { TxSource.valueOf(t.optString("source")) }.getOrDefault(TxSource.MANUAL),
                    recurringId = t.longOrNull("recurringId")?.let { recurringIds[it] },
                    recurringDueDay = t.longOrNull("recurringDueDay"), importHash = hash
                )
            )
            if (id != -1L) { added++; keys += key; hash?.let { hashes += it } }
        }

        json.optJSONArray("rules").objects().forEach { r ->
            val categoryId = categoryIds[r.optLong("categoryId")] ?: return@forEach
            val payee = r.optString("payee")
            if (payee.isNotBlank() && dao.rulesSync().none { it.normalizedPayee == payee }) dao.upsertRule(PayeeRule(payee, categoryId))
        }
        return added
    }
}
