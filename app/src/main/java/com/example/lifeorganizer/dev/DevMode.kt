package com.example.lifeorganizer.dev

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hidden developer mode for tracking down crashes and other problems. Unlocked by tapping the
 * version in "About" seven times; until then nothing of it is visible. Plain SharedPreferences
 * because the crash handler must read them synchronously while the app is going down.
 */
object DevMode {
    const val UNLOCK_TAPS = 7
    private const val PREFS = "dev_mode"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _crashLog = MutableStateFlow(false)
    val crashLog: StateFlow<Boolean> = _crashLog.asStateFlow()

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context) {
        val p = prefs(context)
        _enabled.value = p.getBoolean("enabled", false)
        _crashLog.value = p.getBoolean("crash_log", false)
    }

    /** Turning developer mode on starts the crash log right away; turning it off stops it. */
    fun setEnabled(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("enabled", on).apply()
        _enabled.value = on
        setCrashLog(context, on)
    }

    fun setCrashLog(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("crash_log", on).apply()
        _crashLog.value = on
    }
}
