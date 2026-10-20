package com.example.lifeorganizer

import android.content.SharedPreferences
import androidx.glance.appwidget.updateAll
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.waypoints.WaypointsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LifeHubApplication : WaypointsApplication() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Kept as a field: SharedPreferences only holds listeners weakly
    private val microListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> onMicroSettingChanged(key) }

    override fun onCreate() {
        super.onCreate()
        // Ticking things off in the app or a notification updates the home-screen widget
        com.example.lifeorganizer.habits.HabitsIntegration.onChanged = { com.example.lifeorganizer.widgets.HabitsWidget.refresh(it) }
        // Settings read outside of Compose (formatting, backup) follow the settings screen
        MicroSettings.onBackupChanged = { com.example.lifeorganizer.backup.AutoBackup.schedule(it) }
        applyFormats()
        MicroSettings.prefs(this).registerOnSharedPreferenceChangeListener(microListener)
    }

    private fun applyFormats() {
        com.example.lifeorganizer.core.util.Amounts.currencyCode = MicroSettings.get(this, MicroSettings.CURRENCY)
        com.example.lifeorganizer.core.i18n.dateOrder = MicroSettings.get(this, MicroSettings.DATE_ORDER)
    }

    private fun onMicroSettingChanged(key: String?) {
        applyFormats()
        when (key) {
            MicroSettings.BACKUP_WIFI_ONLY.name -> com.example.lifeorganizer.backup.AutoBackup.schedule(this)
            // Task reminders are planned with these, so re-plan them
            MicroSettings.QUIET_HOURS.name, MicroSettings.QUIET_START.name, MicroSettings.QUIET_END.name, MicroSettings.DEADLINE_REPEAT.name ->
                scope.launch { com.example.lifeorganizer.habits.reminder.HabitReminders.scheduleAll(this@LifeHubApplication) }
            MicroSettings.WIDGET_DONE_HABITS.name, MicroSettings.DATE_ORDER.name ->
                scope.launch { com.example.lifeorganizer.widgets.HabitsWidget.refresh(this@LifeHubApplication) }
        }
        if (key == MicroSettings.WIDGET_BIRTHDAYS.name || key == MicroSettings.DATE_ORDER.name) {
            scope.launch { runCatching { com.example.lifeorganizer.calendar.widget.CalendarWidget().updateAll(this@LifeHubApplication) } }
        }
    }
}
