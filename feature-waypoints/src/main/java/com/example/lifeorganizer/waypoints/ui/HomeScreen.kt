package com.example.lifeorganizer.waypoints.ui

import androidx.compose.material.icons.automirrored.filled.Sort
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.WpStr
import com.example.lifeorganizer.core.i18n.text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.lifeorganizer.waypoints.data.LabelEntity
import com.example.lifeorganizer.waypoints.data.WaypointWithLabels

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    waypoints: List<WaypointWithLabels>,
    labels: List<LabelEntity>,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditWaypoint: (Long) -> Unit,
    onMenuClick: () -> Unit = {},
    onDeleteWaypoint: (WaypointWithLabels) -> Unit,
    onTogglePin: (WaypointWithLabels) -> Unit,
    cardScale: Float,
    useHomeAsOrigin: Boolean,
    homeAddress: String,
    onToggleOrigin: (Boolean) -> Unit,
    transportMode: String,
    searchQuery: String,
    sortMode: String,
    hideAddresses: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSortModeChanged: (String) -> Unit,
    onMarkUsed: (WaypointWithLabels) -> Unit,
    onPlanEvent: ((WaypointWithLabels) -> Unit)? = null
) {
    var selectedLabelId by remember { mutableStateOf<Long?>(null) }
    var isMapView by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    var isSearchOverlayVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = isSearchOverlayVisible || searchQuery.isNotEmpty()) {
        if (isSearchOverlayVisible) {
            isSearchOverlayVisible = false
        } else if (searchQuery.isNotEmpty()) {
            onSearchQueryChanged("")
        }
    }
    
    val isImeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible) {
            wasImeVisible = false
            if (isSearchOverlayVisible) {
                isSearchOverlayVisible = false
            }
        }
    }
    
    val filteredWaypoints = remember(waypoints, selectedLabelId) {
        if (selectedLabelId == null) {
            waypoints
        } else {
            waypoints.filter { it.labels.any { label -> label.id == selectedLabelId } }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(WpStr.title.text()) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = Str.menu.text())
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(56.dp),
                actions = {
                    Spacer(modifier = Modifier.weight(1f))
                    // Sort Menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = WpStr.sort.text())
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(WpStr.sortRecent.text()) },
                                onClick = { 
                                    onSortModeChanged("Recent")
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(WpStr.sortName.text()) },
                                onClick = { 
                                    onSortModeChanged("Name")
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(WpStr.sortLastUsed.text()) },
                                onClick = { 
                                    onSortModeChanged("Last Used")
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(WpStr.sortLastEdited.text()) },
                                onClick = { 
                                    onSortModeChanged("Last Edited")
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Map Toggle
                    IconButton(onClick = { isMapView = !isMapView }) {
                        Icon(
                            if (isMapView) Icons.Filled.List else Icons.Filled.LocationOn,
                            contentDescription = WpStr.toggleView.text()
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Settings
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = Str.settings.text())
                    }
                    
                    Spacer(modifier = Modifier.width(72.dp))
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { isSearchOverlayVisible = true },
                    containerColor = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (searchQuery.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation()
                ) {
                    Icon(Icons.Filled.Search, contentDescription = WpStr.search.text())
                }

                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = WpStr.addWaypoint.text())
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Origin Toggle Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (useHomeAsOrigin) WpStr.fromHome.text() else WpStr.fromCurrent.text(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = useHomeAsOrigin,
                        onCheckedChange = onToggleOrigin
                    )
                }
            }

            if (labels.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedLabelId == null,
                            onClick = { selectedLabelId = null },
                            label = { Text(Str.all.text()) }
                        )
                    }
                    items(labels) { label ->
                        FilterChip(
                            selected = selectedLabelId == label.id,
                            onClick = { selectedLabelId = label.id },
                            label = { Text(label.name) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isMapView) {
                    MapContent(
                        waypoints = filteredWaypoints,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (filteredWaypoints.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(WpStr.noWaypoints.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 8.dp) // removed large padding since fab is in appbar
                        ) {
                            items(filteredWaypoints, key = { it.waypoint.id }) { waypointWithLabels ->
                                WaypointCard(
                                    waypointWithLabels = waypointWithLabels,
                                    onEdit = { onEditWaypoint(waypointWithLabels.waypoint.id) },
                                    onDelete = { onDeleteWaypoint(waypointWithLabels) },
                                    onPlanEvent = onPlanEvent?.let { plan -> { plan(waypointWithLabels) } },
                                    onTogglePin = { onTogglePin(waypointWithLabels) },
                                    onMarkUsed = { onMarkUsed(waypointWithLabels) },
                                    cardScale = cardScale,
                                    useHomeAsOrigin = useHomeAsOrigin,
                                    homeAddress = homeAddress,
                                    transportMode = transportMode,
                                    hideAddress = hideAddresses
                                )
                            }
                        }
                    }
                }
                
                if (isSearchOverlayVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isSearchOverlayVisible = false
                            },
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        val focusRequester = remember { FocusRequester() }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = { Text(WpStr.searchWaypoints.text()) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChanged("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = Str.clear.text())
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    isFirst: Boolean
) {
    val shape = if (isFirst) {
        androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    } else {
        androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    }
    
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(end = if (isFirst) 2.dp else 0.dp)
    ) {
        Text(text)
    }
}
