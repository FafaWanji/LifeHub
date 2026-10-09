package com.example.lifeorganizer.calendar.debug

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object DebugLogger {
    val logs = mutableStateListOf<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("dd.MM. HH:mm:ss", Locale.getDefault())

    /** Also kept in a file, so the developer screen still has it after a crash or restart. */
    private var file: File? = null
    private val writer = Executors.newSingleThreadExecutor()
    private const val KEEP_LINES = 500

    fun init(context: Context) {
        file = File(context.filesDir, "debug.log")
    }

    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        logs.add(0, "[$timestamp] $message")
        if (logs.size > 100) {
            logs.removeAt(logs.size - 1)
        }
        val f = file ?: return
        val line = "[${fileDateFormat.format(Date())}] $message\n"
        writer.execute {
            runCatching {
                f.appendText(line)
                // Trim now and then instead of on every line
                if (f.length() > 200_000) f.writeText(f.readLines().takeLast(KEEP_LINES).joinToString("\n", postfix = "\n"))
            }
        }
    }

    /** Saved log, oldest first. */
    fun saved(): String = file?.takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }.orEmpty()

    fun clearSaved() {
        logs.clear()
        val f = file ?: return
        writer.execute { f.delete() }
    }
}
