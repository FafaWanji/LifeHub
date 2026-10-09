package com.example.lifeorganizer.core.smartadd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MoneyTextParserTest {
    private val today = LocalDate.of(2026, 10, 9)

    @Test fun expenseYesterday() {
        val t = MoneyTextParser.parse("12,50 Döner gestern", today)!!
        assertEquals(-1250L, t.amountCents)
        assertEquals("Döner", t.title)
        assertEquals(today.minusDays(1).toEpochDay(), t.epochDay)
    }

    @Test fun euroSignAndFiller() {
        val t = MoneyTextParser.parse("Kino 9 € ausgegeben", today)!!
        assertEquals(-900L, t.amountCents)
        assertEquals("Kino", t.title)
        assertEquals(today.toEpochDay(), t.epochDay)
    }

    @Test fun incomeByKeyword() {
        val t = MoneyTextParser.parse("Gehalt 2.100 € bekommen", today)!!
        assertEquals(210000L, t.amountCents)
    }

    @Test fun incomeByPlus() = assertEquals(500L, MoneyTextParser.parse("+5 € Pfand", today)!!.amountCents)

    @Test fun explicitDate() =
        assertEquals(LocalDate.of(2026, 10, 3).toEpochDay(), MoneyTextParser.parse("3.10. Tanken 65,10 €", today)!!.epochDay)

    @Test fun timeKeepsEvent() {
        assertNull(MoneyTextParser.parse("Kino morgen 20 Uhr 12 €", today))
        val r = OfflineParser.parse("Kino morgen 20 Uhr 12 €", now = LocalDateTime.of(2026, 10, 9, 10, 0))
        assertTrue(r.single() is SmartResult.Event)
    }

    @Test fun multilineStaysNote() {
        val r = OfflineParser.parse("Einkauf\n- Milch 1,29\n- Brot 2,50", now = LocalDateTime.of(2026, 10, 9, 10, 0))
        assertTrue(r.single() is SmartResult.Note)
    }

    @Test fun plainTextIsNoMoney() = assertNull(MoneyTextParser.parse("Zahnarzt morgen 15 Uhr", today))

    @Test fun offlineParserReturnsTransaction() {
        val r = OfflineParser.parse("12,50 Döner gestern", now = LocalDateTime.of(2026, 10, 9, 10, 0))
        assertTrue(r.single() is SmartResult.Transaction)
    }
}
