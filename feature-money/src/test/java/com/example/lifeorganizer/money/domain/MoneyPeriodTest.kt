package com.example.lifeorganizer.money.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MoneyPeriodTest {
    private val october = YearMonth.of(2026, 10)

    @Test
    fun calendarMonthByDefault() {
        assertEquals(LocalDate.of(2026, 10, 1), MoneyMath.periodStart(october, 1))
        assertEquals(LocalDate.of(2026, 10, 31), MoneyMath.periodEnd(october, 1))
        assertEquals(october, MoneyMath.periodOf(LocalDate.of(2026, 10, 20), 1))
    }

    @Test
    fun paydayMonthRunsIntoTheNext() {
        // Starting on the 25th: "October" is 25 Oct – 24 Nov
        assertEquals(LocalDate.of(2026, 10, 25), MoneyMath.periodStart(october, 25))
        assertEquals(LocalDate.of(2026, 11, 24), MoneyMath.periodEnd(october, 25))
        assertEquals(YearMonth.of(2026, 9), MoneyMath.periodOf(LocalDate.of(2026, 10, 20), 25))
        assertEquals(october, MoneyMath.periodOf(LocalDate.of(2026, 10, 25), 25))
    }

    @Test
    fun shortMonthsUseTheirLastDay() {
        assertEquals(LocalDate.of(2027, 2, 28), MoneyMath.periodStart(YearMonth.of(2027, 2), 30))
    }

    @Test
    fun budgetWarningFollowsTheSetting() {
        assertEquals(BudgetLevel.OK, MoneyMath.budgetLevel(0.75f, warnAt = 0.8f))
        assertEquals(BudgetLevel.WARN, MoneyMath.budgetLevel(0.75f, warnAt = 0.7f))
        assertEquals(BudgetLevel.OVER, MoneyMath.budgetLevel(1f, warnAt = 0.9f))
    }
}
