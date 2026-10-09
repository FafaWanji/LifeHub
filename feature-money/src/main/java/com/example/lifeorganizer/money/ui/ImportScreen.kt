package com.example.lifeorganizer.money.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.util.Amounts
import com.example.lifeorganizer.money.domain.ColumnMapping

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(vm: MoneyViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val state by vm.importState.collectAsStateWithLifecycle()
    val ai by vm.aiEnabled.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val names = categories.associate { it.id to it.name }
    val close = { vm.resetImport(); onClose() }
    BackHandler(onBack = close)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.loadCsv { context.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(MoneyStr.importCsv.text()) },
            navigationIcon = { IconButton(onClick = close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, Str.back.text()) } }
        )
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val s = state) {
                ImportState.Idle -> {
                    Text(MoneyStr.importHint.text())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(MoneyStr.aiCategorize.text(), Modifier.weight(1f))
                        Switch(checked = ai, onCheckedChange = vm::setAiEnabled)
                    }
                    Text(MoneyStr.aiHint.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }) {
                        Text(MoneyStr.chooseFile.text())
                    }
                }
                ImportState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is ImportState.NeedsMapping -> MappingForm(s) { vm.applyMapping(s, it) }
                is ImportState.Preview -> {
                    if (s.skipped > 0) Text(MoneyStr.skippedRows.text().format(s.skipped), style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.weight(1f)) {
                        itemsIndexed(s.rows) { i, row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, maxLines = 1)
                                    Text(
                                        "${row.date} · ${names[s.categoryIds[i]].orEmpty()}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(Amounts.formatSigned(row.amountCents, lang))
                            }
                        }
                    }
                    Button(onClick = vm::confirmImport, modifier = Modifier.fillMaxWidth()) { Text(MoneyStr.importN.text().format(s.rows.size)) }
                }
                is ImportState.Done -> {
                    Text(MoneyStr.importDone.text().format(s.result.imported, s.result.duplicates, s.result.matchedRecurring))
                    Button(onClick = close) { Text(MoneyStr.done.text()) }
                }
                ImportState.Error -> {
                    Text(MoneyStr.importFailed.text(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = vm::resetImport) { Text(MoneyStr.tryAgain.text()) }
                }
            }
        }
    }
}

@Composable
private fun MappingForm(state: ImportState.NeedsMapping, onApply: (ColumnMapping) -> Unit) {
    var date by remember { mutableStateOf<Int?>(null) }
    var amount by remember { mutableStateOf<Int?>(null) }
    var payee by remember { mutableStateOf<Int?>(null) }
    var purpose by remember { mutableStateOf<Int?>(null) }
    Text(MoneyStr.mapColumns.text(), style = MaterialTheme.typography.titleMedium)
    ColumnPicker(MoneyStr.columnDate.text(), state, date, allowNone = false) { date = it }
    ColumnPicker(MoneyStr.columnAmount.text(), state, amount, allowNone = false) { amount = it }
    ColumnPicker(MoneyStr.columnPayee.text(), state, payee, allowNone = true) { payee = it }
    ColumnPicker(MoneyStr.columnPurpose.text(), state, purpose, allowNone = true) { purpose = it }
    Button(enabled = date != null && amount != null, onClick = {
        val d = date ?: return@Button
        onApply(ColumnMapping(date = d, amount = amount, payee = payee, purpose = purpose))
    }) {
        Text(MoneyStr.apply.text())
    }
}

@Composable
private fun ColumnPicker(label: String, state: ImportState.NeedsMapping, selected: Int?, allowNone: Boolean, onSelect: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    fun describe(i: Int) = "${state.header[i]} (${state.sample.getOrNull(i).orEmpty().take(24)})"
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${selected?.let(::describe) ?: MoneyStr.none.text()}", maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (allowNone) DropdownMenuItem(text = { Text(MoneyStr.none.text()) }, onClick = { onSelect(null); open = false })
            state.header.indices.forEach { i ->
                DropdownMenuItem(text = { Text(describe(i)) }, onClick = { onSelect(i); open = false })
            }
        }
    }
}
