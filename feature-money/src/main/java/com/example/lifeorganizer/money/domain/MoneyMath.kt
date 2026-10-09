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

    /** Yellow from 80 %, red from 100 %. */
    fun budgetLevel(fraction: Float): BudgetLevel = when {
        fraction >= 1f -> BudgetLevel.OVER
        fraction >= 0.8f -> BudgetLevel.WARN
        else -> BudgetLevel.OK
    }

    /** (categoryId, amountCents) → spending per category as positive cents, largest first. Income is ignored. */
    fun spendingByCategory(items: List<Pair<Long?, Long>>): List<Pair<Long?, Long>> =
        items.filter { it.second < 0 }
            .groupBy({ it.first }, { -it.second })
            .map { (id, values) -> id to values.sum() }
            .sortedByDescending { it.second }
}
