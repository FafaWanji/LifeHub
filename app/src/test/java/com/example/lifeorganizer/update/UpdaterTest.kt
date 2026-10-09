package com.example.lifeorganizer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun `versions compare numerically`() {
        assertTrue(Updater.isNewer("0.10", "0.9"))
        assertTrue(Updater.isNewer("1.0", "0.9.5"))
        assertFalse(Updater.isNewer("0.9", "0.9"))
        assertFalse(Updater.isNewer("0.8", "0.9"))
    }

    private val notes = "## LifeHub 1.1\n\n### Deutsch\n\n- Neu: Finanzen\n\n**Welche Datei?** arm64\n\n### English\n\n- New: Finance\n\n### 简体中文\n\n- 新功能：财务\n"

    @Test fun notesShowOnlyTheAppLanguage() {
        assertEquals("- Neu: Finanzen\n\n**Welche Datei?** arm64", Updater.notesFor(notes, "de"))
        assertEquals("- 新功能：财务", Updater.notesFor(notes, "zh"))
    }

    @Test fun missingLanguageFallsBackToEnglish() =
        assertEquals("- New: Finance", Updater.notesFor(notes, "tr"))

    @Test fun oldSingleLanguageNotesStayAsTheyAre() =
        assertEquals("- Alles neu", Updater.notesFor("- Alles neu", "en"))
}
