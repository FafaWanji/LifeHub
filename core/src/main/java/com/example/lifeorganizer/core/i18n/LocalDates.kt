package com.example.lifeorganizer.core.i18n

import android.text.format.DateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Java locale for an app language code; "zh" is Simplified Chinese. */
fun appLocale(lang: String): Locale = if (lang == "zh") Locale.SIMPLIFIED_CHINESE else Locale.forLanguageTag(lang)

/**
 * Date formatter in the language's own order, from a skeleton like "yyyyMMMM" or "EEEEMMMMdd":
 * "Oktober 2026" in German, "2026年10月" in Chinese.
 */
fun localDateFormatter(lang: String, skeleton: String): DateTimeFormatter {
    val locale = appLocale(lang)
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}
