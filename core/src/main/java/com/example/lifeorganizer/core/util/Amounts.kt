package com.example.lifeorganizer.core.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/** Amounts as Long cents: parsing bank and user input, formatting for display (euro unless set otherwise). */
object Amounts {
    /** ISO code from the finance settings, e.g. "EUR", "USD", "GBP"; set by the app at start and on change. */
    @Volatile
    var currencyCode: String = "EUR"

    // Spaces, currency signs and codes around the number are ignored
    private val noise = Regex("""(?i)\s| | |\p{Sc}|eur(o)?|usd|gbp|chf|try|cny|rmb""")

    /**
     * "1.234,56", "-12,50", "12.50", "1,234.56", "12 €", "+5", "12,5", "−3,00", "12,50-" → cents.
     * A single separator followed by exactly three digits is a thousands separator ("2.100" = 2100 €).
     * Returns null when the text is not a number.
     */
    fun parseCents(raw: String): Long? {
        var s = raw.replace('−', '-').replace(noise, "")
        if (s.isEmpty()) return null
        var negative = false
        when {
            s.startsWith("-") -> { negative = true; s = s.substring(1) }
            s.startsWith("+") -> s = s.substring(1)
        }
        if (s.endsWith("-")) { negative = true; s = s.dropLast(1) }
        if (s.isEmpty() || !s.all { it.isDigit() || it == '.' || it == ',' } || s.none { it.isDigit() }) return null

        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val decimal: Char? = when {
            lastDot >= 0 && lastComma >= 0 -> if (lastDot > lastComma) '.' else ','
            lastComma >= 0 -> if (s.count { it == ',' } == 1 && s.length - lastComma - 1 != 3) ',' else null
            lastDot >= 0 -> if (s.count { it == '.' } == 1 && s.length - lastDot - 1 != 3) '.' else null
            else -> null
        }
        val split = if (decimal == null) -1 else s.lastIndexOf(decimal)
        val intPart = (if (split < 0) s else s.substring(0, split)).filter { it.isDigit() }
        val fracPart = if (split < 0) "" else s.substring(split + 1)
        if (fracPart.length > 2 || !fracPart.all { it.isDigit() }) return null
        val euros = intPart.ifEmpty { "0" }.toLongOrNull() ?: return null
        val cents = fracPart.padEnd(2, '0').toLong()
        val total = euros * 100 + cents
        return if (negative) -total else total
    }

    private fun locale(lang: String): Locale = when (lang) {
        "de" -> Locale.GERMANY
        "tr" -> Locale.forLanguageTag("tr-TR")
        "es" -> Locale.forLanguageTag("es-ES")
        "zh" -> Locale.SIMPLIFIED_CHINESE
        else -> Locale.forLanguageTag("en-IE")
    }

    /** "1.234,56 €" (German), "€1,234.56" (English); sign kept as the locale writes it. */
    private fun currency(): Currency = runCatching { Currency.getInstance(currencyCode) }.getOrElse { Currency.getInstance("EUR") }

    fun format(cents: Long, lang: String): String =
        NumberFormat.getCurrencyInstance(locale(lang)).apply { currency = currency() }
            .format(cents / 100.0)

    /** "€", "$", "£" … as the language writes it, e.g. next to an amount field. */
    fun symbol(lang: String): String = currency().getSymbol(locale(lang))

    /** Always with an explicit sign: "+12,50 €" / "−12,50 €". */
    fun formatSigned(cents: Long, lang: String): String =
        (if (cents < 0) "−" else "+") + format(abs(cents), lang)
}
