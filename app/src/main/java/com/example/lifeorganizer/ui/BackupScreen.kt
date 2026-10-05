package com.example.lifeorganizer.ui

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.backup.AutoBackup
import com.example.lifeorganizer.backup.BackupManager
import com.example.lifeorganizer.backup.ImportSource
import com.example.lifeorganizer.backup.ImportSummary
import com.example.lifeorganizer.core.i18n.BkStr
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import kotlinx.coroutines.launch
import java.time.LocalDate

private sealed interface BackupResult {
    data class Exported(val text: String) : BackupResult
    data class Imported(val summary: ImportSummary) : BackupResult
    data class Failed(val message: String) : BackupResult
}

/**
 * One place for moving data in and out: a full LifeOrganizer backup plus importers for the
 * apps it replaced (PrivateCalendar2, Waypoints IRL, LifeBase, Checklist).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onClose: () -> Unit,
    onOpenDokki: () -> Unit,
    pendingImport: Uri? = null,
    onPendingImportHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val manager = remember { BackupManager(context) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<BackupResult?>(null) }

    BackHandler(onBack = onClose)

    fun run(block: suspend () -> BackupResult) {
        busy = true
        scope.launch {
            result = runCatching { block() }.getOrElse { BackupResult.Failed(it.localizedMessage ?: it.toString()) }
            busy = false
        }
    }

    fun importUri(uri: Uri) = run { BackupResult.Imported(manager.import(uri)) }

    // Files shared to / opened with LifeOrganizer (e.g. "Export" in the old calendar → LifeOrganizer).
    LaunchedEffect(pendingImport) {
        pendingImport?.let {
            importUri(it)
            onPendingImportHandled()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) run {
            val s = manager.export(uri)
            BackupResult.Exported(
                "${BkStr.events.of(lang)}: ${s.events} · ${BkStr.notes.of(lang)}: ${s.notes} · " +
                    "${BkStr.waypoints.of(lang)}: ${s.waypoints} · ${BkStr.documents.of(lang)}: ${s.documents}"
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importUri(uri)
    }
    val pickFile = { importLauncher.launch(arrayOf("application/json", "text/calendar", "text/*", "application/octet-stream", "*/*")) }
    val paste = {
        val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        if (text.isNullOrBlank()) result = BackupResult.Failed(BkStr.clipboardEmpty.of(lang))
        else run { BackupResult.Imported(manager.importText(text)) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(BkStr.title.text()) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, Str.back.text()) }
                }
            )
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Full backup
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Text(BkStr.fullBackup.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            BkStr.fullBackupDesc.text(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !busy, onClick = { exportLauncher.launch("lifehub-backup-${LocalDate.now()}.json") }) {
                                Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(BkStr.exportBtn.text())
                            }
                            OutlinedButton(
                                enabled = !busy,
                                onClick = pickFile,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.FileDownload, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(BkStr.importBtn.text())
                            }
                        }
                    }
                }

                // Automatic weekly backup
                AutoBackupCard(busy = busy, refreshKey = result) {
                    run { BackupResult.Exported(AutoBackup.runNow(context).absolutePath) }
                }

                // Old apps
                Text(BkStr.fromOldApps.text(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Text(BkStr.fromOldAppsDesc.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SourceCard(Icons.Default.CalendarMonth, "PrivateCalendar", BkStr.calendarHow.text()) {
                    FilledTonalButton(enabled = !busy, onClick = pickFile) { Text(BkStr.importBtn.text()) }
                }
                SourceCard(Icons.Default.Place, "Waypoints IRL", BkStr.waypointsHow.text()) {
                    FilledTonalButton(enabled = !busy, onClick = pickFile) { Text(BkStr.importBtn.text()) }
                }
                SourceCard(Icons.AutoMirrored.Filled.Notes, "LifeBase", BkStr.lifeBaseHow.text()) {
                    FilledTonalButton(enabled = !busy, onClick = paste) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(BkStr.pasteBtn.text())
                    }
                }
                SourceCard(Icons.Default.ChecklistRtl, "Checklist", BkStr.checklistHow.text()) {
                    FilledTonalButton(enabled = !busy, onClick = pickFile) { Text(BkStr.importBtn.text()) }
                    OutlinedButton(enabled = !busy, onClick = paste) { Text(BkStr.pasteBtn.text()) }
                }
                SourceCard(Icons.Default.Description, "DocPocket → ${Str.documents.text()}", BkStr.dokkiHow.text()) {
                    FilledTonalButton(onClick = onOpenDokki) { Text(BkStr.openDokki.text()) }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = {
                Text(
                    when (r) {
                        is BackupResult.Exported -> BkStr.exported.text()
                        is BackupResult.Imported -> if (r.summary.source == ImportSource.UNKNOWN) BkStr.failed.text() else BkStr.imported.text()
                        is BackupResult.Failed -> BkStr.failed.text()
                    }
                )
            },
            text = {
                Text(
                    when (r) {
                        is BackupResult.Exported -> r.text
                        is BackupResult.Failed -> r.message
                        is BackupResult.Imported -> describe(r.summary, lang)
                    }
                )
            },
            confirmButton = { TextButton(onClick = { result = null }) { Text(BkStr.ok.text()) } }
        )
    }
}

private fun describe(s: ImportSummary, lang: String): String {
    if (s.source == ImportSource.UNKNOWN) return BkStr.unknownFormat.of(lang)
    val source = when (s.source) {
        ImportSource.LIFEORGANIZER_BACKUP -> "LifeHub-Backup"
        ImportSource.ICS_CALENDAR -> "Kalender (.ics)"
        ImportSource.WAYPOINTS_IRL -> "Waypoints IRL"
        ImportSource.LIFEBASE -> "LifeBase"
        ImportSource.CHECKLIST -> "Checklist"
        ImportSource.UNKNOWN -> ""
    }
    return buildString {
        append("${BkStr.detected.of(lang)}: $source\n\n")
        if (s.events > 0) append("${BkStr.events.of(lang)}: ${s.events} ${BkStr.added.of(lang)}\n")
        if (s.notes > 0) append("${BkStr.notes.of(lang)}: ${s.notes} ${BkStr.added.of(lang)}\n")
        if (s.waypoints > 0) append("${BkStr.waypoints.of(lang)}: ${s.waypoints} ${BkStr.added.of(lang)}\n")
        if (s.documents > 0) append("${BkStr.documents.of(lang)}: ${s.documents} ${BkStr.added.of(lang)}\n")
        if (s.total == 0 && s.skipped == 0) append("0 ${BkStr.added.of(lang)}\n")
        if (s.skipped > 0) append("${s.skipped} ${BkStr.skippedDup.of(lang)}")
    }.trim()
}

@Composable
private fun SourceCard(
    icon: ImageVector,
    name: String,
    howTo: String,
    actions: @Composable RowScope.() -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                Spacer(Modifier.width(12.dp))
                Text(name, style = MaterialTheme.typography.titleSmall)
            }
            SelectionContainer {
                Text(
                    howTo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

@Composable
private fun AutoBackupCard(busy: Boolean, refreshKey: Any?, onRun: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(AutoBackup.isEnabled(context)) }
    var folder by remember { mutableStateOf(AutoBackup.externalFolder(context)) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { AutoBackup.setExternalFolder(context, uri) }
            folder = AutoBackup.externalFolder(context)
        }
    }
    // Re-read after every action so "last backup" is current.
    val latest = remember(refreshKey) { AutoBackup.latest(context) }
    val formatter = remember { java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(BkStr.autoBackup.text(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    AutoBackup.setEnabled(context, it)
                })
            }
            Text(
                BkStr.autoBackupDesc.text().format(remember { AutoBackup.folder(context).absolutePath.substringAfter("/0/") }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    latest?.let {
                        BkStr.lastBackup.text().format(
                            java.time.Instant.ofEpochMilli(it.lastModified()).atZone(java.time.ZoneId.systemDefault()).format(formatter)
                        )
                    } ?: BkStr.noBackupYet.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(enabled = !busy, onClick = onRun) { Text(BkStr.backupNow.text()) }
            }
            // Optional second copy outside the app (Drive, SD card …) – survives uninstalling
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    folder?.let { BkStr.copyFolder.text().format(android.net.Uri.decode(it.lastPathSegment.orEmpty()).substringAfterLast(':')) }
                        ?: BkStr.copyFolderNone.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (folder != null) {
                    TextButton(onClick = { AutoBackup.setExternalFolder(context, null); folder = null }) { Text(Str.delete.text()) }
                }
                TextButton(onClick = { folderPicker.launch(null) }) { Text(BkStr.chooseFolder.text()) }
            }
        }
    }
}
