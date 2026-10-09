package com.example.lifeorganizer.money.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.util.Amounts
import com.example.lifeorganizer.money.data.CategoryKind
import com.example.lifeorganizer.money.data.Recurring
import com.example.lifeorganizer.money.domain.RecurringInterval
import com.example.lifeorganizer.money.domain.RecurringScheduler
import java.time.LocalDate
import kotlin.math.abs

fun newRecurring() = Recurring(title = "", amountCents = 0, dayOfMonth = LocalDate.now().dayOfMonth, startEpochDay = LocalDate.now().toEpochDay())

@Composable
private fun intervalLabel(i: RecurringInterval) = when (i) {
    RecurringInterval.MONTHLY -> MoneyStr.monthly.text()
    RecurringInterval.QUARTERLY -> MoneyStr.quarterly.text()
    RecurringInterval.YEARLY -> MoneyStr.yearly.text()
}

@Composable
fun RecurringScreen(vm: MoneyViewModel, onEdit: (Recurring) -> Unit) {
    val lang = LocalAppLanguage.current
    val list by vm.recurring.collectAsStateWithLifecycle()
    val perMonth = list.sumOf { RecurringScheduler.monthlyEquivalentCents(it.amountCents, it.interval) }
    LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text(
                if (list.isEmpty()) MoneyStr.noFixed.text() else MoneyStr.fixedSum.text().format(Amounts.format(abs(perMonth), lang)),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(list, key = { it.id }) { r ->
            Row(Modifier.fillMaxWidth().clickable { onEdit(r) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.bodyLarge)
                    Text("${intervalLabel(r.interval)} · ${r.dayOfMonth}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(Amounts.formatSigned(r.amountCents, lang))
            }
        }
    }
}

@Composable
fun RecurringDialog(vm: MoneyViewModel, initial: Recurring, onDismiss: () -> Unit) {
    val lang = LocalAppLanguage.current
    val categories by vm.categories.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf(initial.title) }
    var amountText by remember { mutableStateOf(editableAmount(initial.amountCents)) }
    var income by remember { mutableStateOf(initial.amountCents > 0) }
    var categoryId by remember { mutableStateOf(initial.categoryId) }
    var interval by remember { mutableStateOf(initial.interval) }
    var dayText by remember { mutableStateOf(initial.dayOfMonth.toString()) }
    var calendar by remember { mutableStateOf(initial.showInCalendar) }
    var confirmDelete by remember { mutableStateOf(false) }
    val cents = Amounts.parseCents(amountText)?.let { abs(it) }
    val day = dayText.toIntOrNull()?.takeIf { it in 1..31 }
    val kind = if (income) CategoryKind.INCOME else CategoryKind.EXPENSE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) MoneyStr.newFixed.text() else MoneyStr.editFixed.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(MoneyStr.title.text()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                KindSwitch(income) { income = it; categoryId = null }
                AmountField(amountText, { amountText = it }, isError = amountText.isNotBlank() && cents == null)
                CategoryChips(categories.filter { it.kind == kind }, categoryId, { categoryId = it }, allLabel = null)
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurringInterval.entries.forEach { i ->
                        FilterChip(selected = interval == i, onClick = { interval = i }, label = { Text(intervalLabel(i)) })
                    }
                }
                OutlinedTextField(
                    value = dayText, onValueChange = { dayText = it.filter(Char::isDigit).take(2) }, label = { Text(MoneyStr.dayOfMonth.text()) },
                    singleLine = true, isError = day == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(MoneyStr.showInCalendar.text(), Modifier.weight(1f))
                    Switch(checked = calendar, onCheckedChange = { calendar = it })
                }
                if (initial.id != 0L) {
                    com.example.lifeorganizer.core.theme.DeleteButton(MoneyStr.delete.text(), onClick = { confirmDelete = true })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && cents != null && cents > 0 && day != null, onClick = {
                val value = cents ?: return@TextButton
                val d = day ?: return@TextButton
                vm.saveRecurring(
                    initial.copy(
                        title = title.trim(), amountCents = if (income) value else -value, interval = interval, dayOfMonth = d,
                        categoryId = categoryId ?: categories.firstOrNull { it.kind == kind && it.isFallback }?.id, showInCalendar = calendar
                    ),
                    lang
                )
                onDismiss()
            }) { Text(MoneyStr.save.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(MoneyStr.cancel.text()) } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(MoneyStr.deleteFixedTitle.text().format(initial.title)) },
            text = { Text(MoneyStr.deleteFixedBody.text()) },
            confirmButton = {
                com.example.lifeorganizer.core.theme.ConfirmDeleteButton(MoneyStr.delete.text(), onClick = { vm.deleteRecurring(initial); confirmDelete = false; onDismiss() })
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(MoneyStr.cancel.text()) } }
        )
    }
}
