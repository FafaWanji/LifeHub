package com.example.lifeorganizer.web

import android.content.Context

/** When PC access switches itself on. */
enum class AutoStart {
    /** Only by hand; off again after 30 minutes without use. */
    OFF,
    /** Whenever LifeHub is opened (in a Wi-Fi); off again after 30 minutes without use. */
    APP_OPEN,
    /** Runs while the phone is in a Wi-Fi – any, or only the remembered ones. */
    WIFI,
    /** Runs all the time. */
    ALWAYS;

    /** These keep the service alive in the background instead of stopping after a while. */
    val persistent: Boolean get() = this == WIFI || this == ALWAYS
}

/** Name (for <name>.local), port and automatic start of the PC access server. */
class WebSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("web_access", Context.MODE_PRIVATE)

    var name: String
        get() = sanitizeName(prefs.getString("name", DEFAULT_NAME).orEmpty())
        set(value) { prefs.edit().putString("name", sanitizeName(value)).apply() }

    var port: Int
        get() = prefs.getInt("port", WebServer.DEFAULT_PORT).takeIf(::validPort) ?: WebServer.DEFAULT_PORT
        set(value) { if (validPort(value)) prefs.edit().putInt("port", value).apply() }

    var autoStart: AutoStart
        get() = runCatching { AutoStart.valueOf(prefs.getString("auto_start", null) ?: "") }.getOrDefault(AutoStart.OFF)
        set(value) { prefs.edit().putString("auto_start", value.name).apply() }

    /** With [AutoStart.WIFI]: only in the remembered networks instead of every Wi-Fi. */
    var onlyKnownWifi: Boolean
        get() = prefs.getBoolean("only_known_wifi", false)
        set(value) { prefs.edit().putBoolean("only_known_wifi", value).apply() }

    /** Below this battery level (in %, not while charging) PC access switches off; 0 = never. */
    /**
     * Ended by hand (notification or switch): the automatic start (app open, reboot) leaves it off until it is
     * switched on again or the mode is changed.
     */
    var stoppedByUser: Boolean
        get() = prefs.getBoolean("stopped_by_user", false)
        set(value) { prefs.edit().putBoolean("stopped_by_user", value).apply() }

    var lowBatteryOff: Int
        get() = prefs.getInt("low_battery_off", 0).coerceIn(0, 100)
        set(value) { prefs.edit().putInt("low_battery_off", value.coerceIn(0, 100)).apply() }

    /** Remembered networks: label → ids from [NetworkId.currentIds]. */
    var knownNetworks: Map<String, Set<String>>
        get() = runCatching {
            val o = org.json.JSONObject(prefs.getString("known_networks", null) ?: "{}")
            o.keys().asSequence().associateWith { k ->
                val a = o.getJSONArray(k)
                (0 until a.length()).map { a.getString(it) }.toSet()
            }
        }.getOrDefault(emptyMap())
        set(value) {
            val o = org.json.JSONObject()
            value.forEach { (label, ids) -> o.put(label, org.json.JSONArray(ids.toList())) }
            prefs.edit().putString("known_networks", o.toString()).apply()
        }

    companion object {
        const val DEFAULT_NAME = "lifehub"

        /** Lowercase letters, digits and dashes, max 30 chars – what works as a .local name everywhere. */
        fun sanitizeName(raw: String): String = raw.trim().lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            .replace(Regex("[^a-z0-9]+"), "-").trim('-').take(30).trim('-')
            .ifEmpty { DEFAULT_NAME }

        /** Ports below 1024 are not allowed for apps. */
        fun validPort(port: Int) = port in 1024..65535
    }
}
