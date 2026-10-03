package com.example.lifeorganizer.waypoints.ui

import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.WpStr
import com.example.lifeorganizer.core.i18n.text
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lifeorganizer.waypoints.data.LabelEntity
import com.example.lifeorganizer.waypoints.data.WaypointEntity
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWaypointScreen(
    availableLabels: List<LabelEntity>,
    onSave: (WaypointEntity, List<LabelEntity>) -> Unit,
    onBackClick: () -> Unit,
    initialWaypoint: WaypointEntity? = null,
    initialSelectedLabels: List<LabelEntity> = emptyList()
) {
    var name by remember { mutableStateOf(initialWaypoint?.name ?: "") }
    var address by remember { mutableStateOf(initialWaypoint?.address ?: "") }
    var notes by remember { mutableStateOf(initialWaypoint?.notes ?: "") }
    
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showPredictions by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lang = com.example.lifeorganizer.core.i18n.LocalAppLanguage.current
    // Without an API key Places is not initialised; address autocomplete is then simply unavailable.
    val placesClient: PlacesClient? = remember { if (Places.isInitialized()) Places.createClient(context) else null }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()
    
    var selectedLabelIds by remember { mutableStateOf(initialSelectedLabels.map { it.id }.toSet()) }
    
    // Coordinates
    var lat by remember { mutableStateOf(initialWaypoint?.lat) }
    var lng by remember { mutableStateOf(initialWaypoint?.lng) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { loc ->
                    lat = loc.latitude
                    lng = loc.longitude
                    scope.launch {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        try {
                            @Suppress("DEPRECATION")
                            val addresses = withContext(Dispatchers.IO) {
                                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                            }
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                address = buildString {
                                    for (i in 0..addr.maxAddressLineIndex) {
                                        append(addr.getAddressLine(i))
                                        if (i < addr.maxAddressLineIndex) append(", ")
                                    }
                                }
                            } else {
                                address = "${loc.latitude}, ${loc.longitude}"
                            }
                        } catch (e: Exception) {
                            address = "${loc.latitude}, ${loc.longitude}"
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(address) {
        if (address.length > 2 && showPredictions && placesClient != null) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(address)
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
                title = { Text(if (initialWaypoint == null) WpStr.addWaypoint.text() else WpStr.editWaypoint.text()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = Str.back.text())
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(WpStr.customName.text()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { 
                    address = it
                    showPredictions = true
                },
                label = { Text(WpStr.address.text()) },
                modifier = Modifier.fillMaxWidth()
            )
            
            if (showPredictions && predictions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn {
                        items(predictions) { prediction ->
                            Text(
                                text = prediction.getFullText(null).toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        address = prediction.getFullText(null).toString()
                                        showPredictions = false
                                        // Fetch coordinates if needed
                                        val placeRequest = FetchPlaceRequest.builder(
                                            prediction.placeId, 
                                            listOf(Place.Field.LAT_LNG)
                                        ).build()
                                        
                                        placesClient?.fetchPlace(placeRequest)?.addOnSuccessListener { response ->
                                            lat = response.place.latLng?.latitude
                                            lng = response.place.latLng?.longitude
                                        }
                                    }
                                    .padding(16.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(WpStr.notesOptional.text()) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(Str.labels.text(), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (availableLabels.isEmpty()) {
                Text(WpStr.noLabelsAvailable.text(), style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableLabels) { label ->
                        FilterChip(
                            selected = selectedLabelIds.contains(label.id),
                            onClick = {
                                selectedLabelIds = if (selectedLabelIds.contains(label.id)) {
                                    selectedLabelIds - label.id
                                } else {
                                    selectedLabelIds + label.id
                                }
                            },
                            label = { Text(label.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
                        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                        
                        if (!isGpsEnabled && !isNetworkEnabled) {
                            Toast.makeText(context, WpStr.enableLocation.of(lang), Toast.LENGTH_LONG).show()
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            return@OutlinedButton
                        }

                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            @SuppressLint("MissingPermission")
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let { loc ->
                                    lat = loc.latitude
                                    lng = loc.longitude
                                    scope.launch {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        try {
                                            @Suppress("DEPRECATION")
                                            val addresses = withContext(Dispatchers.IO) {
                                                geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                            }
                                            if (!addresses.isNullOrEmpty()) {
                                                val addr = addresses[0]
                                                address = buildString {
                                                    for (i in 0..addr.maxAddressLineIndex) {
                                                        append(addr.getAddressLine(i))
                                                        if (i < addr.maxAddressLineIndex) append(", ")
                                                    }
                                                }
                                            } else {
                                                address = "${loc.latitude}, ${loc.longitude}"
                                            }
                                        } catch (e: Exception) {
                                            address = "${loc.latitude}, ${loc.longitude}"
                                        }
                                    }
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(WpStr.useCurrentLocation.text())
                }

                Button(
                    onClick = {
                        val entity = WaypointEntity(
                            id = initialWaypoint?.id ?: 0,
                            name = name,
                            address = address,
                            notes = notes,
                            lat = lat,
                            lng = lng,
                            isPinned = initialWaypoint?.isPinned ?: false
                        )
                        val selectedLabels = availableLabels.filter { selectedLabelIds.contains(it.id) }
                        onSave(entity, selectedLabels)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && address.isNotBlank()
                ) {
                    Text(Str.save.text())
                }
            }
        }
    }
}
