package com.example.lifeorganizer.money

import android.content.Context
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.calendar.data.Category
import com.example.lifeorganizer.calendar.data.Event
import com.example.lifeorganizer.core.util.Amounts
import com.example.lifeorganizer.money.data.MoneyRepository
import com.example.lifeorganizer.money.data.Recurring
import com.example.lifeorganizer.money.domain.RecurringInterval
import com.example.lifeorganizer.money.domain.RecurringScheduler
import com.example.lifeorganizer.money.ui.MoneyStr
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** Fixed costs as all-day calendar series in the category "Fixkosten" – only for templates with showInCalendar. */
class FixedCostCalendarSync(context: Context) : FixedCostCalendar {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.getDatabase(appContext).eventDao()

    override suspend fun sync(r: Recurring, lang: String): Long? {
        if (!r.showInCalendar) {
            r.calendarEventId?.let { remove(it) }
            return null
        }
        val categoryName = MoneyStr.fixedCosts.of(lang)
        val categoryId = dao.getCategoriesSync().firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.id
            ?: dao.insertCategory(Category(name = categoryName, color = 0xFF9C27B0.toInt()))
        // Series starts at the first due date so past months stay visible
        val first = RecurringScheduler.firstDue(LocalDate.ofEpochDay(r.startEpochDay), r.dayOfMonth)
        val start = first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val rule = when (r.interval) {
            RecurringInterval.MONTHLY -> "FREQ=MONTHLY"
            RecurringInterval.QUARTERLY -> "FREQ=MONTHLY;INTERVAL=3"
            RecurringInterval.YEARLY -> "FREQ=YEARLY"
        }
        val title = "${r.title} · ${Amounts.format(abs(r.amountCents), lang)}"
        val existing = r.calendarEventId?.let { dao.getEventById(it) }
        return if (existing != null) {
            dao.updateEvent(existing.copy(title = title, startTimeMillis = start, endTimeMillis = null, isAllDay = true, recurrenceRule = rule, categoryId = categoryId))
            existing.id
        } else {
            dao.insertEvent(
                Event(title = title, description = "", startTimeMillis = start, endTimeMillis = null, isAllDay = true, recurrenceRule = rule, categoryId = categoryId)
            )
        }
    }

    override suspend fun remove(eventId: Long) {
        dao.deleteRemindersByEventId(eventId)
        dao.deleteEventById(eventId)
    }

    /** After a backup import: rebuild the series of all templates that want one. */
    suspend fun syncAll(lang: String) {
        val repo = MoneyRepository.get(appContext)
        repo.recurring().first().filter { it.showInCalendar }.forEach { r ->
            val id = sync(r, lang)
            if (id != r.calendarEventId) repo.setCalendarEventId(r.id, id)
        }
    }
}
