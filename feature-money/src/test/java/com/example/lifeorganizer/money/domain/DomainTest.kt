package com.example.lifeorganizer.money.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DomainTest {
    @Test fun summaryRest() {
        val s = MoneyMath.summarize(listOf(210000, -80000, -2345, 500))
        assertEquals(210500L, s.incomeCents)
        assertEquals(-82345L, s.expenseCents)
        assertEquals(128155L, s.restCents)
    }

    @Test fun budgetLevels() {
        assertEquals(BudgetLevel.OK, MoneyMath.budgetLevel(MoneyMath.budgetFraction(7900, 10000)))
        assertEquals(BudgetLevel.WARN, MoneyMath.budgetLevel(MoneyMath.budgetFraction(8000, 10000)))
        assertEquals(BudgetLevel.OVER, MoneyMath.budgetLevel(MoneyMath.budgetFraction(10000, 10000)))
        assertEquals(0f, MoneyMath.budgetFraction(500, 0))
    }

    @Test fun spendingByCategoryIgnoresIncomeAndSorts() {
        val r = MoneyMath.spendingByCategory(listOf(1L to -500L, 2L to -2000L, 1L to -700L, 3L to 9000L, null to -100L))
        assertEquals(listOf(2L to 2000L, 1L to 1200L, null to 100L), r)
    }

    @Test fun day31ClampsToMonthEnd() {
        val start = LocalDate.of(2026, 1, 31)
        val dues = RecurringScheduler.dueUntil(start, 31, RecurringInterval.MONTHLY, LocalDate.of(2026, 4, 30))
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)),
            dues
        )
    }

    @Test fun firstDueThisOrNextMonth() {
        assertEquals(LocalDate.of(2026, 10, 15), RecurringScheduler.firstDue(LocalDate.of(2026, 10, 9), 15))
        assertEquals(LocalDate.of(2026, 11, 1), RecurringScheduler.firstDue(LocalDate.of(2026, 10, 9), 1))
        assertEquals(LocalDate.of(2026, 10, 9), RecurringScheduler.firstDue(LocalDate.of(2026, 10, 9), 9))
    }

    @Test fun quarterlyAndYearly() {
        assertEquals(LocalDate.of(2027, 1, 5), RecurringScheduler.following(LocalDate.of(2026, 10, 5), 5, RecurringInterval.QUARTERLY))
        assertEquals(LocalDate.of(2028, 2, 29), RecurringScheduler.following(LocalDate.of(2027, 2, 28), 29, RecurringInterval.YEARLY))
    }

    @Test fun missedMonthsAllDue() {
        val dues = RecurringScheduler.dueUntil(LocalDate.of(2026, 7, 1), 1, RecurringInterval.MONTHLY, LocalDate.of(2026, 10, 9))
        assertEquals(4, dues.size)
    }

    @Test fun nothingDueBeforeNextDue() {
        assertEquals(emptyList<LocalDate>(), RecurringScheduler.dueUntil(LocalDate.of(2026, 11, 1), 1, RecurringInterval.MONTHLY, LocalDate.of(2026, 10, 31)))
    }

    @Test fun monthlyEquivalent() {
        assertEquals(-1000L, RecurringScheduler.monthlyEquivalentCents(-3000, RecurringInterval.QUARTERLY))
        assertEquals(-1000L, RecurringScheduler.monthlyEquivalentCents(-12000, RecurringInterval.YEARLY))
    }

    @Test fun clampMonth() = assertEquals(LocalDate.of(2026, 2, 28), RecurringScheduler.clamp(YearMonth.of(2026, 2), 30))

    @Test fun normalizePayees() {
        assertEquals("rewe markt", PayeeNormalizer.normalize("REWE MARKT 1234 BERLIN"))
        assertEquals("rewe markt", PayeeNormalizer.normalize("REWE Markt GmbH//Berlin/DE"))
        assertEquals("paypal europe", PayeeNormalizer.normalize("PayPal (Europe) S.a.r.l. et Cie., S.C.A."))
        assertEquals("dm drogerie", PayeeNormalizer.normalize("dm-drogerie markt"))
        assertEquals("muenchner verkehrsgesellschaft", PayeeNormalizer.normalize("Münchner Verkehrsgesellschaft mbH"))
        assertEquals("", PayeeNormalizer.normalize("12345"))
    }
}
