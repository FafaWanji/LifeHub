package com.example.lifeorganizer.web

import android.content.Context

/** Name (for <name>.local) and port of the PC access server. */
class WebSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("web_access", Context.MODE_PRIVATE)

    var name: String
        get() = sanitizeName(prefs.getString("name", DEFAULT_NAME).orEmpty())
        set(value) { prefs.edit().putString("name", sanitizeName(value)).apply() }

    var port: Int
        get() = prefs.getInt("port", WebServer.DEFAULT_PORT).takeIf(::validPort) ?: WebServer.DEFAULT_PORT
        set(value) { if (validPort(value)) prefs.edit().putInt("port", value).apply() }

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
