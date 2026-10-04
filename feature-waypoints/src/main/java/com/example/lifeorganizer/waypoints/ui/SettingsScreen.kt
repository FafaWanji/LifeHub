package com.example.lifeorganizer.waypoints.ui

import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.WpStr
import com.example.lifeorganizer.core.i18n.text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.lifeorganizer.waypoints.data.LabelEntity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeStyle: String,
    currentThemeMode: String,
    currentThemeAccent: String,
    currentCardScale: Float,
    homeAddress: String,
    labels: List<LabelEntity>,
    transportMode: String,
    hideAddresses: Boolean,
    onThemeStyleChanged: (String) -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onThemeAccentChanged: (String) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onHomeAddressChanged: (String) -> Unit,
    onTransportModeChanged: (String) -> Unit,
    onHideAddressesChanged: (Boolean) -> Unit,
    onCreateLabel: (String, String) -> Unit,
    onDeleteLabel: (LabelEntity) -> Unit,
    onImportData: (Uri) -> Unit,
    onExportData: (Uri) -> Unit,
    onBackClick: () -> Unit,
    // Inside LifeOrganizer the global theme settings apply, so the legacy appearance card is hidden.
    showAppearance: Boolean = true
) {
    val pastelAccents = listOf(
        "blush_pink" to Color(0xFFF48FB1),
        "lavender" to Color(0xFFCE93D8),
        "mist_blue" to Color(0xFF90CAF9),
        "soft_mint" to Color(0xFF80CBC4),
        "peach_cream" to Color(0xFFFFCC80)
    )

    val neonAccents = listOf(
        "electric_mint" to Color(0xFF00FFB2),
        "neon_cyan" to Color(0xFF00E5FF),
        "acid_lime" to Color(0xFFD4FF00),
        "plasma_purple" to Color(0xFFD500F9),
        "hot_magenta" to Color(0xFFFF007F)
    )

    val pastelLabelColors = listOf(
        "#FFFFB3BA", // Pastel Pink
        "#FFFFDFBA", // Pastel Orange
        "#FFFFFFBA", // Pastel Yellow
        "#FFBAFFC9", // Pastel Green
        "#FFBAE1FF", // Pastel Blue
        "#FFE1BAFF", // Pastel Purple
        "#FFFFD1DC", // Pastel Rose
        "#FFC1E1C1", // Pastel Mint
        "#FFFDFD96", // Pastel Lemon
        "#FFCFCFC4"  // Pastel Grey
    )

    var addressInput by remember { mutableStateOf(homeAddress) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showPredictions by remember { mutableStateOf(false) }
    
    var newLabelName by remember { mutableStateOf("") }
    var newLabelColorHex by remember { mutableStateOf(pastelLabelColors.first()) }

    val context = LocalContext.current
    val placesClient: PlacesClient = remember { Places.createClient(context) }
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { onExportData(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportData(it) }
    }

    LaunchedEffect(addressInput) {
        if (addressInput.length > 2 && showPredictions) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(addressInput)
                .build()
            
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions = response.autocompletePredictions
            } catch (e: Exception) {
                predictions = emptyList()
            }
        } else {
            predictions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Str.settings.text()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = Str.back.text())
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Card
            if (showAppearance) item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(WpStr.appearance.text(), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(Str.designStyle.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentThemeStyle == "white_light",
                                onClick = { onThemeStyleChanged("white_light") },
                                label = { Text("White Light") }
                            )
                            FilterChip(
                                selected = currentThemeStyle == "midnight",
                                onClick = { onThemeStyleChanged("midnight") },
                                label = { Text("Midnight") }
                            )
                            FilterChip(
                                selected = currentThemeStyle == "pastel",
                                onClick = { onThemeStyleChanged("pastel") },
                                label = { Text("Pastel") }
                            )
                            FilterChip(
                                selected = currentThemeStyle == "neon",
                                onClick = { onThemeStyleChanged("neon") },
                                label = { Text("Neon") }
                            )
                        }

                        if (currentThemeStyle == "pastel") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(Str.mode.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentThemeMode == "light",
                                onClick = { onThemeModeChanged("light") },
                                label = { Text(Str.pastelLight.text()) }
                            )
                            FilterChip(
                                selected = currentThemeMode == "dark",
                                onClick = { onThemeModeChanged("dark") },
                                label = { Text(Str.pastelDark.text()) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(Str.accent.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(pastelAccents) { (accentId, color) ->
                                val isSelected = currentThemeAccent == accentId
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { onThemeAccentChanged(accentId) }
                                        .then(
                                            if (isSelected) Modifier.border(4.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                                             else Modifier
                                        )
                                )
                            }
                        }
                        }

                        if (currentThemeStyle == "neon") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(Str.neonAccent.text(), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(neonAccents) { (accentId, color) ->
                                    val isSelected = currentThemeAccent == accentId
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable { onThemeAccentChanged(accentId) }
                                            .then(
                                                if (isSelected) Modifier.border(4.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                                                else Modifier
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(WpStr.cardSize.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        var sliderPosition by remember { mutableStateOf(currentCardScale) }
                        Slider(
                            value = sliderPosition,
                            onValueChange = { 
                                sliderPosition = it
                                onScaleChanged(it)
                            },
                            valueRange = 0.8f..1.5f,
                            steps = 6
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(WpStr.smaller.text(), style = MaterialTheme.typography.bodySmall)
                            Text(WpStr.default.text(), style = MaterialTheme.typography.bodySmall)
                            Text(WpStr.larger.text(), style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(WpStr.hideAddresses.text(), style = MaterialTheme.typography.bodyMedium)
                                Text(WpStr.hideAddressesDesc.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            Switch(
                                checked = hideAddresses,
                                onCheckedChange = onHideAddressesChanged
                            )
                        }
                    }
                }
            }

            // Labels Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(Str.labels.text(), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newLabelName,
                                onValueChange = { newLabelName = it },
                                placeholder = { Text(WpStr.newLabelName.text()) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = { 
                                    if (newLabelName.isNotBlank()) {
                                        onCreateLabel(newLabelName, newLabelColorHex)
                                        newLabelName = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = WpStr.addLabel.text())
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(WpStr.labelColor.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(pastelLabelColors) { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = newLabelColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { newLabelColorHex = hex }
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                                            else Modifier
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (labels.isEmpty()) {
                            Text(WpStr.noLabelsCreated.text(), style = MaterialTheme.typography.bodySmall)
                        } else {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                labels.forEach { label ->
                                    InputChip(
                                        selected = false,
                                        onClick = { },
                                        label = { Text(label.name) },
                                        colors = InputChipDefaults.inputChipColors(
                                            containerColor = (runCatching { Color(android.graphics.Color.parseColor(label.colorHex)) }.getOrNull() ?: MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.9f)
                                        ),
                                        trailingIcon = {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = WpStr.deleteLabel.text(),
                                                modifier = Modifier.size(InputChipDefaults.AvatarSize).clickable {
                                                    onDeleteLabel(label)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(WpStr.navigation.text(), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(WpStr.homeAddress.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { 
                                addressInput = it
                                showPredictions = true
                            },
                            placeholder = { Text(WpStr.enterHomeAddress.text()) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        if (showPredictions && predictions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                LazyColumn {
                                    items(predictions) { prediction ->
                                        Text(
                                            text = prediction.getFullText(null).toString(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val selectedAddress = prediction.getFullText(null).toString()
                                                    addressInput = selectedAddress
                                                    showPredictions = false
                                                    onHomeAddressChanged(selectedAddress)
                                                }
                                                .padding(16.dp)
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(WpStr.transportMode.text(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var showTransportMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { showTransportMenu = true }, 
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                val modeStr = when(transportMode) {
                                    "car" -> WpStr.car.text()
                                    "bike" -> WpStr.bike.text()
                                    "transit" -> WpStr.transit.text()
                                    else -> WpStr.car.text()
                                }
                                Text(modeStr)
                            }
                            DropdownMenu(expanded = showTransportMenu, onDismissRequest = { showTransportMenu = false }) {
                                DropdownMenuItem(text = { Text(WpStr.car.text()) }, onClick = { onTransportModeChanged("car"); showTransportMenu = false })
                                DropdownMenuItem(text = { Text(WpStr.bike.text()) }, onClick = { onTransportModeChanged("bike"); showTransportMenu = false })
                                DropdownMenuItem(text = { Text(WpStr.transit.text()) }, onClick = { onTransportModeChanged("transit"); showTransportMenu = false })
                            }
                        }
                    }
                }
            }

            // Backup Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(WpStr.dataBackup.text(), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilledTonalButton(onClick = { exportLauncher.launch("waypoints_backup.json") }) {
                                Text(WpStr.export.text())
                            }
                            FilledTonalButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                                Text(WpStr.import.text())
                            }
                        }
                    }
                }
            }
        } // end LazyColumn
    } // end Scaffold
}
