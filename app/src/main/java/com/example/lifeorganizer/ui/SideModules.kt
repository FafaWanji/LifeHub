package com.example.lifeorganizer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TaskAlt
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.settings.AppModule

/**
 * The side modules for the launcher, in menu order, plus PC access. A new [AppModule] fails to compile
 * here until it gets its screen, so the launcher picks it up by itself.
 */
fun sideModules(
    enabled: Set<AppModule>, lang: String, navigate: (ActiveView) -> Unit, openWebAccess: () -> Unit
): List<LauncherItem> {
    val modules = AppModule.entries.filter { it in enabled }.map { m ->
        val (view, icon) = when (m) {
            AppModule.HABITS -> ActiveView.HABITS to Icons.Outlined.TaskAlt
            AppModule.RELEASES -> ActiveView.RELEASES to Icons.Outlined.NewReleases
            AppModule.FINANCE -> ActiveView.MONEY to Icons.Outlined.Savings
            AppModule.WAYPOINTS -> ActiveView.WAYPOINTS to Icons.Outlined.Place
            AppModule.DOCUMENTS -> ActiveView.DOCUMENTS to Icons.Outlined.Description
        }
        LauncherItem(m.key, m.label.of(lang), icon) { navigate(view) }
    }
    return modules + LauncherItem("web", com.example.lifeorganizer.core.i18n.SetStr.pcAccess.of(lang), Icons.Outlined.Computer, openWebAccess)
}
