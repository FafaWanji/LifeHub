package com.example.lifeorganizer.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountsTest {
    @Test fun germanWithThousands() = assertEquals(123456L, Amounts.parseCents("1.234,56"))
    @Test fun germanNegative() = assertEquals(-1250L, Amounts.parseCents("-12,50"))
    @Test fun oneDecimal() = assertEquals(1250L, Amounts.parseCents("12,5"))
    @Test fun englishDecimal() = assertEquals(1250L, Amounts.parseCents("12.50"))
    @Test fun englishThousands() = assertEquals(123456L, Amounts.parseCents("1,234.56"))
    @Test fun germanThousandsOnly() = assertEquals(210000L, Amounts.parseCents("2.100"))
    @Test fun currencyAndSpaces() = assertEquals(1200L, Amounts.parseCents(" 12 € "))
    @Test fun eurSuffix() = assertEquals(-399L, Amounts.parseCents("-3,99 EUR"))
    @Test fun unicodeMinus() = assertEquals(-300L, Amounts.parseCents("−3,00"))
    @Test fun trailingMinus() = assertEquals(-1250L, Amounts.parseCents("12,50-"))
    @Test fun plusSign() = assertEquals(500L, Amounts.parseCents("+5"))
    @Test fun notANumber() = assertNull(Amounts.parseCents("EUR"))
    @Test fun empty() = assertNull(Amounts.parseCents(""))
    @Test fun text() = assertNull(Amounts.parseCents("Miete"))

    @Test fun formatGerman() =
        assertEquals("1.234,56 €", Amounts.format(123456, "de").replace(' ', ' ').replace(' ', ' '))

    @Test fun otherCurrencySigns() {
        assertEquals(1250L, Amounts.parseCents("$12.50"))
        assertEquals(-500L, Amounts.parseCents("-5 £"))
        assertEquals(1000L, Amounts.parseCents("10 CHF"))
    }

    @Test fun formatInTheChosenCurrency() {
        try {
            Amounts.currencyCode = "USD"
            // "$12.50" or "US$12.50" depending on the JDK's locale data
            val dollars = Amounts.format(1250, "en")
            assertTrue(dollars, dollars.contains("$") && dollars.contains("12.50"))
            Amounts.currencyCode = "nonsense"
            // Unknown codes fall back to euro
            assertEquals("1.234,56 €", Amounts.format(123456, "de").replace('\u00a0', ' ').replace('\u202f', ' '))
        } finally {
            Amounts.currencyCode = "EUR"
        }
    }

    @Test fun formatSigned() {
        assertEquals("+12,50 €", Amounts.formatSigned(1250, "de").replace(' ', ' ').replace(' ', ' '))
        assertEquals("−12,50 €", Amounts.formatSigned(-1250, "de").replace(' ', ' ').replace(' ', ' '))
    }
}
