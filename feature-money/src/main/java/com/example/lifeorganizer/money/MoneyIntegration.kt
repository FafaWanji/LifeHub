package com.example.lifeorganizer.money

import com.example.lifeorganizer.money.data.Recurring

/** Calendar series for a fixed cost; implemented in the app so this module does not depend on the calendar. */
interface FixedCostCalendar {
    /** Creates, updates or (when showInCalendar is off) removes the series. Returns the event id or null. */
    suspend fun sync(r: Recurring, lang: String): Long?
    suspend fun remove(eventId: Long)
}

object MoneyIntegration {
    @Volatile var calendar: FixedCostCalendar? = null
}
