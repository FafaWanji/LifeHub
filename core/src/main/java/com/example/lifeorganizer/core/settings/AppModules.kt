package com.example.lifeorganizer.core.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.Txt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Optional modules next to calendar and notes. Switching one off only hides it: its data stays in
 * its database (and in backups) and is all there again when it is switched back on.
 * A new module only needs an entry here; it is on by default because only switched-off keys are stored.
 */
enum class AppModule(val key: String, val label: Txt) {
    WAYPOINTS("waypoints", Str.waypoints),
    DOCUMENTS("documents", Str.documents),
    FINANCE("finance", Str.finance)
}

/** The modules that are switched on. */
val SettingsManager.enabledModules: Flow<Set<AppModule>>
    get() = disabledModuleKeys.map { off -> AppModule.entries.filterTo(mutableSetOf()) { it.key !in off } }

/** Switched-on modules as Compose state; everything counts as on until the setting has loaded. */
@Composable
fun rememberEnabledModules(): Set<AppModule> {
    val context = LocalContext.current.applicationContext
    val flow = remember(context) { SettingsManager(context).enabledModules }
    val modules by flow.collectAsState(initial = AppModule.entries.toSet())
    return modules
}
