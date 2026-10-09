package com.example.lifeorganizer.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.util.Amounts
import com.example.lifeorganizer.money.data.MoneyCategory
import com.example.lifeorganizer.money.data.MoneyTransaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TransactionsScreen(vm: MoneyViewModel, onEdit: (MoneyTransaction) -> Unit, onDeleted: (MoneyTransaction) -> Unit) {
    val lang = LocalAppLanguage.current
    val month by vm.month.collectAsStateWithLifecycle()
    val txs by vm.visibleTransactions.collectAsStateWithLifecycle()
    val query by vm.search.collectAsStateWithLifecycle()
    val filter by vm.categoryFilter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val catById = remember(categories) { categories.associateBy { it.id } }
    val dayFormat = remember(lang) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.forLanguageTag(lang)) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            MonthSwitcher(month, lang, vm::shiftMonth)
            OutlinedTextField(
                value = query, onValueChange = { vm.search.value = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text(MoneyStr.search.text()) }, modifier = Modifier.fillMaxWidth()
            )
        }
        CategoryChips(categories, filter, { vm.categoryFilter.value = it }, MoneyStr.all.text(), Modifier.padding(vertical = 8.dp))
        if (txs.isEmpty()) {
            Text(MoneyStr.noTransactions.text(), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            txs.groupBy { it.epochDay }.forEach { (day, list) ->
                item(key = "d$day") {
                    Text(
                        LocalDate.ofEpochDay(day).format(dayFormat),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
                items(list, key = { it.id }) { tx ->
                    SwipeToDelete(onDelete = { vm.deleteTransaction(tx); onDeleted(tx) }) {
                        TransactionRow(tx, catById[tx.categoryId], lang, onClick = { onEdit(tx) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDelete()
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) { Icon(Icons.Outlined.Delete, MoneyStr.delete.text(), tint = MaterialTheme.colorScheme.onErrorContainer) }
        }
    ) { content() }
}

@Composable
private fun TransactionRow(tx: MoneyTransaction, category: MoneyCategory?, lang: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val color = Color(category?.color ?: 0xFF9E9E9E.toInt())
        Box(Modifier.size(36.dp).background(color.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(category?.icon ?: "other"), null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(tx.title.ifBlank { category?.name.orEmpty() }, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(category?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(
            Amounts.formatSigned(tx.amountCents, lang),
            color = if (tx.amountCents >= 0) incomeColor else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/** Horizontal category chooser; [allLabel] adds an "all" chip that selects null. */
@Composable
fun CategoryChips(
    categories: List<MoneyCategory>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    allLabel: String?,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (allLabel != null) item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(allLabel) }) }
        items(categories, key = { it.id }) { c ->
            FilterChip(
                selected = selected == c.id, onClick = { onSelect(c.id) }, label = { Text(c.name) },
                leadingIcon = { Icon(categoryIcon(c.icon), null, tint = Color(c.color), modifier = Modifier.size(18.dp)) }
            )
        }
    }
}
