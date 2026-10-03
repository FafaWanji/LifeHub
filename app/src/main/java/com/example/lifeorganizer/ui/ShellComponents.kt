package com.example.lifeorganizer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.documents.ui.navigation.Screen
import com.example.lifeorganizer.documents.ui.screens.DetailScreen
import com.example.lifeorganizer.documents.ui.screens.DocumentViewModel
import com.example.lifeorganizer.documents.ui.screens.DocumentsScreen
import com.example.lifeorganizer.notes.data.NoteLabel
import com.example.lifeorganizer.notes.data.NoteTemplate
import com.example.lifeorganizer.waypoints.ui.AddWaypointScreen
import com.example.lifeorganizer.waypoints.ui.HomeScreen
import com.example.lifeorganizer.waypoints.ui.SettingsScreen
import com.example.lifeorganizer.waypoints.ui.MainViewModel as WaypointsViewModel

/**
 * Small corner button that morphs between calendar and notes: the shape goes from circle to
 * squircle, the colour cross-fades and the icon spins into the next one.
 */
@Composable
fun ViewSwitcherButton(showingNotes: Boolean, onToggle: () -> Unit) {
    val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val corner by animateIntAsState(if (showingNotes) 30 else 50, label = "switcher_corner")
    val rotation by animateFloatAsState(if (showingNotes) 180f else 0f, springSpec, label = "switcher_rotation")
    val container by animateColorAsState(
        if (showingNotes) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "switcher_container"
    )
    val content by animateColorAsState(
        if (showingNotes) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "switcher_content"
    )
    val description = if (showingNotes) Str.switchToCalendar.text() else Str.switchToNotes.text()

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content,
        shadowElevation = 3.dp,
        tonalElevation = 3.dp,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.rotate(rotation)) {
            AnimatedContent(
                targetState = showingNotes,
                transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.4f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.4f)) },
                label = "switcher_icon"
            ) { notes ->
                // Shows where the tap leads; counter-rotated so the glyph always ends upright.
                Icon(
                    imageVector = if (notes) Icons.Default.CalendarMonth else Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).rotate(if (notes) 180f else 0f)
                )
            }
        }
    }
}

@Composable
fun TemplatePickerDialog(
    templates: List<NoteTemplate>,
    labels: List<NoteLabel> = emptyList(),
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit,
    onPickLabel: (Long) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Str.chooseTemplate.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TemplateRow(Icons.AutoMirrored.Outlined.NoteAdd, Str.blankNote.text(), null) { onPick(null) }
                templates.forEach { template ->
                    TemplateRow(Icons.Default.Description, template.name, template.content) { onPick(template.id) }
                }
                // Starting with a label assigns it and fills in the label's template.
                if (labels.isNotEmpty()) {
                    Text(
                        Str.labels.text(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    labels.forEach { label ->
                        TemplateRow(
                            icon = Icons.AutoMirrored.Filled.Label,
                            title = label.name,
                            preview = label.template,
                            tint = Color(label.color)
                        ) { onPickLabel(label.id) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Str.cancel.text()) } }
    )
}

@Composable
private fun TemplateRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    preview: String?,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint.takeOrElse { MaterialTheme.colorScheme.primary })
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (!preview.isNullOrBlank()) {
                Text(
                    com.example.lifeorganizer.notes.ui.markdownToPlainText(preview).lineSequence().first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Hosts the Waypoints module with working add / edit / delete / settings. */
@Composable
fun WaypointsHost(
    viewModel: WaypointsViewModel,
    route: WaypointRoute,
    onRouteChange: (WaypointRoute) -> Unit,
    onMenuClick: () -> Unit,
    onPlanEvent: (com.example.lifeorganizer.waypoints.data.WaypointWithLabels) -> Unit = {}
) {
    val waypoints by viewModel.waypoints.collectAsState()
    val allWaypoints by viewModel.allWaypoints.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val cardScale by viewModel.cardScale.collectAsState()
    val homeAddress by viewModel.homeAddress.collectAsState()
    val useHomeAsOrigin by viewModel.useHomeAsOrigin.collectAsState()
    val transportMode by viewModel.transportMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val hideAddresses by viewModel.hideAddresses.collectAsState()
    val themeStyle by viewModel.themeStyle.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val themeAccent by viewModel.themeAccent.collectAsState()

    when (route) {
        WaypointRoute.List -> HomeScreen(
            waypoints = waypoints,
            labels = labels,
            onAddClick = { onRouteChange(WaypointRoute.Add) },
            onSettingsClick = { onRouteChange(WaypointRoute.Settings) },
            onEditWaypoint = { id -> onRouteChange(WaypointRoute.Edit(id)) },
            onMenuClick = onMenuClick,
            onDeleteWaypoint = { viewModel.deleteWaypoint(it.waypoint) },
            onTogglePin = { viewModel.togglePin(it) },
            cardScale = cardScale,
            useHomeAsOrigin = useHomeAsOrigin,
            homeAddress = homeAddress,
            onToggleOrigin = { viewModel.updateUseHomeAsOrigin(it) },
            transportMode = transportMode,
            searchQuery = searchQuery,
            sortMode = sortMode,
            hideAddresses = hideAddresses,
            onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
            onSortModeChanged = { viewModel.updateSortMode(it) },
            onMarkUsed = { viewModel.markWaypointUsed(it.waypoint.id) },
            onPlanEvent = onPlanEvent
        )

        WaypointRoute.Add -> AddWaypointScreen(
            availableLabels = labels,
            onSave = { entity, selected ->
                viewModel.saveWaypoint(entity, selected)
                onRouteChange(WaypointRoute.List)
            },
            onBackClick = { onRouteChange(WaypointRoute.List) }
        )

        is WaypointRoute.Edit -> {
            val existing = allWaypoints.find { it.waypoint.id == route.id }
            if (existing == null) {
                // The list is still loading – wait instead of opening an empty form.
                Box(Modifier.fillMaxSize())
            } else {
                AddWaypointScreen(
                    availableLabels = labels,
                    initialWaypoint = existing.waypoint,
                    initialSelectedLabels = existing.labels,
                    onSave = { entity, selected ->
                        viewModel.saveWaypoint(entity, selected)
                        onRouteChange(WaypointRoute.List)
                    },
                    onBackClick = { onRouteChange(WaypointRoute.List) }
                )
            }
        }

        WaypointRoute.Settings -> SettingsScreen(
            currentThemeStyle = themeStyle,
            currentThemeMode = themeMode,
            currentThemeAccent = themeAccent,
            currentCardScale = cardScale,
            homeAddress = homeAddress,
            labels = labels,
            transportMode = transportMode,
            hideAddresses = hideAddresses,
            onThemeStyleChanged = { viewModel.updateThemeStyle(it) },
            onThemeModeChanged = { viewModel.updateThemeMode(it) },
            onThemeAccentChanged = { viewModel.updateThemeAccent(it) },
            onScaleChanged = { viewModel.updateCardScale(it) },
            onHomeAddressChanged = { viewModel.updateHomeAddress(it) },
            onTransportModeChanged = { viewModel.updateTransportMode(it) },
            onHideAddressesChanged = { viewModel.updateHideAddresses(it) },
            onCreateLabel = { name, color -> viewModel.createLabel(name, color) },
            onDeleteLabel = { viewModel.deleteLabel(it) },
            onImportData = { viewModel.importData(it) },
            onExportData = { viewModel.exportData(it) },
            onBackClick = { onRouteChange(WaypointRoute.List) },
            showAppearance = false
        )
    }
}

private const val DOCS_LIST = "documents"

/** Dokki needs a real NavHost – navigating on a bare NavController (as before) crashed when opening a document. */
@Composable
fun DocumentsHost(
    navController: NavHostController,
    viewModel: DocumentViewModel,
    onMenuClick: () -> Unit,
    openDocumentId: Long? = null,
    onDocumentOpened: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = DOCS_LIST) {
        composable(DOCS_LIST) {
            DocumentsScreen(navController = navController, viewModel = viewModel, onMenuClick = onMenuClick)
        }
        composable(Screen.Detail.route) { entry ->
            val id = entry.arguments?.getString("documentId")?.toLongOrNull() ?: return@composable
            DetailScreen(documentId = id, viewModel = viewModel, navController = navController)
        }
    }
    // Runs after NavHost has set the graph, so navigating is safe here (e.g. from universal search).
    LaunchedEffect(openDocumentId) {
        if (openDocumentId != null) {
            navController.navigate(Screen.Detail.createRoute(openDocumentId)) { launchSingleTop = true }
            onDocumentOpened()
        }
    }
}
