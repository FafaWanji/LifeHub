package com.example.lifeorganizer.money.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeorganizer.core.settings.SettingsManager
import com.example.lifeorganizer.money.MoneyIntegration
import com.example.lifeorganizer.money.data.CategoryKind
import com.example.lifeorganizer.money.data.ImportResult
import com.example.lifeorganizer.money.data.MoneyCategory
import com.example.lifeorganizer.money.data.MoneyRepository
import com.example.lifeorganizer.money.data.MoneyTransaction
import com.example.lifeorganizer.money.data.PayeeRule
import com.example.lifeorganizer.money.data.Recurring
import com.example.lifeorganizer.money.domain.ColumnMapping
import com.example.lifeorganizer.money.domain.CsvImporter
import com.example.lifeorganizer.money.domain.CsvRow
import com.example.lifeorganizer.money.domain.GroqCategorizer
import com.example.lifeorganizer.money.domain.MoneyMath
import com.example.lifeorganizer.money.domain.MonthSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

sealed interface ImportState {
    data object Idle : ImportState
    data object Loading : ImportState
    class NeedsMapping(val header: List<String>, val sample: List<String>, val bytes: ByteArray) : ImportState
    data class Preview(val rows: List<CsvRow>, val categoryIds: List<Long?>, val skipped: Int) : ImportState
    data class Done(val result: ImportResult) : ImportState
    data object Error : ImportState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MoneyRepository.get(application)
    private val prefs = application.getSharedPreferences("money", Context.MODE_PRIVATE)
    private val settings = SettingsManager(application)

    val month = MutableStateFlow(YearMonth.now())
    val search = MutableStateFlow("")
    val categoryFilter = MutableStateFlow<Long?>(null)

    val categories: StateFlow<List<MoneyCategory>> = repo.categories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recurring: StateFlow<List<Recurring>> = repo.recurring().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val rules: StateFlow<List<PayeeRule>> = repo.rules().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val monthTransactions: StateFlow<List<MoneyTransaction>> =
        month.flatMapLatest { repo.transactionsIn(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val summary: StateFlow<MonthSummary> = monthTransactions.map { list -> MoneyMath.summarize(list.map { it.amountCents }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MonthSummary(0, 0))

    /** Fixed costs still due this month; only shown for the current month. */
    val expected: StateFlow<Long> = combine(month, recurring) { m, list ->
        if (m == YearMonth.now()) MoneyRepository.expectedAfter(LocalDate.now(), list) else 0L
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val visibleTransactions: StateFlow<List<MoneyTransaction>> = combine(monthTransactions, search, categoryFilter) { list, q, c ->
        list.filter { (c == null || it.categoryId == c) && (q.isBlank() || it.title.contains(q, true) || it.note.contains(q, true)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val aiEnabled = MutableStateFlow(prefs.getBoolean("ai_categorize", false))
    val importState = MutableStateFlow<ImportState>(ImportState.Idle)

    fun init(lang: String) = viewModelScope.launch {
        repo.ensureDefaults(lang)
        repo.bookDueRecurring()
    }

    fun shiftMonth(delta: Int) { month.value = month.value.plusMonths(delta.toLong()) }

    fun saveTransaction(tx: MoneyTransaction) = viewModelScope.launch { repo.saveTransaction(tx) }
    fun deleteTransaction(tx: MoneyTransaction) = viewModelScope.launch { repo.deleteTransaction(tx) }
    fun restoreTransaction(tx: MoneyTransaction) = viewModelScope.launch { repo.restoreTransaction(tx) }

    fun saveCategory(c: MoneyCategory) = viewModelScope.launch { repo.saveCategory(c) }
    fun deleteCategory(c: MoneyCategory) = viewModelScope.launch { repo.deleteCategory(c) }
    fun deleteRule(rule: PayeeRule) = viewModelScope.launch { repo.deleteRule(rule) }
    fun fallbackName(kind: CategoryKind): String = categories.value.firstOrNull { it.kind == kind && it.isFallback }?.name.orEmpty()

    fun saveRecurring(r: Recurring, lang: String) = viewModelScope.launch {
        val saved = repo.saveRecurring(r)
        MoneyIntegration.calendar?.let { calendar ->
            val eventId = calendar.sync(saved, lang)
            if (eventId != saved.calendarEventId) repo.setCalendarEventId(saved.id, eventId)
        }
        repo.bookDueRecurring()
    }

    fun deleteRecurring(r: Recurring) = viewModelScope.launch {
        r.calendarEventId?.let { MoneyIntegration.calendar?.remove(it) }
        repo.deleteRecurring(r)
    }

    // ---- CSV import

    fun setAiEnabled(enabled: Boolean) {
        aiEnabled.value = enabled
        prefs.edit().putBoolean("ai_categorize", enabled).apply()
    }

    fun resetImport() { importState.value = ImportState.Idle }

    fun loadCsv(read: () -> ByteArray) = viewModelScope.launch(Dispatchers.IO) {
        importState.value = ImportState.Loading
        val bytes = runCatching(read).getOrNull() ?: run { importState.value = ImportState.Error; return@launch }
        val auto = CsvImporter.parse(bytes)
        val result = if (auto.mapping != null) auto else {
            prefs.getString("csv_" + CsvImporter.headerKey(auto.header), null)
                ?.let(ColumnMapping::decode)
                ?.let { CsvImporter.parse(bytes, it) } ?: auto
        }
        when {
            result.mapping == null && result.header.size >= 2 ->
                importState.value = ImportState.NeedsMapping(result.header, result.sample, bytes)
            result.rows.isEmpty() -> importState.value = ImportState.Error
            else -> preview(result.rows, result.skipped)
        }
    }

    fun applyMapping(state: ImportState.NeedsMapping, mapping: ColumnMapping) = viewModelScope.launch(Dispatchers.IO) {
        importState.value = ImportState.Loading
        val result = CsvImporter.parse(state.bytes, mapping)
        if (result.rows.isEmpty()) { importState.value = ImportState.Error; return@launch }
        prefs.edit().putString("csv_" + CsvImporter.headerKey(state.header), mapping.encode()).apply()
        preview(result.rows, result.skipped)
    }

    private suspend fun preview(rows: List<CsvRow>, skipped: Int) {
        val key = settings.geminiApiKey.first().orEmpty()
        val ai: (suspend (List<String>, List<String>) -> Map<String, String>)? =
            if (aiEnabled.value && key.isNotBlank()) { payees, cats -> GroqCategorizer.categorize(key, payees, cats) } else null
        importState.value = ImportState.Preview(rows, repo.suggestCategories(rows, ai), skipped)
    }

    fun confirmImport() {
        val state = importState.value as? ImportState.Preview ?: return
        viewModelScope.launch(Dispatchers.IO) {
            importState.value = ImportState.Loading
            importState.value = ImportState.Done(repo.importRows(state.rows, state.categoryIds))
        }
    }
}
