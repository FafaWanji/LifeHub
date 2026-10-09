package com.example.lifeorganizer.money.domain

import com.example.lifeorganizer.core.util.Amounts
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class CsvRow(val date: LocalDate, val amountCents: Long, val payee: String, val purpose: String) {
    /** Name shown for the transaction: the counterpart, else the start of the purpose. */
    val title: String get() = payee.ifBlank { purpose.take(40) }.trim()
}

/** Column indexes. [payer] is the paying side's column, used for incoming money (DKB has both). */
data class ColumnMapping(
    val date: Int,
    val amount: Int? = null,
    val debit: Int? = null,
    val credit: Int? = null,
    val sign: Int? = null,
    val payee: Int? = null,
    val payer: Int? = null,
    val purpose: Int? = null
) {
    val isUsable: Boolean get() = amount != null || debit != null || credit != null

    fun encode(): String = listOf(date, amount, debit, credit, sign, payee, payer, purpose).joinToString(",") { (it ?: -1).toString() }

    companion object {
        fun decode(s: String): ColumnMapping? {
            val v = s.split(",").map { it.toIntOrNull() ?: return null }
            if (v.size != 8 || v[0] < 0) return null
            fun n(i: Int) = v[i].takeIf { it >= 0 }
            return ColumnMapping(v[0], n(1), n(2), n(3), n(4), n(5), n(6), n(7))
        }
    }
}

data class CsvParseResult(
    val headerIndex: Int,
    val header: List<String>,
    /** First line after the header, shown when the user maps columns by hand. */
    val sample: List<String>,
    val mapping: ColumnMapping?,
    val rows: List<CsvRow>,
    val skipped: Int
)

/**
 * Reads bank exports (Sparkasse, ING, DKB, Volksbank, N26, Commerzbank, …) by column names instead of
 * positions, so format changes and unknown banks mostly still work. Lines above the header (account info)
 * and lines without a valid date and amount (balances, footers) are skipped.
 */
object CsvImporter {
    private val dateNames = listOf("buchungstag", "buchungsdatum", "buchung", "datum", "date", "bookingdate", "valutadatum", "valuta", "wertstellung")
    private val amountNames = listOf("betrag", "umsatz", "amount")
    private val debitNames = listOf("soll", "ausgang", "belastung", "debit")
    private val creditNames = listOf("haben", "eingang", "gutschrift", "credit")
    private val signNames = listOf("sollhaben")
    private val payeeNames = listOf(
        "namezahlungsbeteiligter", "beguenstigterzahlungspflichtiger", "empfaengerzahlungspflichtiger",
        "zahlungsempfaengerin", "zahlungsempfaenger", "auftraggeberempfaenger", "empfaenger", "auftraggeber",
        "payee", "partnername", "counterparty", "name"
    )
    private val payerNames = listOf("zahlungspflichtiger", "zahlungspflichtige")
    private val purposeNames = listOf("verwendungszweck", "vorgangverwendungszweck", "paymentreference", "reference", "buchungstext", "beschreibung", "description")
    private val excluded = listOf(
        "konto", "iban", "bic", "blz", "bankleitzahl", "nummer", "number", "saldo", "waehrung", "currency",
        "foreign", "glaeubiger", "mandat", "kundenreferenz"
    )
    private val dateFormats = listOf("dd.MM.yyyy", "dd.MM.yy", "yyyy-MM-dd", "dd/MM/yyyy").map { DateTimeFormatter.ofPattern(it) }

    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            // German banks often still export Windows-1252 (umlauts)
            String(bytes, Charset.forName("windows-1252"))
        }
    }

    fun normalizeHeader(h: String): String =
        h.lowercase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").filter { it in 'a'..'z' }

    fun headerKey(header: List<String>): String = header.joinToString("|") { normalizeHeader(it) }

    fun parseDate(s: String): LocalDate? {
        val t = s.trim()
        return dateFormats.firstNotNullOfOrNull { f -> runCatching { LocalDate.parse(t, f) }.getOrNull() }
    }

    fun parse(bytes: ByteArray, manual: ColumnMapping? = null): CsvParseResult {
        val lines = decode(bytes).lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvParseResult(-1, emptyList(), emptyList(), null, emptyList(), 0)
        val delimiter = detectDelimiter(lines.take(30))
        val table = lines.map { splitLine(it, delimiter) }

        val detected = if (manual == null) findHeader(table) else null
        val headerIndex = detected?.first ?: guessHeaderRow(table)
        val header = table[headerIndex]
        val sample = table.getOrNull(headerIndex + 1).orEmpty()
        val mapping = manual ?: detected?.second
        if (mapping == null || !mapping.isUsable) return CsvParseResult(headerIndex, header, sample, null, emptyList(), 0)

        val withSign = if (mapping.sign == null) mapping.copy(sign = findSignColumn(header, table.drop(headerIndex + 1))) else mapping
        val rows = mutableListOf<CsvRow>()
        var skipped = 0
        table.drop(headerIndex + 1).forEach { cells ->
            val row = toRow(cells, withSign)
            if (row == null) skipped++ else rows += row
        }
        return CsvParseResult(headerIndex, header, sample, withSign, rows, skipped)
    }

    private fun detectDelimiter(lines: List<String>): Char =
        listOf(';', ',', '\t').maxBy { d -> lines.sumOf { line -> countOutsideQuotes(line, d) } }

    private fun countOutsideQuotes(line: String, d: Char): Int {
        var inQuotes = false
        var n = 0
        line.forEach { c ->
            if (c == '"') inQuotes = !inQuotes else if (c == d && !inQuotes) n++
        }
        return n
    }

    /** Splits one line; supports "quoted; fields" and "" as an escaped quote. */
    fun splitLine(line: String, d: Char): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> { cur.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == d && !inQuotes -> { out += cur.toString().trim(); cur.clear() }
                else -> cur.append(c)
            }
            i++
        }
        out += cur.toString().trim()
        return out
    }

    private fun guessHeaderRow(table: List<List<String>>): Int {
        val window = table.take(30)
        val widest = window.maxOf { it.size }
        return window.indexOfFirst { it.size == widest }
    }

    private fun findHeader(table: List<List<String>>): Pair<Int, ColumnMapping>? {
        table.take(30).forEachIndexed { index, cells ->
            val m = mapColumns(cells)
            if (m != null) return index to m
        }
        return null
    }

    private fun mapColumns(cells: List<String>): ColumnMapping? {
        val names = cells.map { normalizeHeader(it) }
        val used = mutableSetOf<Int>()
        fun find(candidates: List<String>, containsAllowed: Boolean = true): Int? {
            for (c in candidates) {
                val i = names.indices.firstOrNull { it !in used && names[it] == c }
                if (i != null) { used += i; return i }
            }
            if (!containsAllowed) return null
            for (c in candidates) {
                val i = names.indices.firstOrNull { idx ->
                    idx !in used && names[idx].contains(c) && excluded.none { names[idx].contains(it) }
                }
                if (i != null) { used += i; return i }
            }
            return null
        }
        // Date must match exactly: metadata lines like "Datei erstellt am: …" would otherwise look like a header
        val date = find(dateNames, containsAllowed = false) ?: return null
        val sign = find(signNames, containsAllowed = false)
        val amount = find(amountNames)
        val debit = if (amount == null) find(debitNames, containsAllowed = false) else null
        val credit = if (amount == null) find(creditNames, containsAllowed = false) else null
        if (amount == null && debit == null && credit == null) return null
        val payee = find(payeeNames)
        val payer = find(payerNames)
        val purpose = find(purposeNames)
        return ColumnMapping(date, amount, debit, credit, sign, payee, payer, purpose)
    }

    /** Old Volksbank exports put S (debit) / H (credit) into an unnamed last column. */
    private fun findSignColumn(header: List<String>, data: List<List<String>>): Int? =
        header.indices.firstOrNull { i ->
            normalizeHeader(header[i]).let { it.isEmpty() || it == "sollhaben" } &&
                data.take(20).any { row -> row.getOrNull(i)?.trim()?.uppercase() in setOf("S", "H") }
        }

    private fun toRow(cells: List<String>, m: ColumnMapping): CsvRow? {
        fun cell(i: Int?): String = i?.let { cells.getOrNull(it) }?.trim().orEmpty()
        val date = parseDate(cell(m.date)) ?: return null
        val amount = if (m.amount != null) {
            Amounts.parseCents(cell(m.amount)) ?: return null
        } else {
            val debit = Amounts.parseCents(cell(m.debit))
            val credit = Amounts.parseCents(cell(m.credit))
            when {
                debit != null && debit != 0L -> -abs(debit)
                credit != null -> abs(credit)
                else -> return null
            }
        }
        val signed = when (cell(m.sign).uppercase()) {
            "S" -> -abs(amount)
            "H" -> abs(amount)
            else -> amount
        }
        val payee = if (signed >= 0 && m.payer != null) cell(m.payer).ifBlank { cell(m.payee) }
        else cell(m.payee).ifBlank { cell(m.payer) }
        return CsvRow(date, signed, payee, cell(m.purpose))
    }
}
