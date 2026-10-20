package com.example.lifeorganizer.core.i18n

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Java locale for an app language code; "zh" is Simplified Chinese. */
fun appLocale(lang: String): Locale = if (lang == "zh") Locale.SIMPLIFIED_CHINESE else Locale.forLanguageTag(lang)

/**
 * Order of day, month and year from the appearance settings: "language" (the language's own),
 * "dmy", "mdy" or "ymd". Set by the app at start and when it changes.
 */
@Volatile
var dateOrder: String = "language"

/** "dmy", "mdy" or "ymd" as [locale] writes a numeric date. */
private fun orderOf(locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, "yMd")
    return listOf('d', 'M', 'y').sortedBy { c -> pattern.indexOf(c).let { if (it < 0) Int.MAX_VALUE else it } }
        .joinToString("") { it.lowercase() }
}

/** A locale whose patterns have neutral separators and the wanted order. */
private fun orderLocale(order: String): Locale? = when (order) {
    "dmy" -> Locale.UK
    "mdy" -> Locale.US
    "ymd" -> Locale.forLanguageTag("sv-SE")
    else -> null
}

/**
 * Date formatter in the language's own order, from a skeleton like "yyyyMMMM" or "EEEEMMMMdd":
 * "Oktober 2026" in German, "2026年10月" in Chinese. With another [dateOrder] the pattern follows that
 * order while month and day names stay in the app language.
 */
fun localDateFormatter(lang: String, skeleton: String, order: String = dateOrder): DateTimeFormatter {
    val locale = appLocale(lang)
    val patternLocale = if (order == "language" || orderOf(locale) == order) locale else orderLocale(order) ?: locale
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(patternLocale, skeleton), locale)
}
