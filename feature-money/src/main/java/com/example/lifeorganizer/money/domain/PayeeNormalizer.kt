package com.example.lifeorganizer.money.domain

/**
 * Turns a bank counterpart into a stable key for learned category rules:
 * "REWE MARKT 1234 BERLIN" and "REWE Markt GmbH//Berlin/DE" both become "rewe markt".
 * Keeps the first two words after dropping numbers, legal forms and one-letter fragments.
 */
object PayeeNormalizer {
    private val dropped = setOf(
        "gmbh", "mbh", "ag", "kg", "kgaa", "se", "co", "ohg", "ug", "ev", "inc", "ltd", "llc",
        "sarl", "sca", "cie", "et", "und", "and", "haftungsbeschraenkt"
    )

    fun normalize(raw: String): String {
        val base = raw.substringBefore("//").lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        return base.split(Regex("[^a-z0-9]+"))
            .filter { t -> t.length > 1 && t.none { it.isDigit() } && t !in dropped }
            .take(2)
            .joinToString(" ")
    }
}
