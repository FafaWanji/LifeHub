package com.example.lifeorganizer.waypoints.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeorganizer.waypoints.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.io.InputStreamReader

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.waypointDao()
    private val settingsRepo = SettingsRepository(application)

    init {
        viewModelScope.launch { settingsRepo.migrateLegacyNavigationSettings() }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow("Recent") // Options: "Recent", "Name", "Last Used", "Last Edited"
    val sortMode: StateFlow<String> = _sortMode.asStateFlow()

    val waypoints: StateFlow<List<WaypointWithLabels>> = combine(
        dao.getAllWaypointsWithLabels(),
        _searchQuery,
        _sortMode
    ) { list, query, sort ->
        var result = list
        if (query.isNotBlank()) {
            result = result.filter { 
                it.waypoint.name.contains(query, ignoreCase = true) || 
                it.waypoint.address.contains(query, ignoreCase = true)
            }
        }
        when (sort) {
            "Alphabetical", "Name" -> result = result.sortedWith(compareByDescending<WaypointWithLabels> { it.waypoint.isPinned }.thenBy { it.waypoint.name.lowercase() })
            "Recent" -> result = result.sortedWith(compareByDescending<WaypointWithLabels> { it.waypoint.isPinned }.thenByDescending { it.waypoint.id })
            "Last Used" -> result = result.sortedWith(compareByDescending<WaypointWithLabels> { it.waypoint.isPinned }.thenByDescending { it.waypoint.lastUsed })
            "Last Edited" -> result = result.sortedWith(compareByDescending<WaypointWithLabels> { it.waypoint.isPinned }.thenByDescending { it.waypoint.lastEdited })
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Every waypoint, ignoring this screen's search/sort (used by universal search). */
    val allWaypoints: StateFlow<List<WaypointWithLabels>> = dao.getAllWaypointsWithLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labels: StateFlow<List<LabelEntity>> = dao.getAllLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val themeStyle: StateFlow<String> = settingsRepo.themeStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "pastel")

    val themeMode: StateFlow<String> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "light")

    val themeAccent: StateFlow<String> = settingsRepo.themeAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "blush_pink")

    val cardScale: StateFlow<Float> = settingsRepo.cardScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
        
    val homeAddress: StateFlow<String> = settingsRepo.homeAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        
    val useHomeAsOrigin: StateFlow<Boolean> = settingsRepo.useHomeAsOrigin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val transportMode: StateFlow<String> = settingsRepo.transportMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "car")
        
    val hideAddresses: StateFlow<Boolean> = settingsRepo.hideAddresses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)



    fun saveWaypoint(waypoint: WaypointEntity, selectedLabels: List<LabelEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val entityToSave = if (waypoint.id == 0L) {
                waypoint.copy(lastEdited = now, lastUsed = now)
            } else {
                waypoint.copy(lastEdited = now)
            }
            
            val waypointId = dao.insertWaypoint(entityToSave)
            // Real id is either the old one or the newly generated one
            val finalId = if (entityToSave.id == 0L) waypointId else entityToSave.id
            
            // Re-create relations
            dao.deleteAllLabelsForWaypoint(finalId)
            for (label in selectedLabels) {
                dao.insertWaypointLabelCrossRef(WaypointLabelCrossRef(finalId, label.id))
            }
        }
    }

    fun markWaypointUsed(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateLastUsed(id, System.currentTimeMillis())
        }
    }

    fun deleteWaypoint(waypoint: WaypointEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteWaypoint(waypoint)
            dao.deleteAllLabelsForWaypoint(waypoint.id)
        }
    }
    
    fun togglePin(waypointWithLabels: WaypointWithLabels) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateWaypoint(waypointWithLabels.waypoint.copy(isPinned = !waypointWithLabels.waypoint.isPinned))
        }
    }
    
    suspend fun getWaypoint(id: Long): WaypointEntity? {
        return withContext(Dispatchers.IO) {
            dao.getWaypointById(id)
        }
    }

    fun updateThemeStyle(style: String) {
        viewModelScope.launch {
            settingsRepo.saveThemeStyle(style)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepo.saveThemeMode(mode)
        }
    }

    fun updateThemeAccent(accent: String) {
        viewModelScope.launch {
            settingsRepo.saveThemeAccent(accent)
        }
    }

    fun updateCardScale(scale: Float) {
        viewModelScope.launch {
            settingsRepo.saveCardScale(scale)
        }
    }
    
    fun updateHomeAddress(address: String) {
        viewModelScope.launch {
            settingsRepo.saveHomeAddress(address)
        }
    }
    
    fun updateUseHomeAsOrigin(useHome: Boolean) {
        viewModelScope.launch {
            settingsRepo.saveUseHomeAsOrigin(useHome)
        }
    }
    
    fun updateTransportMode(mode: String) {
        viewModelScope.launch {
            settingsRepo.saveTransportMode(mode)
        }
    }
    
    fun updateHideAddresses(hide: Boolean) {
        viewModelScope.launch {
            settingsRepo.saveHideAddresses(hide)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortMode(mode: String) {
        _sortMode.value = mode
    }
    
    fun createLabel(name: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertLabel(LabelEntity(name = name, colorHex = colorHex))
        }
    }
    
    fun deleteLabel(label: LabelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteLabel(label)
        }
    }
    
    // Import/Export
    data class BackupData(
        val waypoints: List<WaypointEntity>,
        val labels: List<LabelEntity>,
        val crossRefs: List<WaypointLabelCrossRef>
    )

    fun exportData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val waypointsWithLabels = dao.getAllWaypointsWithLabelsSync()
                val labels = dao.getAllLabelsSync()
                
                val waypoints = waypointsWithLabels.map { it.waypoint }
                val crossRefs = mutableListOf<WaypointLabelCrossRef>()
                for (w in waypointsWithLabels) {
                    for (l in w.labels) {
                        crossRefs.add(WaypointLabelCrossRef(w.waypoint.id, l.id))
                    }
                }
                
                val backup = BackupData(waypoints, labels, crossRefs)
                val json = Gson().toJson(backup)
                
                val outputStream = getApplication<Application>().contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val writer = OutputStreamWriter(outputStream)
                    writer.write(json)
                    writer.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val reader = InputStreamReader(inputStream)
                    val type = object : TypeToken<BackupData>() {}.type
                    val backup: BackupData = Gson().fromJson(reader, type)
                    reader.close()
                    
                    // Clear existing database and insert new data
                    for (waypoint in backup.waypoints) {
                        dao.insertWaypoint(waypoint)
                    }
                    for (label in backup.labels) {
                        dao.insertLabel(label)
                    }
                    for (ref in backup.crossRefs) {
                        dao.insertWaypointLabelCrossRef(ref)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
