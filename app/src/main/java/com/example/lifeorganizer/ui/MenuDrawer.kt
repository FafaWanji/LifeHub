package com.example.lifeorganizer.ui

import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import com.example.lifeorganizer.core.i18n.BkStr
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text

@Composable
fun MenuDrawerSheet(
    currentView: ActiveView,
    onNavigate: (ActiveView) -> Unit,
    onSettingsClick: () -> Unit,
    onBackupClick: () -> Unit = {},
    onChangelogClick: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onWebAccessClick: () -> Unit = {}
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp)) {
            DrawerHeader()

            SectionLabel(Str.organise.text())
            DrawerEntry(
                selectedIcon = Icons.Filled.CalendarMonth,
                icon = Icons.Outlined.CalendarMonth,
                label = Str.calendar.text(),
                selected = currentView == ActiveView.CALENDAR,
                onClick = { onNavigate(ActiveView.CALENDAR) }
            )
            DrawerEntry(
                selectedIcon = Icons.AutoMirrored.Filled.Notes,
                icon = Icons.AutoMirrored.Outlined.Notes,
                label = Str.notes.text(),
                selected = currentView == ActiveView.NOTES || currentView == ActiveView.NOTE_EDITOR,
                onClick = { onNavigate(ActiveView.NOTES) }
            )
            DrawerEntry(
                selectedIcon = Icons.Filled.Savings,
                icon = Icons.Outlined.Savings,
                label = Str.finance.text(),
                selected = currentView == ActiveView.MONEY,
                onClick = { onNavigate(ActiveView.MONEY) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            SectionLabel(Str.tools.text())
            DrawerEntry(
                selectedIcon = Icons.Filled.Place,
                icon = Icons.Outlined.Place,
                label = Str.waypoints.text(),
                selected = currentView == ActiveView.WAYPOINTS,
                onClick = { onNavigate(ActiveView.WAYPOINTS) }
            )
            DrawerEntry(
                selectedIcon = Icons.Filled.Description,
                icon = Icons.Outlined.Description,
                label = Str.documents.text(),
                selected = currentView == ActiveView.DOCUMENTS,
                onClick = { onNavigate(ActiveView.DOCUMENTS) }
            )
            DrawerEntry(
                selectedIcon = Icons.Outlined.Computer,
                icon = Icons.Outlined.Computer,
                label = com.example.lifeorganizer.web.WebStr.title.text(),
                selected = false,
                onClick = onWebAccessClick
            )

            Spacer(modifier = Modifier.weight(1f))

            DrawerEntry(
                selectedIcon = Icons.Outlined.SettingsBackupRestore,
                icon = Icons.Outlined.SettingsBackupRestore,
                label = BkStr.title.text(),
                selected = false,
                onClick = onBackupClick
            )

            DrawerEntry(
                selectedIcon = Icons.Outlined.SystemUpdate,
                icon = Icons.Outlined.SystemUpdate,
                label = Str.checkUpdates.text(),
                selected = false,
                onClick = onUpdateClick
            )
            DrawerEntry(
                selectedIcon = Icons.Outlined.NewReleases,
                icon = Icons.Outlined.NewReleases,
                label = Str.changelog.text(),
                selected = false,
                onClick = onChangelogClick
            )
            DrawerEntry(
                selectedIcon = Icons.Outlined.Info,
                icon = Icons.Outlined.Info,
                label = Str.about.text(),
                selected = false,
                onClick = onAboutClick
            )
            DrawerEntry(
                selectedIcon = Icons.Outlined.Settings,
                icon = Icons.Outlined.Settings,
                label = Str.settings.text(),
                selected = false,
                onClick = onSettingsClick
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    ),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Same hub mark as the launcher icon
            Icon(
                androidx.compose.ui.res.painterResource(com.example.lifeorganizer.R.drawable.ic_lifehub_foreground),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("LifeHub", style = MaterialTheme.typography.titleLarge)
            Text(
                Str.tagline.text(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerEntry(
    selectedIcon: ImageVector,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(if (selected) selectedIcon else icon, contentDescription = null) },
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
