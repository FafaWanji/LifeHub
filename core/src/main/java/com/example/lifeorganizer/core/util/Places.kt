package com.example.lifeorganizer.core.util

/** Small helpers for event locations, shared by calendar, Smart Add and waypoints. */
object Places {

    // Values AI models / imports put in when there is no real place.
    private val placeholders = setOf(
        "", "-", "--", "?", "n/a", "na", "none", "null", "nil", "unknown", "not specified", "no location",
        "tbd", "tba", "unbekannt", "keine", "keiner", "keine angabe", "nicht angegeben", "kein ort", "ohne ort",
        "bilinmiyor", "yok", "desconocido", "ninguno", "sin ubicación"
    )

    /** Trimmed address, or null when it is empty or only a placeholder like "N/A" / "Nicht angegeben". */
    fun clean(address: String?): String? {
        val trimmed = address?.trim()?.replace(Regex("\\s+"), " ") ?: return null
        return trimmed.takeUnless { it.lowercase().trim('.', ' ') in placeholders }
    }

    fun hasPlace(address: String?) = clean(address) != null

    /** Comparison key: "Hauptstraße 1, Berlin " and "hauptstrasse 1,berlin" are the same place. */
    fun key(address: String?): String? = clean(address)?.lowercase()
        ?.replace("ß", "ss")
        ?.replace(Regex("[^\\p{L}\\p{N}]"), "")

    fun samePlace(a: String?, b: String?): Boolean {
        val ka = key(a) ?: return false
        return ka == key(b)
    }
}
