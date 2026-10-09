package com.example.lifeorganizer.money.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.money.data.MoneyCategory
import com.example.lifeorganizer.money.data.MoneyTransaction
import com.example.lifeorganizer.money.data.Recurring
import kotlinx.coroutines.launch

enum class MoneyTab { OVERVIEW, TRANSACTIONS, FIXED, CATEGORIES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyHost(onMenuClick: () -> Unit, vm: MoneyViewModel = viewModel()) {
    val lang = LocalAppLanguage.current
    LaunchedEffect(lang) { vm.init(lang) }
    var tab by rememberSaveable { mutableStateOf(MoneyTab.OVERVIEW) }
    var showImport by rememberSaveable { mutableStateOf(false) }
    var editTx by remember { mutableStateOf<MoneyTransaction?>(null) }
    var editRecurring by remember { mutableStateOf<Recurring?>(null) }
    var editCategory by remember { mutableStateOf<MoneyCategory?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showImport) {
        ImportScreen(vm, onClose = { showImport = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(MoneyStr.finance.text()) },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, Str.menu.text()) } },
                actions = { IconButton(onClick = { showImport = true }) { Icon(Icons.Outlined.FileUpload, MoneyStr.importCsv.text()) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (tab) {
                    MoneyTab.OVERVIEW, MoneyTab.TRANSACTIONS -> editTx = newTransaction()
                    MoneyTab.FIXED -> editRecurring = newRecurring()
                    MoneyTab.CATEGORIES -> editCategory = newCategory()
                }
            }) {
                Icon(
                    Icons.Default.Add,
                    when (tab) {
                        MoneyTab.OVERVIEW, MoneyTab.TRANSACTIONS -> MoneyStr.newTransaction.text()
                        MoneyTab.FIXED -> MoneyStr.newFixed.text()
                        MoneyTab.CATEGORIES -> MoneyStr.newCategory.text()
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal) {
                MoneyTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { tab = t }, text = {
                        Text(
                            when (t) {
                                MoneyTab.OVERVIEW -> MoneyStr.overview.text()
                                MoneyTab.TRANSACTIONS -> MoneyStr.transactions.text()
                                MoneyTab.FIXED -> MoneyStr.fixedCosts.text()
                                MoneyTab.CATEGORIES -> MoneyStr.categories.text()
                            }
                        )
                    })
                }
            }
            when (tab) {
                MoneyTab.OVERVIEW -> OverviewScreen(vm)
                MoneyTab.TRANSACTIONS -> TransactionsScreen(
                    vm,
                    onEdit = { editTx = it },
                    onDeleted = { tx ->
                        scope.launch {
                            val r = snackbar.showSnackbar(MoneyStr.deleted.of(lang), MoneyStr.undo.of(lang), duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) vm.restoreTransaction(tx)
                        }
                    }
                )
                MoneyTab.FIXED -> RecurringScreen(vm, onEdit = { editRecurring = it })
                MoneyTab.CATEGORIES -> CategoriesScreen(vm, onEdit = { editCategory = it })
            }
        }
    }

    editTx?.let { tx -> TransactionDialog(vm, tx, onDismiss = { editTx = null }) }
    editRecurring?.let { r -> RecurringDialog(vm, r, onDismiss = { editRecurring = null }) }
    editCategory?.let { c -> CategoryDialog(vm, c, onDismiss = { editCategory = null }) }
}
