package com.example.lifeorganizer.releases.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.core.settings.rememberMicro
import com.example.lifeorganizer.releases.api.SearchHit
import com.example.lifeorganizer.releases.data.ReleaseRow
import com.example.lifeorganizer.releases.data.Source
import com.example.lifeorganizer.releases.data.WatchItem
import com.example.lifeorganizer.releases.work.ReleaseSync
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Tiers from best to worst, each with its badge colour. */
private val TIERS = listOf(
    "S" to Color(0xFFFF5A5F), "A" to Color(0xFFFF9F43), "B" to Color(0xFFFECA57),
    "C" to Color(0xFF1DD1A1), "D" to Color(0xFF54A0FF), "F" to Color(0xFF8395A7)
)

/** Search series, keep them on the radar; their release dates show in the calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesHost(onMenuClick: () -> Unit, vm: ReleasesViewModel = viewModel()) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle()
    val releases by vm.releases.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    val tiersOn = rememberMicro(MicroSettings.RELEASE_TIERS)
    val sort = rememberMicro(MicroSettings.RELEASE_SORT)
    var query by rememberSaveable { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
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
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ReleaseStr.onRadar.of(lang).uppercase() + if (items.isNotEmpty()) "  ${items.size}" else "",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        TextButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(sortLabel(sort, lang))
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf("next", "oldest", "name").forEach { key ->
                                DropdownMenuItem(
                                    text = { Text(sortLabel(key, lang)) },
                                    leadingIcon = { if (key == sort) Icon(Icons.Default.Check, null) },
                                    onClick = { MicroSettings.set(context, MicroSettings.RELEASE_SORT, key); sortMenu = false }
                                )
                            }
                        }
                    }
                }
            }
            if (offline) item(key = "offline") { Hint(ReleaseStr.offline.of(lang), error = true) }
            if (items.isEmpty()) item(key = "empty") { Hint(ReleaseStr.empty.of(lang)) }

            val now = System.currentTimeMillis()
            val nextOf = items.associate { item -> item.id to releases.firstOrNull { it.itemId == item.id && it.at > now } }
            val sorted = items.sortedWith(
                when (sort) {
                    // An old series from 1989 first; unknown start last
                    "oldest" -> compareBy<WatchItem> { it.premiered.ifBlank { "9999" } }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                    "name" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                    // Next episode soonest first; titles without one after them
                    else -> compareBy<WatchItem, Long?>(nullsLast()) { nextOf[it.id]?.at }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                }
            )
            val groups = if (tiersOn) {
                (TIERS.map { it.first } + listOf<String?>(null)).map { t -> t to sorted.filter { it.tier == t } }.filter { it.second.isNotEmpty() }
            } else listOf<Pair<String?, List<WatchItem>>>("all" to sorted)

            groups.forEach { (tier, list) ->
                if (tiersOn) item(key = "tier_$tier") { TierHeader(tier, list.size, lang) }
                items(list, key = { "item_${it.id}" }) { item ->
                    ItemCard(
                        item, nextOf[item.id], lang, tiersOn,
                        onTier = { vm.setTier(item, it) },
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
}

private fun sortLabel(key: String, lang: String) = when (key) {
    "oldest" -> ReleaseStr.sortOldest
    "name" -> ReleaseStr.sortName
    else -> ReleaseStr.sortNext
}.of(lang)

@Composable
private fun Hint(text: String, error: Boolean = false) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/** Poster, or the first letter on a tinted card while there is none. */
@Composable
private fun Poster(url: String?, title: String, width: Dp, height: Dp) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(width, height).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Text(title.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(width, height))
    }
}

@Composable
private fun HitRow(hit: SearchHit, known: Boolean, lang: String, onAdd: () -> Unit) {
    ListItem(
        leadingContent = { Poster(hit.imageUrl, hit.title, 40.dp, 56.dp) },
        headlineContent = { Text(hit.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(listOfNotNull(hit.year?.toString(), statusText(hit.status, lang), hit.format.takeIf { it.isNotBlank() }).joinToString(" · "))
        },
        trailingContent = {
            if (known) Icon(Icons.Default.Check, ReleaseStr.added.of(lang), tint = MaterialTheme.colorScheme.primary)
            else IconButton(onClick = onAdd) { Icon(Icons.Default.Add, ReleaseStr.add.of(lang)) }
        }
    )
}

@Composable
private fun TierHeader(tier: String?, count: Int, lang: String) {
    val color = TIERS.firstOrNull { it.first == tier }?.second
    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (tier != null && color != null) TierBadge(tier, color, 28.dp)
        else Text(ReleaseStr.noTier.of(lang), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TierBadge(tier: String, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(6.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(tier, color = Color(0xFF1B1B1F), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ItemCard(
    item: WatchItem, next: ReleaseRow?, lang: String, tiersOn: Boolean,
    onTier: (String?) -> Unit, onNotify: () -> Unit, onRemove: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var tierMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Poster(item.imageUrl, item.title, 56.dp, 80.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(item.premiered.take(4).takeIf { it.isNotBlank() }, statusText(item.status, lang)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                NextLine(next, lang)
            }
            if (tiersOn) Box {
                val color = TIERS.firstOrNull { it.first == item.tier }?.second
                if (item.tier != null && color != null) TierBadge(item.tier, color, 32.dp, Modifier.clickable { tierMenu = true })
                else Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { tierMenu = true },
                    contentAlignment = Alignment.Center
                ) { Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                DropdownMenu(expanded = tierMenu, onDismissRequest = { tierMenu = false }) {
                    TIERS.forEach { (t, c) ->
                        DropdownMenuItem(text = { Text(ReleaseStr.tier.of(lang) + " " + t) }, leadingIcon = { TierBadge(t, c, 24.dp) }, onClick = { onTier(t); tierMenu = false })
                    }
                    DropdownMenuItem(text = { Text(ReleaseStr.noTier.of(lang)) }, onClick = { onTier(null); tierMenu = false })
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, ReleaseStr.more.of(lang)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (item.notify) ReleaseStr.notifyOn.of(lang) else ReleaseStr.notifyOff.of(lang)) },
                        leadingIcon = { Icon(if (item.notify) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null) },
                        onClick = { onNotify(); menu = false }
                    )
                    DropdownMenuItem(
                        text = { Text(ReleaseStr.remove.of(lang)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { onRemove(); menu = false }
                    )
                }
            }
        }
    }
}

/** "Tomorrow 20:00 · Episode 5" highlighted, or a quiet "no more dates". */
@Composable
private fun NextLine(next: ReleaseRow?, lang: String) {
    if (next == null) {
        Text(ReleaseStr.noMore.of(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(4.dp))
        Text(
            whenText(next, lang), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** "Today 20:00", "Tomorrow 20:00", "Fri 20:00" within a week, else "30 Oct"; plus the episode. */
private fun whenText(row: ReleaseRow, lang: String): String {
    val time = Instant.ofEpochMilli(row.at).atZone(ZoneId.systemDefault())
    val locale = Locale.forLanguageTag(lang)
    val days = ChronoUnit.DAYS.between(LocalDate.now(), time.toLocalDate())
    val clock = DateTimeFormatter.ofPattern("HH:mm", locale).format(time)
    val day = when {
        days == 0L -> ReleaseStr.today.of(lang) + " " + clock
        days == 1L -> ReleaseStr.tomorrow.of(lang) + " " + clock
        days in 2..6 -> DateTimeFormatter.ofPattern("EEE", locale).format(time) + " " + clock
        else -> DateTimeFormatter.ofPattern("d. MMM", locale).format(time)
    }
    return if (row.label.isEmpty()) day else "$day · ${ReleaseSync.label(row.label, lang)}"
}

private fun statusText(status: String, lang: String): String? = when (status.uppercase()) {
    "RELEASING", "RUNNING" -> ReleaseStr.statusReleasing
    "FINISHED", "ENDED" -> ReleaseStr.statusFinished
    "NOT_YET_RELEASED", "IN DEVELOPMENT", "TO BE DETERMINED" -> ReleaseStr.statusUpcoming
    "HIATUS" -> ReleaseStr.statusPaused
    "CANCELLED" -> ReleaseStr.statusCancelled
    else -> null
}?.of(lang)
