package com.example.lifeorganizer.money.domain

data class MonthSummary(val incomeCents: Long, val expenseCents: Long) {
    /** Expenses are negative, so the rest is a plain sum. */
    val restCents: Long get() = incomeCents + expenseCents
}

enum class BudgetLevel { OK, WARN, OVER }

object MoneyMath {
    fun summarize(amounts: List<Long>): MonthSummary =
        MonthSummary(amounts.filter { it > 0 }.sum(), amounts.filter { it < 0 }.sum())

    /** [spentCents] is positive. No budget → 0. */
    fun budgetFraction(spentCents: Long, budgetCents: Long): Float =
        if (budgetCents <= 0) 0f else spentCents.toFloat() / budgetCents

    /** Yellow from [warnAt] (80 % unless set otherwise in the finance settings), red from 100 %. */
    fun budgetLevel(fraction: Float, warnAt: Float = 0.8f): BudgetLevel = when {
        fraction >= 1f -> BudgetLevel.OVER
        fraction >= warnAt -> BudgetLevel.WARN
        else -> BudgetLevel.OK
    }

    /**
     * First day of the finance month [month] when months start on [startDay] (e.g. 25 = payday):
     * "October" then runs from 25 Oct to 24 Nov. Short months use their last day.
     */
    fun periodStart(month: java.time.YearMonth, startDay: Int): java.time.LocalDate =
        month.atDay(startDay.coerceIn(1, month.lengthOfMonth()))

    fun periodEnd(month: java.time.YearMonth, startDay: Int): java.time.LocalDate =
        periodStart(month.plusMonths(1), startDay).minusDays(1)

    /** The finance month [date] belongs to. */
    fun periodOf(date: java.time.LocalDate, startDay: Int): java.time.YearMonth {
        val month = java.time.YearMonth.from(date)
        return if (date.isBefore(periodStart(month, startDay))) month.minusMonths(1) else month
    }

    /** (categoryId, amountCents) → spending per category as positive cents, largest first. Income is ignored. */
    fun spendingByCategory(items: List<Pair<Long?, Long>>): List<Pair<Long?, Long>> =
        items.filter { it.second < 0 }
            .groupBy({ it.first }, { -it.second })
            .map { (id, values) -> id to values.sum() }
            .sortedByDescending { it.second }
}
