package com.example.lifeorganizer.waypoints

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lifeorganizer.waypoints.data.WaypointEntity
import com.example.lifeorganizer.waypoints.ui.AddWaypointScreen
import com.example.lifeorganizer.waypoints.ui.HomeScreen
import com.example.lifeorganizer.waypoints.ui.MainViewModel
import com.example.lifeorganizer.waypoints.ui.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    
    val waypoints by viewModel.waypoints.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val themeStyle by viewModel.themeStyle.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val themeAccent by viewModel.themeAccent.collectAsState()
    val cardScale by viewModel.cardScale.collectAsState()
    val homeAddress by viewModel.homeAddress.collectAsState()
    val useHomeAsOrigin by viewModel.useHomeAsOrigin.collectAsState()
    val transportMode by viewModel.transportMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val hideAddresses by viewModel.hideAddresses.collectAsState()
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                waypoints = waypoints,
                labels = labels,
                onAddClick = { navController.navigate("add") },
                onSettingsClick = { navController.navigate("settings") },
                onEditWaypoint = { id -> navController.navigate("edit/$id") },
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
                onMarkUsed = { viewModel.markWaypointUsed(it.waypoint.id) }
            )
        }
        
        composable("add") {
            AddWaypointScreen(
                availableLabels = labels,
                onSave = { entity, selectedLabels ->
                    viewModel.saveWaypoint(entity, selectedLabels)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("edit/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            // We need to fetch the existing entity to populate the edit screen
            // Since we're in compose, we can just find it from the list
            val existingWaypoint = waypoints.find { it.waypoint.id == id }?.waypoint
            val existingLabels = waypoints.find { it.waypoint.id == id }?.labels ?: emptyList()
            
            AddWaypointScreen(
                availableLabels = labels,
                initialWaypoint = existingWaypoint,
                initialSelectedLabels = existingLabels,
                onSave = { entity, selectedLabels ->
                    viewModel.saveWaypoint(entity, selectedLabels)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
