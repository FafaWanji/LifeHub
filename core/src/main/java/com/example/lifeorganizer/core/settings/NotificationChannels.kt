package com.example.lifeorganizer.core.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.lifeorganizer.core.i18n.Str

/**
 * Notification channels of all modules. Sound and vibration are set per channel in the system, so the
 * notification settings link straight to each one.
 */
object NotificationChannels {
    const val CALENDAR = "calendar_reminders"
    const val BIRTHDAYS = "birthday_reminders"
    const val HABITS = "habits"
    const val TASKS = "tasks"

    /** Creating an existing channel only updates its name, so this is safe before every post. */
    fun ensureHabitsAndTasks(context: Context, lang: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(HABITS, Str.channelHabits.of(lang), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(TASKS, Str.channelTasks.of(lang), NotificationManager.IMPORTANCE_HIGH))
    }

    /**
     * The system page for one channel's sound and vibration; the app's notification page when the channel
     * does not exist yet (nothing of that module was ever shown) or on Android before 8.
     */
    fun settingsIntent(context: Context, channelId: String): Intent {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
        }
        val exists = context.getSystemService(NotificationManager::class.java).getNotificationChannel(channelId) != null
        return if (exists) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }
}
