package com.example.lifeorganizer.update

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.example.lifeorganizer.core.theme.AlertDialog
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
                        val notes = Updater.notesFor(info!!.notes, com.example.lifeorganizer.core.i18n.LocalAppLanguage.current)
                        if (notes.isNotBlank()) Text(notes, style = MaterialTheme.typography.bodyMedium)
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

/**
 * When the app checks GitHub on its own: on every start and whenever it comes back after
 * [IDLE_MS] in the background, but never more often than every [MIN_GAP_MS].
 * A release the user put off with "Later" stays quiet for [SNOOZE_MS].
 */
object AutoUpdateCheck {
    const val IDLE_MS = 10 * 60_000L
    const val MIN_GAP_MS = 10 * 60_000L
    const val SNOOZE_MS = 24 * 3_600_000L

    /** [backgroundedAt] is null on a fresh start. */
    fun shouldCheck(now: Long, lastCheck: Long, backgroundedAt: Long?): Boolean =
        (backgroundedAt == null || now - backgroundedAt >= IDLE_MS) && now - lastCheck >= MIN_GAP_MS

    fun shouldShow(version: String, snoozedVersion: String?, snoozedAt: Long, now: Long): Boolean =
        version != snoozedVersion || now - snoozedAt >= SNOOZE_MS

    private fun prefs(context: Context) = context.getSharedPreferences("updater", Context.MODE_PRIVATE)

    /** Returns a newer release worth showing, or null. */
    suspend fun run(context: Context, backgroundedAt: Long?): UpdateInfo? {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        if (!shouldCheck(now, p.getLong("last_check", 0), backgroundedAt)) return null
        p.edit().putLong("last_check", now).apply()
        val info = Updater.check() ?: return null
        return info.takeIf { shouldShow(it.version, p.getString("snoozed_version", null), p.getLong("snoozed_at", 0), now) }
    }

    fun snooze(context: Context, version: String) {
        prefs(context).edit().putString("snoozed_version", version).putLong("snoozed_at", System.currentTimeMillis()).apply()
    }
}
