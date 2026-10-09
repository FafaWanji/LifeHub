package com.example.lifeorganizer.money.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifeorganizer.money.domain.CsvRow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MoneyRepositoryTest {
    private lateinit var db: MoneyDatabase
    private lateinit var dao: MoneyDao
    private var today = LocalDate.of(2026, 10, 9)
    private lateinit var repo: MoneyRepository

    @Before fun setUp() = runBlocking {
        db = MoneyDatabase.inMemory(ApplicationProvider.getApplicationContext())
        dao = db.moneyDao()
        repo = MoneyRepository(dao) { today }
        repo.ensureDefaults("de")
    }

    @After fun tearDown() = db.close()

    private suspend fun all() = dao.allTransactionsSync()

    @Test fun defaultsCreatedOnce() = runBlocking {
        repo.ensureDefaults("de")
        assertEquals(10, dao.categoriesSync().size)
        assertEquals("Sonstiges", dao.categoriesSync().single { it.isFallback && it.kind == CategoryKind.EXPENSE }.name)
    }

    @Test fun bookingMissedMonthsIsIdempotent() = runBlocking {
        repo.saveRecurring(Recurring(title = "Miete", amountCents = -80000, dayOfMonth = 1, startEpochDay = LocalDate.of(2026, 8, 1).toEpochDay()))
        assertEquals(3, repo.bookDueRecurring())
        assertEquals(0, repo.bookDueRecurring())
        assertEquals(3, all().size)
        assertEquals(LocalDate.of(2026, 11, 1).toEpochDay(), dao.recurringSync().single().nextDueEpochDay)
    }

    @Test fun deletingFixedCostKeepsBookedMonths() = runBlocking {
        val r = repo.saveRecurring(Recurring(title = "Netflix", amountCents = -1399, dayOfMonth = 5, startEpochDay = LocalDate.of(2026, 9, 1).toEpochDay()))
        repo.bookDueRecurring()
        repo.deleteRecurring(r)
        assertEquals(2, all().size)
        assertNull(all().first().recurringId)
        assertEquals(0, dao.recurringSync().size)
    }

    @Test fun deletingCategoryMovesTransactionsToFallbackAndDropsRules() = runBlocking {
        val food = dao.categoriesSync().first { it.icon == "cart" }
        repo.saveTransaction(MoneyTransaction(epochDay = today.toEpochDay(), amountCents = -500, title = "REWE", categoryId = food.id))
        assertEquals(food.id, repo.categoryFor("REWE", -100))
        repo.deleteCategory(food)
        val fallback = repo.fallbackId(CategoryKind.EXPENSE)
        assertEquals(fallback, all().single().categoryId)
        assertEquals(fallback, repo.categoryFor("REWE", -100))
    }

    @Test fun fallbackCategoryIsNotLearned() = runBlocking {
        val other = repo.fallbackId(CategoryKind.EXPENSE)
        repo.saveTransaction(MoneyTransaction(epochDay = today.toEpochDay(), amountCents = -500, title = "Kiosk", categoryId = other))
        assertEquals(0, dao.rulesSync().size)
    }

    @Test fun importTwiceAddsNothingButKeepsIdenticalRows() = runBlocking {
        val coffee = CsvRow(today, -350, "Café", "Kaffee")
        val rows = listOf(coffee, coffee, CsvRow(today, -1000, "Kino", ""))
        val ids = repo.suggestCategories(rows, null)
        assertEquals(ImportResult(3, 0, 0), repo.importRows(rows, ids))
        assertEquals(ImportResult(0, 3, 0), repo.importRows(rows, ids))
        assertEquals(3, all().size)
    }

    @Test fun bankRowReplacesBookedFixedCost() = runBlocking {
        repo.saveRecurring(Recurring(title = "Miete", amountCents = -80000, dayOfMonth = 1, startEpochDay = LocalDate.of(2026, 10, 1).toEpochDay()))
        repo.bookDueRecurring()
        val rows = listOf(CsvRow(LocalDate.of(2026, 10, 2), -80000, "Hausverwaltung", "Miete Oktober"))
        assertEquals(ImportResult(0, 0, 1), repo.importRows(rows, repo.suggestCategories(rows, null)))
        val tx = all().single()
        assertEquals(TxSource.CSV, tx.source)
        assertEquals(LocalDate.of(2026, 10, 2).toEpochDay(), tx.epochDay)
    }

    @Test fun earlyBankRowCountsForUpcomingFixedCost() = runBlocking {
        val r = repo.saveRecurring(Recurring(title = "Handy", amountCents = -1999, dayOfMonth = 12, startEpochDay = LocalDate.of(2026, 10, 9).toEpochDay()))
        val rows = listOf(CsvRow(LocalDate.of(2026, 10, 10), -1999, "Telekom", ""))
        assertEquals(ImportResult(1, 0, 1), repo.importRows(rows, repo.suggestCategories(rows, null)))
        today = LocalDate.of(2026, 10, 12)
        assertEquals(0, repo.bookDueRecurring())
        assertEquals(1, all().size)
        assertEquals(r.id, all().single().recurringId)
    }

    @Test fun aiSuggestionsUsedOnlyForUnknownPayees() = runBlocking {
        val food = dao.categoriesSync().first { it.icon == "cart" }
        repo.saveTransaction(MoneyTransaction(epochDay = today.toEpochDay(), amountCents = -500, title = "REWE", categoryId = food.id))
        var asked: List<String> = emptyList()
        val rows = listOf(CsvRow(today, -100, "REWE", ""), CsvRow(today, -6000, "Aral", ""))
        val ids = repo.suggestCategories(rows) { payees, _ -> asked = payees; mapOf("Aral" to "Mobilität") }
        assertEquals(listOf("Aral"), asked)
        assertEquals(food.id, ids[0])
        assertEquals(dao.categoriesSync().first { it.icon == "car" }.id, ids[1])
    }

    @Test fun smartAddMatchesCategoryByName() = runBlocking {
        repo.addFromSmartAdd("Döner", -1250, today.toEpochDay(), "lebensmittel")
        assertEquals(dao.categoriesSync().first { it.icon == "cart" }.id, all().single().categoryId)
        assertEquals(TxSource.SMART_ADD, all().single().source)
    }

    @Test fun expectedRestOfMonth() = runBlocking {
        repo.saveRecurring(Recurring(title = "Strom", amountCents = -8500, dayOfMonth = 20, startEpochDay = today.toEpochDay()))
        repo.saveRecurring(Recurring(title = "Miete", amountCents = -80000, dayOfMonth = 1, startEpochDay = today.toEpochDay()))
        assertEquals(-8500L, MoneyRepository.expectedAfter(today, dao.recurringSync()))
    }
}
