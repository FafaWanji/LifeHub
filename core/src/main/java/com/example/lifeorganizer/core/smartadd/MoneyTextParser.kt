package com.example.lifeorganizer.core.smartadd

import com.example.lifeorganizer.core.util.Amounts
import java.time.LocalDate
import kotlin.math.abs

/**
 * Offline money detection: "12,50 Döner gestern", "Kino 9 € ausgegeben", "Gehalt 2.100 € bekommen".
 * A number counts as money with €/EUR/Euro or a decimal comma with two digits. Text that also has a clock
 * time ("20 Uhr", "18:30") stays an event.
 */
object MoneyTextParser {
    private val amountRx = Regex(
        """(?i)€\s*([+-]?\d[\d.]*(?:,\d{1,2})?)|([+-]?\d[\d.]*(?:[.,]\d{1,2})?)\s*(?:€|eur\b|euro\b)|(?<![\d.,:])([+-]?\d+,\d{2})(?![\d.,])"""
    )
    private val incomeWords = Regex("""(?i)(?<!\p{L})(gehalt|lohn|einnahme|bekommen|erhalten|salary|income|received)(?!\p{L})""")
    private val filler = Regex("""(?i)(?<!\p{L})(für|fuer|for|ausgegeben|bezahlt|gezahlt|paid|spent|bekommen|erhalten|received|bei|at|im|am|on)(?!\p{L})""")
    private val dayWords = mapOf("heute" to 0L, "today" to 0L, "gestern" to -1L, "yesterday" to -1L, "vorgestern" to -2L)
    private val clockTime = Regex("""(?i)(?<!\d)\d{1,2}(?::\d{2}|\s*uhr\b|\s*(?:am|pm)\b)""")
    private val dateRx = Regex("""(?<!\d)(\d{1,2})\.(\d{1,2})\.(\d{2,4})?(?!\d)""")

    fun parse(text: String, today: LocalDate = LocalDate.now()): SmartResult.Transaction? {
        val m = amountRx.find(text) ?: return null
        var rest = text.removeRange(m.range)
        if (clockTime.containsMatchIn(rest)) return null
        val raw = m.groupValues.drop(1).first { it.isNotEmpty() }
        val cents = Amounts.parseCents(raw) ?: return null

        var date = today
        Regex("""\p{L}+""").findAll(rest).firstOrNull { it.value.lowercase() in dayWords }?.let { w ->
            date = today.plusDays(dayWords.getValue(w.value.lowercase()))
            rest = rest.removeRange(w.range)
        }
        dateRx.find(rest)?.let { d ->
            val year = d.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it }
            runCatching { LocalDate.of(year ?: today.year, d.groupValues[2].toInt(), d.groupValues[1].toInt()) }.getOrNull()?.let { parsed ->
                // Money entries are about the past: a date without year that lies ahead means last year
                date = if (year == null && parsed.isAfter(today)) parsed.minusYears(1) else parsed
                rest = rest.removeRange(d.range)
            }
        }

        val income = raw.startsWith("+") || incomeWords.containsMatchIn(text)
        val title = rest.replace(filler, " ").replace(Regex("\\s{2,}"), " ")
            .trim(' ', ',', '-', '.', ':', '+').replaceFirstChar { it.uppercase() }
        return SmartResult.Transaction(
            title = title.ifBlank { text.trim() },
            amountCents = if (income) abs(cents) else -abs(cents),
            epochDay = date.toEpochDay()
        )
    }
}
