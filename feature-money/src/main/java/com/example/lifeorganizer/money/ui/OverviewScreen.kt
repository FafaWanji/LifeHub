package com.example.lifeorganizer.money.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.util.Amounts
import com.example.lifeorganizer.money.data.CategoryKind
import com.example.lifeorganizer.money.domain.BudgetLevel
import com.example.lifeorganizer.money.domain.MoneyMath
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

internal val incomeColor = Color(0xFF2E7D32)
private val warnColor = Color(0xFFF9A825)

@Composable
fun OverviewScreen(vm: MoneyViewModel, modifier: Modifier = Modifier) {
    val lang = LocalAppLanguage.current
    val month by vm.month.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val expected by vm.expected.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val txs by vm.monthTransactions.collectAsStateWithLifecycle()
    val byCategory = remember(txs) { MoneyMath.spendingByCategory(txs.map { it.categoryId to it.amountCents }) }
    val catById = remember(categories) { categories.associateBy { it.id } }

    LazyColumn(
        modifier = modifier.pointerInput(Unit) {
            var drag = 0f
            detectHorizontalDragGestures(
                onDragEnd = { if (abs(drag) > 120) vm.shiftMonth(if (drag < 0) 1 else -1); drag = 0f },
                onHorizontalDrag = { _, amount -> drag += amount }
            )
        },
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MonthSwitcher(month, lang, vm::shiftMonth) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(MoneyStr.income.text(), Amounts.format(summary.incomeCents, lang), incomeColor, Modifier.weight(1f))
                SummaryCard(MoneyStr.expenses.text(), Amounts.format(abs(summary.expenseCents), lang), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                SummaryCard(
                    MoneyStr.rest.text(), Amounts.format(summary.restCents, lang),
                    if (summary.restCents >= 0) incomeColor else MaterialTheme.colorScheme.error, Modifier.weight(1f)
                )
            }
        }
        if (expected != 0L) item {
            Text(
                MoneyStr.expectedFixed.text().format(Amounts.format(abs(expected), lang)),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (byCategory.isNotEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    DonutChart(byCategory.map { (id, cents) -> Color(catById[id]?.color ?: 0xFF9E9E9E.toInt()) to cents }, Modifier.size(160.dp))
                    Text(Amounts.format(abs(summary.expenseCents), lang), style = MaterialTheme.typography.titleSmall)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    byCategory.take(6).forEach { (id, cents) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(categoryIcon(catById[id]?.icon ?: "other"), null, tint = Color(catById[id]?.color ?: 0xFF9E9E9E.toInt()), modifier = Modifier.size(18.dp))
                            Text(" ${catById[id]?.name.orEmpty()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp), maxLines = 1)
                            Text(Amounts.format(cents, lang), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        items(categories.filter { it.kind == CategoryKind.EXPENSE && (it.monthlyBudgetCents ?: 0) > 0 }, key = { it.id }) { c ->
            val budget = c.monthlyBudgetCents ?: 0L
            val spent = byCategory.firstOrNull { it.first == c.id }?.second ?: 0L
            val fraction = MoneyMath.budgetFraction(spent, budget)
            val color = when (MoneyMath.budgetLevel(fraction)) {
                BudgetLevel.OK -> MaterialTheme.colorScheme.primary
                BudgetLevel.WARN -> warnColor
                BudgetLevel.OVER -> MaterialTheme.colorScheme.error
            }
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(c.name, style = MaterialTheme.typography.bodyMedium)
                    Text(MoneyStr.spentOf.text().format(Amounts.format(spent, lang), Amounts.format(budget, lang)), style = MaterialTheme.typography.bodySmall)
                }
                LinearProgressIndicator(progress = { fraction.coerceAtMost(1f) }, color = color, modifier = Modifier.fillMaxWidth())
            }
        }
        if (txs.isEmpty()) item {
            Text(MoneyStr.noTransactions.text(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MonthSwitcher(month: YearMonth, lang: String, onShift: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onShift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, MoneyStr.previousMonth.text()) }
        Text(
            run {
                // Each language's own order: "Oktober 2026", "2026年10月"
                val locale = if (lang == "zh") Locale.SIMPLIFIED_CHINESE else Locale.forLanguageTag(lang)
                month.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "yyyyLLLL"), locale))
                    .replaceFirstChar { it.uppercase() }
            },
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = { onShift(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, MoneyStr.nextMonth.text()) }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
        }
    }
}
