package com.example.lifeorganizer.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.util.Amounts
import com.example.lifeorganizer.money.data.CategoryKind
import com.example.lifeorganizer.money.data.MoneyCategory
import kotlin.math.abs

private val palette = listOf(0xFF4CAF50, 0xFF795548, 0xFF2196F3, 0xFFFF9800, 0xFF9C27B0, 0xFFE91E63, 0xFF00BCD4, 0xFF9E9E9E).map { it.toInt() }

fun newCategory() = MoneyCategory(name = "", color = palette[2], icon = "other", kind = CategoryKind.EXPENSE)

@Composable
fun CategoriesScreen(vm: MoneyViewModel, onEdit: (MoneyCategory) -> Unit) {
    val lang = LocalAppLanguage.current
    val categories by vm.categories.collectAsStateWithLifecycle()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val names = categories.associate { it.id to it.name }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp)) {
        items(categories, key = { it.id }) { c ->
            Row(
                Modifier.fillMaxWidth().clickable { onEdit(c) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(categoryIcon(c.icon), null, tint = Color(c.color))
                Text(c.name, Modifier.weight(1f))
                c.monthlyBudgetCents?.let { Text(Amounts.format(it, lang), style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(MoneyStr.rules.text(), style = MaterialTheme.typography.titleSmall)
            if (rules.isEmpty()) Text(MoneyStr.noRules.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(rules, key = { it.normalizedPayee }) { rule ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${rule.normalizedPayee} → ${names[rule.categoryId].orEmpty()}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { vm.deleteRule(rule) }) { Icon(Icons.Outlined.Close, MoneyStr.delete.text()) }
            }
        }
    }
}

@Composable
fun CategoryDialog(vm: MoneyViewModel, initial: MoneyCategory, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var color by remember { mutableStateOf(initial.color) }
    var icon by remember { mutableStateOf(initial.icon) }
    var kind by remember { mutableStateOf(initial.kind) }
    var budgetText by remember { mutableStateOf(initial.monthlyBudgetCents?.let { editableAmount(it) }.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val budget = budgetText.takeIf { it.isNotBlank() }?.let { Amounts.parseCents(it)?.let(::abs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) MoneyStr.newCategory.text() else MoneyStr.editCategory.text()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(MoneyStr.name.text()) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (initial.id == 0L) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = kind == CategoryKind.EXPENSE, onClick = { kind = CategoryKind.EXPENSE }, label = { Text(MoneyStr.expense.text()) })
                        FilterChip(selected = kind == CategoryKind.INCOME, onClick = { kind = CategoryKind.INCOME }, label = { Text(MoneyStr.incomeOne.text()) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(palette) { p ->
                        Box(
                            Modifier.size(32.dp).background(Color(p), CircleShape)
                                .then(if (p == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = p }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(categoryIconKeys) { key ->
                        IconButton(onClick = { icon = key }) {
                            Icon(categoryIcon(key), key, tint = if (key == icon) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (kind == CategoryKind.EXPENSE) {
                    AmountField(budgetText, { budgetText = it }, isError = budgetText.isNotBlank() && budget == null, label = MoneyStr.budget.text())
                }
                if (initial.id != 0L && !initial.isFallback) {
                    com.example.lifeorganizer.core.theme.DeleteButton(MoneyStr.delete.text(), onClick = { confirmDelete = true })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && (budgetText.isBlank() || budget != null), onClick = {
                vm.saveCategory(
                    initial.copy(
                        name = name.trim(), color = color, icon = icon, kind = kind,
                        monthlyBudgetCents = if (kind == CategoryKind.EXPENSE) budget?.takeIf { it > 0 } else null
                    )
                )
                onDismiss()
            }) { Text(MoneyStr.save.text()) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(MoneyStr.cancel.text()) } }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(MoneyStr.deleteFixedTitle.text().format(initial.name)) },
            text = { Text(MoneyStr.deleteCategoryBody.text().format(vm.fallbackName(initial.kind))) },
            confirmButton = {
                com.example.lifeorganizer.core.theme.ConfirmDeleteButton(MoneyStr.delete.text(), onClick = { vm.deleteCategory(initial); confirmDelete = false; onDismiss() })
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(MoneyStr.cancel.text()) } }
        )
    }
}
