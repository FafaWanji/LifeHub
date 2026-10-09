package com.example.lifeorganizer.money

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifeorganizer.calendar.data.AppDatabase
import com.example.lifeorganizer.money.data.Recurring
import com.example.lifeorganizer.money.domain.RecurringInterval
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class FixedCostCalendarSyncTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dao = AppDatabase.getDatabase(context).eventDao()

    @Test fun createsUpdatesAndRemovesSeries(): Unit = runBlocking {
        AppDatabase.getDatabase(context).clearAllTables()
        val sync = FixedCostCalendarSync(context)
        val r = Recurring(id = 1, title = "Miete", amountCents = -80000, dayOfMonth = 1, startEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(), showInCalendar = true)

        val id = sync.sync(r, "de")!!
        val event = dao.getEventById(id)!!
        assertEquals("FREQ=MONTHLY", event.recurrenceRule)
        assertEquals(true, event.isAllDay)
        assertEquals("Fixkosten", dao.getCategoriesSync().single { it.id == event.categoryId }.name)

        val quarterly = sync.sync(r.copy(interval = RecurringInterval.QUARTERLY, calendarEventId = id), "de")
        assertEquals(id, quarterly)
        assertEquals("FREQ=MONTHLY;INTERVAL=3", dao.getEventById(id)!!.recurrenceRule)

        assertNull(sync.sync(r.copy(showInCalendar = false, calendarEventId = id), "de"))
        assertNull(dao.getEventById(id))
        assertNotNull(dao.getCategoriesSync().firstOrNull { it.name == "Fixkosten" })
        AppDatabase.getDatabase(context).clearAllTables()
    }
}
