package com.example.lifeorganizer.releases.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.releases.api.SearchHit
import com.example.lifeorganizer.releases.data.ReleaseRow
import com.example.lifeorganizer.releases.data.Source
import com.example.lifeorganizer.releases.data.WatchItem
import com.example.lifeorganizer.releases.work.ReleaseSync
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Search anime and series, keep them on the radar; their release dates show in the calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesHost(onMenuClick: () -> Unit, vm: ReleasesViewModel = viewModel()) {
    val lang = LocalAppLanguage.current
    val items by vm.items.collectAsStateWithLifecycle()
    val releases by vm.releases.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Dates older than half a day count as stale: update when the screen opens
    LaunchedEffect(Unit) {
        if (items.any { System.currentTimeMillis() - it.lastSync > 12L * 60 * 60 * 1000 }) vm.refreshAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Str.releaseRadar.text()) },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, Str.menu.text()) } },
                actions = {
                    if (refreshing) CircularProgressIndicator(Modifier.size(24.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = { vm.refreshAll() }, enabled = items.isNotEmpty()) { Icon(Icons.Default.Refresh, ReleaseStr.refresh.text()) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "search") {
                // TVmaze has a whole anime as one series, not every season on its own like AniList
                val run = { vm.search(Source.TVMAZE, query) }
                OutlinedTextField(
                    value = query, onValueChange = { query = it; if (it.isBlank()) vm.clearSearch() }, singleLine = true,
                    placeholder = { Text(ReleaseStr.searchHint.of(lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = ""; vm.clearSearch() }) { Icon(Icons.Default.Close, Str.cancel.text()) }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { run() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            when (val s = search) {
                SearchState.Idle -> Unit
                SearchState.Loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                SearchState.Failed -> item(key = "failed") { Hint(ReleaseStr.offline.of(lang), error = true) }
                is SearchState.Done -> {
                    if (s.hits.isEmpty()) item(key = "none") { Hint(ReleaseStr.noResults.of(lang)) }
                    items(s.hits, key = { "hit_${it.source}_${it.id}" }) { hit ->
                        val known = items.any { it.source == hit.source && it.sourceId == hit.id }
                        HitRow(hit, known, lang) { vm.add(hit) }
                    }
                }
            }
            item(key = "radarHeader") {
                Text(
                    ReleaseStr.onRadar.of(lang).uppercase(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            if (offline) item(key = "offline") { Hint(ReleaseStr.offline.of(lang), error = true) }
            if (items.isEmpty()) item(key = "empty") { Hint(ReleaseStr.empty.of(lang)) }
            val now = System.currentTimeMillis()
            // Soonest next release first; titles without one after them
            val nextOf = items.associate { item -> item.id to releases.firstOrNull { it.itemId == item.id && it.at > now } }
            items(items.sortedWith(compareBy(nullsLast()) { nextOf[it.id]?.at }), key = { "item_${it.id}" }) { item ->
                ItemRow(
                    item, nextOf[item.id], lang,
                    onNotify = { vm.setNotify(item, !item.notify) },
                    onRemove = {
                        vm.remove(item)
                        scope.launch {
                            val r = snackbar.showSnackbar(ReleaseStr.removed.of(lang).format(item.title), ReleaseStr.undo.of(lang), duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) vm.restore(item)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String, error: Boolean = false) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun HitRow(hit: SearchHit, known: Boolean, lang: String, onAdd: () -> Unit) {
    ListItem(
        headlineContent = { Text(hit.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(listOfNotNull(hit.format.takeIf { it.isNotBlank() }, hit.year?.toString(), statusText(hit.status, lang)).joinToString(" · "))
        },
        trailingContent = {
            if (known) Icon(Icons.Default.Check, ReleaseStr.added.of(lang), tint = MaterialTheme.colorScheme.primary)
            else IconButton(onClick = onAdd) { Icon(Icons.Default.Add, ReleaseStr.add.of(lang)) }
        }
    )
}

@Composable
private fun ItemRow(item: WatchItem, next: ReleaseRow?, lang: String, onNotify: () -> Unit, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val line = if (next != null) ReleaseStr.next.of(lang).format(whenText(next, lang)) else ReleaseStr.noMore.of(lang)
            Text(listOfNotNull(line, statusText(item.status, lang)).joinToString(" · "))
        },
        trailingContent = {
            Row {
                IconButton(onClick = onNotify) {
                    Icon(
                        if (item.notify) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                        if (item.notify) ReleaseStr.notifyOn.of(lang) else ReleaseStr.notifyOff.of(lang),
                        tint = if (item.notify) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, ReleaseStr.remove.of(lang)) }
            }
        }
    )
}

/** "Fri, 31 Oct, 17:30 · Episode 6". */
private fun whenText(row: ReleaseRow, lang: String): String {
    val time = Instant.ofEpochMilli(row.at).atZone(ZoneId.systemDefault())
    val date = DateTimeFormatter.ofPattern("EEE, d. MMM, HH:mm", Locale.forLanguageTag(lang)).format(time)
    return if (row.label.isEmpty()) date else "$date · ${ReleaseSync.label(row.label, lang)}"
}

private fun statusText(status: String, lang: String): String? = when (status.uppercase()) {
    "RELEASING", "RUNNING" -> ReleaseStr.statusReleasing
    "FINISHED", "ENDED" -> ReleaseStr.statusFinished
    "NOT_YET_RELEASED", "IN DEVELOPMENT", "TO BE DETERMINED" -> ReleaseStr.statusUpcoming
    "HIATUS" -> ReleaseStr.statusPaused
    "CANCELLED" -> ReleaseStr.statusCancelled
    else -> null
}?.of(lang)
