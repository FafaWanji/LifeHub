package com.example.lifeorganizer.ui

import com.example.lifeorganizer.core.i18n.SupportedLanguages
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChangelogTest {
    @Test fun everyEntryExistsInEveryLanguage() {
        CHANGELOG.forEach { release ->
            release.items.forEach { item ->
                SupportedLanguages.forEach { lang ->
                    assertTrue("${release.version} [$lang] empty: ${item.en}", item.of(lang).isNotBlank())
                }
                assertTrue("${release.version} not Chinese: ${item.en}", item.zh.any { it in '一'..'鿿' })
            }
        }
    }

    @Test fun releaseNotesHaveAllLanguages() {
        val notes = ReleaseNotes.markdown(CHANGELOG.first().version)
        listOf("### Deutsch", "### English", "### Türkçe", "### Español", "### 简体中文").forEach {
            assertTrue("missing $it", notes.contains(it))
        }
        // Handy for releases: notes for every version end up in app/build/release-notes/<version>.md
        val dir = File("build/release-notes").apply { mkdirs() }
        CHANGELOG.forEach { r ->
            File(dir, "${r.version}.md").writeText(ReleaseNotes.markdown(r.version, splitApks = r.version !in setOf("0.8", "0.9")))
        }
    }
}
