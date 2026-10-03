package com.example.lifeorganizer.calendar.debug

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    val logs = mutableStateListOf<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        logs.add(0, "[$timestamp] $message")
        if (logs.size > 100) {
            logs.removeAt(logs.size - 1)
        }
    }
}
