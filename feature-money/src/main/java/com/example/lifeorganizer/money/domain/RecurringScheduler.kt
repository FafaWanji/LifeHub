package com.example.lifeorganizer.money.domain

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

enum class RecurringInterval(val months: Int) { MONTHLY(1), QUARTERLY(3), YEARLY(12) }

/** Due dates of fixed costs. A day past the month's end means the month's last day. */
object RecurringScheduler {
    fun clamp(month: YearMonth, dayOfMonth: Int): LocalDate =
        month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))

    /** First due date on or after [start]. */
    fun firstDue(start: LocalDate, dayOfMonth: Int): LocalDate {
        val inMonth = clamp(YearMonth.from(start), dayOfMonth)
        return if (inMonth.isBefore(start)) clamp(YearMonth.from(start).plusMonths(1), dayOfMonth) else inMonth
    }

    fun following(due: LocalDate, dayOfMonth: Int, interval: RecurringInterval): LocalDate =
        clamp(YearMonth.from(due).plusMonths(interval.months.toLong()), dayOfMonth)

    /** All due dates from [nextDue] up to and including [until] (missed months included). */
    fun dueUntil(nextDue: LocalDate, dayOfMonth: Int, interval: RecurringInterval, until: LocalDate): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var d = nextDue
        while (!d.isAfter(until)) {
            out += d
            d = following(d, dayOfMonth, interval)
        }
        return out
    }

    /** Share per month, e.g. 120 € yearly → 10 € per month. */
    fun monthlyEquivalentCents(amountCents: Long, interval: RecurringInterval): Long =
        (amountCents.toDouble() / interval.months).roundToLong()
}
