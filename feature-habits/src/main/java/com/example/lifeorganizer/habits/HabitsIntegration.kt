package com.example.lifeorganizer.habits

import android.content.Context

/** Hooks the app sets so this module can tell it about changes (e.g. to refresh the home-screen widget). */
object HabitsIntegration {
    @Volatile
    var onChanged: (suspend (Context) -> Unit)? = null

    suspend fun changed(context: Context) {
        runCatching { onChanged?.invoke(context.applicationContext) }
    }
}
