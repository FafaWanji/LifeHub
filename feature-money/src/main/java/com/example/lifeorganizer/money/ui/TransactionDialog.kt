package com.example.lifeorganizer.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.util.Amounts
import com.example.lifeorganizer.money.data.CategoryKind
import com.example.lifeorganizer.money.data.MoneyTransaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

private const val DAY_MS = 86_400_000L

fun newTransaction() = MoneyTransaction(epochDay = LocalDate.now().toEpochDay(), amountCents = 0, title = "")

/** Amount text for editing: "12,50" (decimal comma, parsed back by [Amounts.parseCents]). */
internal fun editableAmount(cents: Long): String =
    if (cents == 0L) "" else "%d,%02d".format(abs(cents) / 100, abs(cents) % 100)

/** Expense/income switch used by the transaction and fixed-cost dialogs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KindSwitch(income: Boolean, onChange: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(selected = !income, onClick = { onChange(false) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(MoneyStr.expense.text()) }
        SegmentedButton(selected = income, onClick = { onChange(true) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(MoneyStr.incomeOne.text()) }
    }
}

@Composable
internal fun AmountField(text: String, onChange: (String) -> Unit, isError: Boolean, label: String = MoneyStr.amount.text()) {
    OutlinedTextField(
        value = text, onValueChange = onChange, label = { Text(label) }, suffix = { Text("€") },
        singleLine = true, isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(vm: MoneyViewModel, initial: MoneyTransaction, onDismiss: () -> Unit) {
    val lang = LocalAppLanguage.current
    val categories by vm.categories.collectAsStateWithLifecycle()
    var amountText by remember { mutableStateOf(editableAmount(initial.amountCents)) }
    var income by remember { mutableStateOf(initial.amountCents > 0) }
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var categoryId by remember { mutableStateOf(initial.categoryId) }
    var epochDay by remember { mutableStateOf(initial.epochDay) }
    var pickDate by remember { mutableStateOf(false) }
    val cents = Amounts.parseCents(amountText)?.let { abs(it) }
    val kind = if (income) CategoryKind.INCOME else CategoryKind.EXPENSE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) MoneyStr.newTransaction.text() else MoneyStr.editTransaction.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KindSwitch(income) { income = it; categoryId = null }
                AmountField(amountText, { amountText = it }, isError = amountText.isNotBlank() && cents == null)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(MoneyStr.title.text()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(MoneyStr.category.text())
                CategoryChips(categories.filter { it.kind == kind }, categoryId, { categoryId = it }, allLabel = null)
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang))))
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(MoneyStr.note.text()) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = cents != null && cents > 0, onClick = {
                val value = cents ?: return@TextButton
                val category = categoryId ?: categories.firstOrNull { it.kind == kind && it.isFallback }?.id
                vm.saveTransaction(
                    initial.copy(amountCents = if (income) value else -value, title = title.trim(), note = note.trim(), categoryId = category, epochDay = epochDay)
                )
                onDismiss()
            }) { Text(MoneyStr.save.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(MoneyStr.cancel.text()) } }
    )

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { epochDay = it / DAY_MS }; pickDate = false }) { Text(MoneyStr.save.text()) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(MoneyStr.cancel.text()) } }
        ) { DatePicker(state) }
    }
}
