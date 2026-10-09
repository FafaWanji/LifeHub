package com.example.lifeorganizer.money.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.Charset
import java.time.LocalDate

class CsvImporterTest {
    private fun bytes(name: String) = javaClass.classLoader!!.getResource("csv/$name")!!.readBytes()
    private fun parse(name: String) = CsvImporter.parse(bytes(name))

    @Test fun sparkasse() {
        val r = parse("sparkasse.csv")
        assertEquals(3, r.rows.size)
        assertEquals(CsvRow(LocalDate.of(2026, 10, 1), -80000, "Hausverwaltung Muster", "Miete Oktober"), r.rows[0])
        assertEquals(210000L, r.rows[2].amountCents)
    }

    @Test fun ingSkipsMetadataAndUsesBetragNotSaldo() {
        val r = parse("ing.csv")
        assertEquals(2, r.rows.size)
        assertEquals(-1399L, r.rows[0].amountCents)
        assertEquals("Netflix International B.V.", r.rows[0].payee)
        assertEquals(LocalDate.of(2026, 10, 5), r.rows[0].date)
    }

    @Test fun ingInWindows1252() {
        val text = String(bytes("ing.csv"), Charsets.UTF_8)
        val r = CsvImporter.parse(text.toByteArray(Charset.forName("windows-1252")))
        assertEquals(2, r.rows.size)
        assertEquals("Auftraggeber/Empfänger", r.header[2])
    }

    @Test fun dkbPicksCounterpartBySign() {
        val r = parse("dkb.csv")
        assertEquals("Spotify AB", r.rows[0].payee)
        assertEquals(-1099L, r.rows[0].amountCents)
        assertEquals("Muster AG", r.rows[1].payee)
        assertEquals(210000L, r.rows[1].amountCents)
    }

    @Test fun volksbankNewFormat() {
        val r = parse("volksbank.csv")
        assertEquals(CsvRow(LocalDate.of(2026, 10, 4), -6510, "Aral Tankstelle", "Tanken"), r.rows.single())
    }

    @Test fun volksbankOldFormatUsesSollHabenAndSkipsFooter() {
        val r = parse("volksbank_sh.csv")
        assertEquals(2, r.rows.size)
        assertEquals(-8500L, r.rows[0].amountCents)
        assertEquals("Stadtwerke Muster", r.rows[0].payee)
        assertEquals(210000L, r.rows[1].amountCents)
        assertEquals(3, r.skipped)
    }

    @Test fun n26CommaSeparatedIsoDates() {
        val r = parse("n26.csv")
        assertEquals(CsvRow(LocalDate.of(2026, 10, 6), -1840, "Lidl", ""), r.rows[0])
        assertEquals(210000L, r.rows[1].amountCents)
    }

    @Test fun commerzbankWithoutPayeeColumnUsesPurposeAsTitle() {
        val r = parse("commerzbank.csv")
        val row = r.rows.single()
        assertEquals("", row.payee)
        assertEquals("Vodafone GmbH Mobilfunk Rechnung 123", row.title)
        assertEquals(-3999L, row.amountCents)
    }

    @Test fun unknownFormatNeedsManualMapping() {
        val csv = "Wann;Wer;Wieviel\n05.10.2026;Kiosk;-2,50\n".toByteArray()
        val auto = CsvImporter.parse(csv)
        assertNull(auto.mapping)
        assertEquals(listOf("Wann", "Wer", "Wieviel"), auto.header)
        assertEquals(listOf("05.10.2026", "Kiosk", "-2,50"), auto.sample)

        val manual = CsvImporter.parse(csv, ColumnMapping(date = 0, amount = 2, payee = 1))
        assertEquals(CsvRow(LocalDate.of(2026, 10, 5), -250, "Kiosk", ""), manual.rows.single())
    }

    @Test fun mappingRoundTrip() {
        val m = ColumnMapping(date = 1, amount = 14, payee = 11, purpose = 4, sign = null)
        assertEquals(m, ColumnMapping.decode(m.encode()))
        assertNull(ColumnMapping.decode("garbage"))
    }

    @Test fun quotedDelimiterInsideField() {
        val csv = "Buchungstag;Empfänger;Betrag\n01.10.2026;\"Muster; Söhne\";-1,00\n".toByteArray()
        assertEquals("Muster; Söhne", CsvImporter.parse(csv).rows.single().payee)
    }

    @Test fun dates() {
        assertEquals(LocalDate.of(2026, 10, 1), CsvImporter.parseDate("01.10.26"))
        assertEquals(LocalDate.of(2026, 10, 1), CsvImporter.parseDate("01.10.2026"))
        assertEquals(LocalDate.of(2026, 10, 1), CsvImporter.parseDate("2026-10-01"))
        assertNull(CsvImporter.parseDate("Anfangssaldo"))
    }

    @Test fun emptyFile() {
        val r = CsvImporter.parse(ByteArray(0))
        assertNull(r.mapping)
        assertEquals(0, r.rows.size)
    }

    @Test fun headerKeyIsStable() {
        assertNotNull(CsvImporter.headerKey(listOf("Wann", "Wer")))
        assertEquals(CsvImporter.headerKey(listOf("Wann", "Wer")), CsvImporter.headerKey(listOf(" wann ", "WER")))
    }
}
