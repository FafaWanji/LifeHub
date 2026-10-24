package com.example.lifeorganizer.releases.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeorganizer.releases.api.ReleaseApi
import com.example.lifeorganizer.releases.api.SearchHit
import com.example.lifeorganizer.releases.data.ReleaseDatabase
import com.example.lifeorganizer.releases.data.ReleaseRow
import com.example.lifeorganizer.releases.data.Source
import com.example.lifeorganizer.releases.data.WatchItem
import com.example.lifeorganizer.releases.work.ReleaseSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Search results: still loading, found (maybe nothing) or the source was not reachable. */
sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Done(val hits: List<SearchHit>) : SearchState
    data object Failed : SearchState
}

class ReleasesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val dao = ReleaseDatabase.get(application).dao()

    val items: StateFlow<List<WatchItem>> = dao.items().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val releases: StateFlow<List<ReleaseRow>> = dao.releases().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val search = MutableStateFlow<SearchState>(SearchState.Idle)
    val refreshing = MutableStateFlow(false)
    /** The last update could not reach a source. */
    val offline = MutableStateFlow(false)
    private var searchJob: Job? = null

    fun search(source: Source, query: String) {
        if (query.isBlank()) { search.value = SearchState.Idle; return }
        searchJob?.cancel()
        search.value = SearchState.Loading
        searchJob = viewModelScope.launch {
            search.value = withContext(Dispatchers.IO) {
                runCatching { SearchState.Done(ReleaseApi.search(source, query.trim())) }.getOrDefault(SearchState.Failed)
            }
        }
    }

    fun clearSearch() { searchJob?.cancel(); search.value = SearchState.Idle }

    fun add(hit: SearchHit) = viewModelScope.launch(Dispatchers.IO) {
        val id = dao.insert(WatchItem(source = hit.source, sourceId = hit.id, title = hit.title, format = hit.format, status = hit.status, premiered = hit.premiered, imageUrl = hit.imageUrl))
        if (id <= 0) return@launch
        offline.value = !ReleaseSync.refresh(app, dao.find(hit.source, hit.id) ?: return@launch)
        ReleaseSync.schedule(app)
        ReleaseSync.announce(app)
    }

    fun remove(item: WatchItem) = viewModelScope.launch(Dispatchers.IO) { dao.delete(item) }

    /** Undo after a remove: back with its id; its dates come again with an update. */
    fun restore(item: WatchItem) = viewModelScope.launch(Dispatchers.IO) {
        dao.insert(item)
        ReleaseSync.refresh(app, item)
    }

    fun setNotify(item: WatchItem, on: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        dao.update(item.copy(notify = on))
        ReleaseSync.announce(app)
    }

    /** null takes the title out of the tier list. */
    fun setTier(item: WatchItem, tier: String?) = viewModelScope.launch(Dispatchers.IO) { dao.update(item.copy(tier = tier)) }

    fun refreshAll() = viewModelScope.launch(Dispatchers.IO) {
        refreshing.value = true
        offline.value = !ReleaseSync.refreshAll(app)
        refreshing.value = false
    }
}
