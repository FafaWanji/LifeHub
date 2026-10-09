package com.example.lifeorganizer.money.data

import android.content.Context
import com.example.lifeorganizer.money.domain.CsvRow
import com.example.lifeorganizer.money.domain.PayeeNormalizer
import com.example.lifeorganizer.money.domain.RecurringScheduler
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class ImportResult(val imported: Int, val duplicates: Int, val matchedRecurring: Int)

/** All write logic of the finance area: learned rules, fixed-cost booking, CSV import. */
class MoneyRepository(
    private val dao: MoneyDao,
    private val clock: () -> LocalDate = { LocalDate.now() }
) {
    companion object {
        fun get(context: Context) = MoneyRepository(MoneyDatabase.getDatabase(context).moneyDao())

        private fun rowKey(row: CsvRow) =
            "${row.date.toEpochDay()}|${row.amountCents}|${PayeeNormalizer.normalize(row.payee)}|${row.purpose.trim().lowercase()}"

        /** Stable id of a CSV row; [occurrence] keeps identical rows (two coffees on one day) apart. */
        fun importHash(row: CsvRow, occurrence: Int): String =
            MessageDigest.getInstance("SHA-1").digest("${rowKey(row)}|$occurrence".toByteArray())
                .joinToString("") { "%02x".format(it) }

        /** Fixed costs still due this month after [today] (negative for expenses). */
        fun expectedAfter(today: LocalDate, recurring: List<Recurring>): Long {
            val end = YearMonth.from(today).atEndOfMonth()
            return recurring.sumOf { r ->
                RecurringScheduler.dueUntil(LocalDate.ofEpochDay(r.nextDueEpochDay), r.dayOfMonth, r.interval, end)
                    .count { it.isAfter(today) } * r.amountCents
            }
        }
    }

    fun transactionsIn(month: YearMonth) =
        dao.transactionsBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())
    fun categories() = dao.categories()
    fun recurring() = dao.recurring()
    fun rules() = dao.rules()

    suspend fun ensureDefaults(lang: String) {
        if (dao.categoriesSync().isNotEmpty()) return
        DefaultCategories.all.forEachIndexed { i, d ->
            dao.insertCategory(
                MoneyCategory(name = d.name.of(lang), color = d.color, icon = d.icon, kind = d.kind, sortOrder = i, isFallback = d.fallback)
            )
        }
    }

    suspend fun fallbackId(kind: CategoryKind): Long? =
        dao.categoriesSync().firstOrNull { it.kind == kind && it.isFallback }?.id

    /** Learned category of a counterpart, else "other" of the matching kind. */
    suspend fun categoryFor(payee: String, amountCents: Long): Long? {
        val key = PayeeNormalizer.normalize(payee)
        val rule = if (key.isEmpty()) null else dao.rulesSync().firstOrNull { it.normalizedPayee == key }
        return rule?.categoryId ?: fallbackId(if (amountCents >= 0) CategoryKind.INCOME else CategoryKind.EXPENSE)
    }

    /** Remembers payee → category; "other" is never learned so unknown payees stay unknown. */
    private suspend fun learn(payee: String, categoryId: Long?) {
        val key = PayeeNormalizer.normalize(payee)
        if (key.isEmpty() || categoryId == null) return
        if (dao.categoriesSync().firstOrNull { it.id == categoryId }?.isFallback != false) return
        dao.upsertRule(PayeeRule(key, categoryId))
    }

    suspend fun saveTransaction(tx: MoneyTransaction): Long {
        val id = if (tx.id == 0L) dao.insertTransaction(tx) else { dao.updateTransaction(tx); tx.id }
        learn(tx.title, tx.categoryId)
        return id
    }

    suspend fun deleteTransaction(tx: MoneyTransaction) = dao.deleteTransaction(tx)

    /** Undo of a delete: same id again. */
    suspend fun restoreTransaction(tx: MoneyTransaction) { dao.insertTransaction(tx) }

    suspend fun addFromSmartAdd(title: String, amountCents: Long, epochDay: Long, categoryName: String?): Long {
        val byName = categoryName?.let { n -> dao.categoriesSync().firstOrNull { it.name.equals(n.trim(), ignoreCase = true) }?.id }
        val tx = MoneyTransaction(
            epochDay = epochDay, amountCents = amountCents, title = title,
            categoryId = byName ?: categoryFor(title, amountCents), source = TxSource.SMART_ADD
        )
        return saveTransaction(tx)
    }

    suspend fun saveCategory(c: MoneyCategory) {
        if (c.id == 0L) dao.insertCategory(c) else dao.updateCategory(c)
    }

    suspend fun deleteCategory(c: MoneyCategory) {
        if (c.isFallback) return
        val fallback = fallbackId(c.kind) ?: return
        dao.deleteCategory(c, fallback)
    }

    /** New template: computes the first due date from start date and day. Edit: moves the next due date to the new day. */
    suspend fun saveRecurring(r: Recurring): Recurring =
        if (r.id == 0L) {
            val first = RecurringScheduler.firstDue(LocalDate.ofEpochDay(r.startEpochDay), r.dayOfMonth)
            val withDue = r.copy(nextDueEpochDay = first.toEpochDay())
            withDue.copy(id = dao.insertRecurring(withDue))
        } else {
            val next = LocalDate.ofEpochDay(r.nextDueEpochDay)
            val moved = RecurringScheduler.clamp(YearMonth.from(next), r.dayOfMonth)
            r.copy(nextDueEpochDay = moved.toEpochDay()).also { dao.updateRecurring(it) }
        }

    suspend fun setCalendarEventId(id: Long, eventId: Long?) {
        dao.recurringSync().firstOrNull { it.id == id }?.let { dao.updateRecurring(it.copy(calendarEventId = eventId)) }
    }

    suspend fun deleteRecurring(r: Recurring) = dao.deleteRecurring(r)
    suspend fun deleteRule(rule: PayeeRule) = dao.deleteRule(rule)

    /** Books every due date up to today, missed months included. Safe to call any number of times. */
    suspend fun bookDueRecurring(): Int {
        val today = clock()
        var booked = 0
        dao.recurringSync().forEach { r ->
            val dues = RecurringScheduler.dueUntil(LocalDate.ofEpochDay(r.nextDueEpochDay), r.dayOfMonth, r.interval, today)
            if (dues.isEmpty()) return@forEach
            dues.forEach { d ->
                val id = dao.insertTransaction(
                    MoneyTransaction(
                        epochDay = d.toEpochDay(), amountCents = r.amountCents, title = r.title, categoryId = r.categoryId,
                        source = TxSource.RECURRING, recurringId = r.id, recurringDueDay = d.toEpochDay()
                    )
                )
                if (id != -1L) booked++
            }
            dao.updateRecurring(r.copy(nextDueEpochDay = RecurringScheduler.following(dues.last(), r.dayOfMonth, r.interval).toEpochDay()))
        }
        return booked
    }

    /**
     * Category per row: learned rule, else [ai] (only for unknown payees, one call), else "other".
     * [ai] gets payee names and category names and returns payee → category name.
     */
    suspend fun suggestCategories(
        rows: List<CsvRow>,
        ai: (suspend (payees: List<String>, categories: List<String>) -> Map<String, String>)?
    ): List<Long?> {
        val categories = dao.categoriesSync()
        val rules = dao.rulesSync().associate { it.normalizedPayee to it.categoryId }
        val unknown = rows.map { it.title }
            .filter { PayeeNormalizer.normalize(it).let { k -> k.isNotEmpty() && k !in rules } }
            .distinctBy { PayeeNormalizer.normalize(it) }
        val aiByKey: Map<String, Long> = if (ai != null && unknown.isNotEmpty()) {
            val byName = categories.associateBy { it.name }
            ai(unknown, categories.filter { !it.isFallback }.map { it.name })
                .mapNotNull { (payee, name) -> byName[name]?.let { PayeeNormalizer.normalize(payee) to it.id } }
                .toMap()
        } else emptyMap()
        val income = fallbackId(CategoryKind.INCOME)
        val expense = fallbackId(CategoryKind.EXPENSE)
        return rows.map { r ->
            val key = PayeeNormalizer.normalize(r.title)
            rules[key] ?: aiByKey[key] ?: if (r.amountCents >= 0) income else expense
        }
    }

    suspend fun importRows(rows: List<CsvRow>, categoryIds: List<Long?>): ImportResult {
        val known = dao.importHashes().toHashSet()
        val occurrences = HashMap<String, Int>()
        val templates = dao.recurringSync().toMutableList()
        var imported = 0
        var duplicates = 0
        var matched = 0
        rows.forEachIndexed { i, row ->
            val occurrence = occurrences.merge(rowKey(row), 1, Int::plus)!! - 1
            val hash = importHash(row, occurrence)
            if (hash in known) { duplicates++; return@forEachIndexed }
            val day = row.date.toEpochDay()

            // Fixed cost already booked by the app → the bank row replaces it.
            val booked = dao.findBookedRecurring(row.amountCents, day - 3, day + 3)
            if (booked != null) {
                dao.updateTransaction(booked.copy(epochDay = day, title = row.title.ifBlank { booked.title }, source = TxSource.CSV, importHash = hash))
                known += hash; matched++
                return@forEachIndexed
            }

            // Bank was earlier than the due date → this row is that month's fixed cost.
            val upcoming = templates.firstOrNull { it.amountCents == row.amountCents && abs(it.nextDueEpochDay - day) <= 3 }
            val categoryId = categoryIds.getOrNull(i) ?: upcoming?.categoryId
            val tx = MoneyTransaction(
                epochDay = day, amountCents = row.amountCents, title = row.title,
                note = row.purpose.takeIf { it.isNotBlank() && it != row.title }.orEmpty(),
                categoryId = categoryId, source = TxSource.CSV, importHash = hash,
                recurringId = upcoming?.id, recurringDueDay = upcoming?.nextDueEpochDay
            )
            if (dao.insertTransaction(tx) == -1L) { duplicates++; return@forEachIndexed }
            if (upcoming != null) {
                val advanced = upcoming.copy(
                    nextDueEpochDay = RecurringScheduler.following(LocalDate.ofEpochDay(upcoming.nextDueEpochDay), upcoming.dayOfMonth, upcoming.interval).toEpochDay()
                )
                dao.updateRecurring(advanced)
                templates[templates.indexOf(upcoming)] = advanced
                matched++
            }
            learn(row.title, categoryId)
            known += hash; imported++
        }
        return ImportResult(imported, duplicates, matched)
    }
}
