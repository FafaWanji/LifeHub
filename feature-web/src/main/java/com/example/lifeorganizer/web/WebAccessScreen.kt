package com.example.lifeorganizer.web

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.RadioButton
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAccessScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by WebAccessService.state.collectAsStateWithLifecycle()
    val store = remember { PrefsTokenStore(context) }
    var browsers by remember { mutableStateOf(store.load()) }
    val lastSeen by WebAccessService.lastSeen.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    BackHandler(onBack = onBack)
    // Pairings happen in the browser: refresh the list while the screen is open
    LaunchedEffect(Unit) {
        while (true) {
            browsers = store.load()
            now = System.currentTimeMillis()
            delay(2_000)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(WebStr.title.text()) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, Str.back.text()) } }
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(WebStr.switchLabel.text(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = state.running || state.waiting != null, onCheckedChange = { on ->
                    if (on) WebAccessService.start(context) else WebAccessService.stop(context)
                })
            }
            Text(WebStr.intro.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.failed) Text(WebStr.startFailed.text(), color = MaterialTheme.colorScheme.error)
            state.waiting?.let { reason ->
                Text(
                    when (reason) {
                        WaitReason.WIFI -> WebStr.waitWifi
                        WaitReason.KNOWN_WIFI -> WebStr.waitKnownWifi
                        WaitReason.BATTERY -> WebStr.waitBattery
                    }.text(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (state.running) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(WebStr.address.text(), style = MaterialTheme.typography.labelLarge)
                        SelectionContainer {
                            Text(state.nameAddress ?: WebStr.noWifi.text(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        }
                        state.address?.let { ip ->
                            SelectionContainer {
                                Text(WebStr.fallback.text().format(ip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(WebStr.code.text(), style = MaterialTheme.typography.labelLarge)
                        Text(state.code.chunked(3).joinToString(" "), fontSize = 40.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(WebStr.codeHint.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { WebAccessService.newCode(context) }) { Text(WebStr.newCode.text()) }
                    }
                }
            }

            AutoStartSettings()

            AddressSettings(onSaved = { WebAccessService.restart(context) })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.tertiary)
                Text(WebStr.security.text(), style = MaterialTheme.typography.bodySmall)
            }

            Text(WebStr.browsers.text(), style = MaterialTheme.typography.titleSmall)
            if (browsers.isEmpty()) Text(WebStr.noBrowsers.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            browsers.forEach { b ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Green dot while this browser is asking the phone right now
                    val online = lastSeen[b.id]?.let { now - it < WebAccessService.ONLINE_MS } == true && state.running
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(b.name, Modifier.weight(1f, fill = false))
                            if (online) {
                                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                                androidx.compose.foundation.layout.Box(
                                    Modifier.size(8.dp).background(androidx.compose.ui.graphics.Color(0xFF43A047), androidx.compose.foundation.shape.CircleShape)
                                )
                                androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                                Text(WebStr.connectedNow.text(), style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF43A047))
                            }
                        }
                        Text(
                            WebStr.lastUsed.text().format(DateUtils.getRelativeTimeSpanString(b.lastUsed)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        store.save(store.load().filterNot { it.id == b.id })
                        browsers = store.load()
                    }) { Icon(Icons.Outlined.Close, WebStr.remove.text()) }
                }
            }
        }
    }
}

/** Name for <name>.local and the port; saving restarts a running server with the new address. */
@Composable
private fun AddressSettings(onSaved: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { WebSettings(context) }
    var name by remember { mutableStateOf(settings.name) }
    var port by remember { mutableStateOf(settings.port.toString()) }
    val portValue = port.toIntOrNull()
    val portOk = portValue != null && WebSettings.validPort(portValue)
    val changed = WebSettings.sanitizeName(name) != settings.name || portValue != settings.port

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(WebStr.settings.text(), style = MaterialTheme.typography.titleSmall)
        com.example.lifeorganizer.core.theme.InfoTip(com.example.lifeorganizer.core.theme.Tips.webAddress)
    }
    // Both fields on one line with the same height; the port range hint sits below them
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(40) }, label = { Text(WebStr.name.text()) },
            suffix = { Text(".local") }, singleLine = true, modifier = Modifier.weight(2f)
        )
        OutlinedTextField(
            value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, label = { Text(WebStr.port.text()) },
            isError = !portOk, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
        )
    }
    Text(
        "${WebStr.port.text()}: ${WebStr.portHint.text()}", style = MaterialTheme.typography.bodySmall,
        color = if (portOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
    )
    TextButton(enabled = changed && portOk, onClick = {
        settings.name = name
        settings.port = portValue!!
        name = settings.name
        onSaved()
    }) { Text(WebStr.save.text()) }
}

/** When PC access switches itself on, with an estimate of what it costs, and switching off on low battery. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AutoStartSettings() {
    val context = LocalContext.current
    val settings = remember { WebSettings(context) }
    var mode by remember { mutableStateOf(settings.autoStart) }
    var onlyKnown by remember { mutableStateOf(settings.onlyKnownWifi) }
    var known by remember { mutableStateOf(settings.knownNetworks) }
    var lowBattery by remember { mutableStateOf(settings.lowBatteryOff) }
    var notInWifi by remember { mutableStateOf(false) }
    var noName by remember { mutableStateOf(false) }

    // Saved under its name when readable; an entry for the same network (e.g. saved by router address) is merged in
    fun saveCurrent() {
        val ids = NetworkId.currentIds(context)
        val label = NetworkId.label(context)
        notInWifi = ids.isEmpty() || label == null
        if (notInWifi) return
        noName = NetworkId.ssid(context) == null
        val same = known.filterValues { it.any { id -> id in ids } }
        known = known - same.keys + (label!! to (ids + same.values.flatten()))
        settings.knownNetworks = known
    }
    val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { saveCurrent() }
    // Entries saved before the name was readable get it once the phone is in that network again
    LaunchedEffect(Unit) {
        val ssid = NetworkId.ssid(context) ?: return@LaunchedEffect
        val ids = NetworkId.currentIds(context)
        val same = known.filterValues { it.any { id -> id in ids } }
        if (same.isNotEmpty() && same.keys != setOf(ssid)) {
            known = known - same.keys + (ssid to (ids + same.values.flatten()))
            settings.knownNetworks = known
        }
    }

    Text(WebStr.autoTitle.text(), style = MaterialTheme.typography.titleSmall)
    Column {
        listOf(
            AutoStart.OFF to (WebStr.modeOff to WebStr.modeOffHint),
            AutoStart.APP_OPEN to (WebStr.modeAppOpen to WebStr.modeAppOpenHint),
            AutoStart.WIFI to (WebStr.modeWifi to WebStr.modeWifiHint),
            AutoStart.ALWAYS to (WebStr.modeAlways to WebStr.modeAlwaysHint)
        ).forEach { (m, texts) ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
                    .selectable(selected = mode == m, role = Role.RadioButton, onClick = {
                        mode = m
                        settings.autoStart = m
                        WebAutoStart.apply(context)
                    })
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = mode == m, onClick = null)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(texts.first.text())
                    Text(texts.second.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Which Wi-Fi: every one, or only remembered ones
            if (m == AutoStart.WIFI && mode == AutoStart.WIFI) {
                Column(Modifier.padding(start = 40.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !onlyKnown, onClick = { onlyKnown = false; settings.onlyKnownWifi = false }, label = { Text(WebStr.allWifi.text()) })
                        FilterChip(selected = onlyKnown, onClick = { onlyKnown = true; settings.onlyKnownWifi = true }, label = { Text(WebStr.knownWifi.text()) })
                    }
                    if (onlyKnown) {
                        if (known.isEmpty()) Text(WebStr.noKnownWifi.text(), style = MaterialTheme.typography.bodySmall)
                        known.keys.sorted().forEach { label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label, Modifier.weight(1f))
                                IconButton(onClick = {
                                    known = known - label
                                    settings.knownNetworks = known
                                }) { Icon(Icons.Outlined.Close, WebStr.remove.text()) }
                            }
                        }
                        TextButton(onClick = {
                            // The Wi-Fi name is only readable with location access
                            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (granted) saveCurrent() else askLocation.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }) { Text(WebStr.rememberWifi.text()) }
                        if (noName && !notInWifi) Text(WebStr.wifiNameHint.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (notInWifi) Text(WebStr.notInWifi.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    Text(WebStr.lowBattery.text(), style = MaterialTheme.typography.titleSmall)
    Text(WebStr.lowBatteryHint.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 10, 15, 20, 30).forEach { p ->
            FilterChip(
                selected = lowBattery == p,
                onClick = { lowBattery = p; settings.lowBatteryOff = p },
                label = { Text(if (p == 0) WebStr.modeOff.text() else "$p %") }
            )
        }
    }
}
