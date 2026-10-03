package com.example.lifeorganizer.notes.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {

    @Test
    fun `checkbox lines are parsed with state and line index`() {
        val blocks = parseMarkdown("# Einkauf\n- [ ] Milch\n- [x] Brot\nText")
        assertTrue(blocks[0] is MdBlock.Heading)
        val milk = blocks[1] as MdBlock.Checkbox
        val bread = blocks[2] as MdBlock.Checkbox
        assertEquals(1, milk.lineIndex)
        assertFalse(milk.checked)
        assertEquals("Milch", milk.text)
        assertTrue(bread.checked)
        assertTrue(blocks[3] is MdBlock.Paragraph)
    }

    @Test
    fun `plain bullets are not mistaken for checkboxes`() {
        val blocks = parseMarkdown("- item\n* other")
        assertTrue(blocks.all { it is MdBlock.Bullet })
    }

    @Test
    fun `toggleCheckbox flips only the given line`() {
        val content = "- [ ] a\n- [x] b\n- [ ] c"
        assertEquals("- [x] a\n- [x] b\n- [ ] c", toggleCheckbox(content, 0))
        assertEquals("- [ ] a\n- [ ] b\n- [ ] c", toggleCheckbox(content, 1))
        // Non-checkbox line or out of range: unchanged
        assertEquals("text", toggleCheckbox("text", 0))
        assertEquals(content, toggleCheckbox(content, 9))
    }

    @Test
    fun `checklist progress counts done items`() {
        assertEquals(ChecklistProgress(1, 3), checklistProgress("- [x] a\n- [ ] b\n* [ ] c\nplain"))
        assertNull(checklistProgress("no tasks here"))
    }

    @Test
    fun `legacy checklist format is migrated to markdown tasks`() {
        assertEquals("- [ ] Milch\n- [x] Brot\nNotiz", migrateLegacyChecklist("[ ] Milch\n[x] Brot\nNotiz"))
    }

    @Test
    fun `plain text strips markdown syntax`() {
        val plain = markdownToPlainText("## Titel\n**fett** und *kursiv* mit [Link](https://x.de)\n- [x] erledigt\n- Punkt")
        assertEquals("Titel\nfett und kursiv mit Link\n☑ erledigt\n• Punkt", plain)
    }

    @Test
    fun `enter after checklist item continues the list`() {
        val old = "- [x] Milch"
        val new = "- [x] Milch\n"
        val (text, cursor) = continueListOnNewline(old, new, new.length)!!
        assertEquals("- [x] Milch\n- [ ] ", text)
        assertEquals(text.length, cursor)
    }

    @Test
    fun `enter on empty list item ends the list`() {
        val old = "- [ ] Milch\n- [ ] "
        val new = "$old\n"
        val (text, cursor) = continueListOnNewline(old, new, new.length)!!
        assertEquals("- [ ] Milch\n", text)
        assertEquals(text.length, cursor)
    }

    @Test
    fun `numbered lists increment`() {
        val old = "1. eins"
        val new = "1. eins\n"
        assertEquals("1. eins\n2. ", continueListOnNewline(old, new, new.length)!!.first)
    }

    @Test
    fun `normal newline is left alone`() {
        assertNull(continueListOnNewline("Hallo", "Hallo\n", 6))
    }

    @Test
    fun `bold wraps the selection`() {
        val result = applyFormat(TextFieldValue("Hallo Welt", TextRange(6, 10)), FormatAction.BOLD)
        assertEquals("Hallo **Welt**", result.text)
        assertEquals(TextRange(8, 12), result.selection)
    }

    @Test
    fun `checkbox action toggles the line prefix`() {
        val added = applyFormat(TextFieldValue("Milch", TextRange(2)), FormatAction.CHECKBOX)
        assertEquals("- [ ] Milch", added.text)
        val removed = applyFormat(TextFieldValue(added.text, TextRange(8)), FormatAction.CHECKBOX)
        assertEquals("Milch", removed.text)
    }

    @Test
    fun `heading action applies to the current line only`() {
        val result = applyFormat(TextFieldValue("eins\nzwei", TextRange(7)), FormatAction.HEADING)
        assertEquals("eins\n## zwei", result.text)
    }

    @Test
    fun `word count ignores markdown markers`() {
        assertEquals(6, countWords("Einkauf **wichtig**\n- [ ] Milch\n- [x] Brot\n- [ ] Eier\n## Ende"))
    }
}
