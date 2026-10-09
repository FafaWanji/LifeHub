package com.example.lifeorganizer.web

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
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
    BackHandler(onBack = onBack)
    // Pairings happen in the browser: refresh the list while the screen is open
    LaunchedEffect(Unit) {
        while (true) {
            browsers = store.load()
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
                Switch(checked = state.running, onCheckedChange = { on ->
                    if (on) WebAccessService.start(context) else WebAccessService.stop(context)
                })
            }
            Text(WebStr.intro.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.failed) Text(WebStr.startFailed.text(), color = MaterialTheme.colorScheme.error)

            if (state.running) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(WebStr.address.text(), style = MaterialTheme.typography.labelLarge)
                        SelectionContainer {
                            Text(state.address ?: WebStr.noWifi.text(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(WebStr.code.text(), style = MaterialTheme.typography.labelLarge)
                        Text(state.code.chunked(3).joinToString(" "), fontSize = 40.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(WebStr.codeHint.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { WebAccessService.newCode(context) }) { Text(WebStr.newCode.text()) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.tertiary)
                Text(WebStr.security.text(), style = MaterialTheme.typography.bodySmall)
            }

            Text(WebStr.browsers.text(), style = MaterialTheme.typography.titleSmall)
            if (browsers.isEmpty()) Text(WebStr.noBrowsers.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            browsers.forEach { b ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.name)
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
