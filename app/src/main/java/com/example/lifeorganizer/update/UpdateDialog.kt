package com.example.lifeorganizer.update

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import kotlinx.coroutines.launch

/**
 * Update flow. With [known] the check already happened (start-up check), otherwise it runs now.
 */
@Composable
fun UpdateDialog(known: UpdateInfo? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(known == null) }
    var info by remember { mutableStateOf(known) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (known == null) {
            info = Updater.check()
            checking = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = {
            Text(
                when {
                    checking -> Str.checkUpdates.text()
                    info != null -> Str.updateAvailable.text().format(info!!.version)
                    else -> Str.upToDate.text()
                }
            )
        },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                when {
                    checking -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    info != null -> {
                        if (info!!.notes.isNotBlank()) Text(info!!.notes, style = MaterialTheme.typography.bodyMedium)
                        progress?.let { p ->
                            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                        }
                        if (failed) Text(Str.updateFailed.text(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                    else -> Text(Str.upToDateDesc.text())
                }
            }
        },
        confirmButton = {
            val i = info
            if (!checking && i != null) {
                Button(enabled = progress == null, onClick = {
                    failed = false
                    progress = 0f
                    scope.launch {
                        runCatching { Updater.download(context, i) { progress = it } }
                            .onSuccess { apk -> progress = null; Updater.install(context, apk); onDismiss() }
                            .onFailure { progress = null; failed = true }
                    }
                }) { Text(Str.downloadInstall.text()) }
            } else if (!checking) {
                TextButton(onClick = onDismiss) { Text(Str.done.text()) }
            }
        },
        dismissButton = {
            if (!checking && info != null && progress == null) TextButton(onClick = onDismiss) { Text(Str.later.text()) }
        }
    )
}

/** At most one silent check per day on start; returns a newer release if there is one. */
suspend fun dailyUpdateCheck(context: Context): UpdateInfo? {
    val prefs = context.getSharedPreferences("updater", Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    if (now - prefs.getLong("last_check", 0) < 24 * 3_600_000L) return null
    prefs.edit().putLong("last_check", now).apply()
    return Updater.check()
}
