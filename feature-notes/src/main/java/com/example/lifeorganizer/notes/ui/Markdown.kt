package com.example.lifeorganizer.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------------------------
// Line model
// ---------------------------------------------------------------------------------------------

private val checkboxLine = Regex("^(\\s*)[-*+]\\s+\\[([ xX])]\\s?(.*)$")
private val bulletLine = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val numberedLine = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val headingLine = Regex("^(#{1,6})\\s+(.*)$")
private val quoteLine = Regex("^>\\s?(.*)$")
private val ruleLine = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")

sealed interface MdBlock {
    val lineIndex: Int
    data class Heading(override val lineIndex: Int, val level: Int, val text: String) : MdBlock
    data class Checkbox(override val lineIndex: Int, val indent: Int, val checked: Boolean, val text: String) : MdBlock
    data class Bullet(override val lineIndex: Int, val indent: Int, val text: String) : MdBlock
    data class Numbered(override val lineIndex: Int, val indent: Int, val number: String, val text: String) : MdBlock
    data class Quote(override val lineIndex: Int, val text: String) : MdBlock
    data class Rule(override val lineIndex: Int) : MdBlock
    data class Code(override val lineIndex: Int, val text: String) : MdBlock
    data class Paragraph(override val lineIndex: Int, val text: String) : MdBlock
    data class Blank(override val lineIndex: Int) : MdBlock
}

fun parseMarkdown(content: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = content.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            // Fenced code block: everything up to the closing fence.
            val start = i
            val body = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                body += lines[i]
                i++
            }
            blocks += MdBlock.Code(start, body.joinToString("\n"))
            i++
            continue
        }
        blocks += parseLine(i, line)
        i++
    }
    return blocks
}

private fun parseLine(index: Int, line: String): MdBlock {
    checkboxLine.matchEntire(line)?.let {
        return MdBlock.Checkbox(index, it.groupValues[1].length / 2, it.groupValues[2] != " ", it.groupValues[3])
    }
    headingLine.matchEntire(line)?.let { return MdBlock.Heading(index, it.groupValues[1].length, it.groupValues[2]) }
    if (ruleLine.matches(line)) return MdBlock.Rule(index)
    bulletLine.matchEntire(line)?.let { return MdBlock.Bullet(index, it.groupValues[1].length / 2, it.groupValues[2]) }
    numberedLine.matchEntire(line)?.let {
        return MdBlock.Numbered(index, it.groupValues[1].length / 2, it.groupValues[2], it.groupValues[3])
    }
    quoteLine.matchEntire(line)?.let { return MdBlock.Quote(index, it.groupValues[1]) }
    if (line.isBlank()) return MdBlock.Blank(index)
    return MdBlock.Paragraph(index, line)
}

/** Flips "- [ ]" ↔ "- [x]" on the given line. */
fun toggleCheckbox(content: String, lineIndex: Int): String {
    val lines = content.split("\n").toMutableList()
    if (lineIndex !in lines.indices) return content
    val match = checkboxLine.matchEntire(lines[lineIndex]) ?: return content
    val marker = if (match.groupValues[2] == " ") "[x]" else "[ ]"
    lines[lineIndex] = lines[lineIndex].replaceFirst(Regex("\\[[ xX]]"), marker)
    return lines.joinToString("\n")
}

/** Unchecks every "- [x]" item, e.g. to reuse a shopping list. */
fun uncheckAll(content: String): String =
    content.split("\n").joinToString("\n") { line ->
        if (checkboxLine.matches(line)) line.replaceFirst(Regex("\\[[xX]]"), "[ ]") else line
    }

data class ChecklistProgress(val done: Int, val total: Int)

fun checklistProgress(content: String): ChecklistProgress? {
    var done = 0
    var total = 0
    content.lineSequence().forEach { line ->
        checkboxLine.matchEntire(line)?.let {
            total++
            if (it.groupValues[2] != " ") done++
        }
    }
    return if (total == 0) null else ChecklistProgress(done, total)
}

fun hasChecklist(content: String) = content.lineSequence().any { checkboxLine.matches(it) }

/**
 * Converts the old checklist format ("[ ] item" / "[x] item") to Markdown task items so
 * notes written before the rewrite keep their checkboxes.
 */
/**
 * Fills placeholders of templates: {{date}}/{{datum}}, {{weekday}}/{{wochentag}}, {{time}}/{{uhrzeit}}.
 */
fun expandPlaceholders(text: String, lang: String, now: java.time.LocalDateTime = java.time.LocalDateTime.now()): String {
    if (!text.contains("{{")) return text
    val locale = java.util.Locale.forLanguageTag(lang)
    val date = now.toLocalDate().format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale))
    val weekday = now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, locale)
    val time = now.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    return Regex("\\{\\{\\s*(\\w+)\\s*}}").replace(text) { m ->
        when (m.groupValues[1].lowercase()) {
            "date", "datum", "fecha", "tarih" -> date
            "weekday", "wochentag", "dia", "gun" -> weekday
            "time", "uhrzeit", "hora", "saat" -> time
            else -> m.value
        }
    }
}

/** Moves checked items below the unchecked ones within each block of consecutive checklist lines. */
fun moveCheckedToBottom(content: String): String {
    val lines = content.split("\n")
    val out = mutableListOf<String>()
    var i = 0
    while (i < lines.size) {
        if (!checkboxLine.matches(lines[i])) { out += lines[i]; i++; continue }
        val block = mutableListOf<String>()
        while (i < lines.size && checkboxLine.matches(lines[i])) { block += lines[i]; i++ }
        val (done, open) = block.partition { checkboxLine.matchEntire(it)!!.groupValues[2] != " " }
        out += open + done
    }
    return out.joinToString("\n")
}

fun migrateLegacyChecklist(content: String): String =
    content.lines().joinToString("\n") { line ->
        when {
            line.startsWith("[ ] ") -> "- [ ] " + line.removePrefix("[ ] ")
            line.startsWith("[x] ") || line.startsWith("[X] ") -> "- [x] " + line.substring(4)
            else -> line
        }
    }

/** Words as a reader sees them: Markdown markers like "-", "[ ]" or "##" are not counted. */
fun countWords(content: String): Int =
    markdownToPlainText(content).split(Regex("\\s+")).count { word -> word.any { it.isLetterOrDigit() } }

/** Plain text for previews, search snippets and widgets. */
fun markdownToPlainText(content: String): String =
    parseMarkdown(content).mapNotNull { block ->
        when (block) {
            is MdBlock.Heading -> stripInline(block.text)
            is MdBlock.Checkbox -> (if (block.checked) "☑ " else "☐ ") + stripInline(block.text)
            is MdBlock.Bullet -> "• " + stripInline(block.text)
            is MdBlock.Numbered -> "${block.number}. " + stripInline(block.text)
            is MdBlock.Quote -> stripInline(block.text)
            is MdBlock.Code -> block.text
            is MdBlock.Paragraph -> stripInline(block.text)
            is MdBlock.Rule, is MdBlock.Blank -> null
        }
    }.joinToString("\n")

private fun stripInline(text: String): String =
    text.replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "$1")
        .replace(Regex("(\\*\\*|__|~~|`)"), "")
        .replace(Regex("(?<![\\w*])[*_](?=\\S)(.+?)(?<=\\S)[*_](?![\\w*])"), "$1")

// ---------------------------------------------------------------------------------------------
// Inline formatting
// ---------------------------------------------------------------------------------------------

private val inlineToken = Regex(
    "(\\*\\*(.+?)\\*\\*)|(__(.+?)__)|(~~(.+?)~~)|(`([^`]+)`)|(\\[([^\\]]+)]\\(([^)\\s]+)\\))|" +
        "((?<![\\w*])\\*(?=\\S)(.+?)(?<=\\S)\\*(?![\\w*]))|((?<![\\w_])_(?=\\S)(.+?)(?<=\\S)_(?![\\w_]))"
)

@Composable
fun inlineMarkdown(text: String): AnnotatedString {
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val linkColor = MaterialTheme.colorScheme.primary
    return buildInline(text, codeBg, linkColor)
}

private fun buildInline(text: String, codeBg: Color, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in inlineToken.findAll(text)) {
        append(text.substring(cursor, match.range.first))
        val g = match.groups
        when {
            g[2] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(buildInline(g[2]!!.value, codeBg, linkColor)) }
            g[4] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(buildInline(g[4]!!.value, codeBg, linkColor)) }
            g[6] != null -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(buildInline(g[6]!!.value, codeBg, linkColor)) }
            g[8] != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(" ${g[8]!!.value} ") }
            g[10] != null -> withLink(
                LinkAnnotation.Url(
                    g[11]!!.value,
                    TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                )
            ) { append(g[10]!!.value) }
            g[13] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(buildInline(g[13]!!.value, codeBg, linkColor)) }
            g[15] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(buildInline(g[15]!!.value, codeBg, linkColor)) }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

// ---------------------------------------------------------------------------------------------
// Rendered preview
// ---------------------------------------------------------------------------------------------

@Composable
fun MarkdownView(
    content: String,
    onToggleCheckbox: ((lineIndex: Int) -> Unit)?,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val blocks = parseMarkdown(content)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        blocks.forEach { block -> RenderBlock(block, onToggleCheckbox, textStyle) }
    }
}

@Composable
private fun RenderBlock(block: MdBlock, onToggleCheckbox: ((Int) -> Unit)?, textStyle: TextStyle) {
    val colors = MaterialTheme.colorScheme
    when (block) {
        is MdBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            Text(
                inlineMarkdown(block.text),
                style = style,
                modifier = Modifier.padding(top = if (block.level == 1) 12.dp else 8.dp, bottom = 4.dp)
            )
        }

        is MdBlock.Checkbox -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (block.indent * 20).dp)
                .then(if (onToggleCheckbox != null) Modifier.clickable { onToggleCheckbox(block.lineIndex) } else Modifier)
        ) {
            Checkbox(
                checked = block.checked,
                onCheckedChange = onToggleCheckbox?.let { toggle -> { _: Boolean -> toggle(block.lineIndex) } },
                modifier = Modifier.size(40.dp)
            )
            Text(
                inlineMarkdown(block.text),
                style = textStyle.copy(
                    textDecoration = if (block.checked) TextDecoration.LineThrough else null,
                    color = if (block.checked) colors.onSurfaceVariant else colors.onSurface
                )
            )
        }

        is MdBlock.Bullet -> ListRow(block.indent) {
            Box(Modifier.padding(top = 9.dp).size(6.dp).background(colors.primary, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(inlineMarkdown(block.text), style = textStyle)
        }

        is MdBlock.Numbered -> ListRow(block.indent) {
            Text("${block.number}.", style = textStyle.copy(color = colors.primary, fontWeight = FontWeight.SemiBold), modifier = Modifier.widthIn(min = 20.dp))
            Spacer(Modifier.width(6.dp))
            Text(inlineMarkdown(block.text), style = textStyle)
        }

        is MdBlock.Quote -> Row(Modifier.padding(vertical = 2.dp)) {
            Box(Modifier.width(3.dp).height(22.dp).background(colors.primary, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(12.dp))
            Text(inlineMarkdown(block.text), style = textStyle.copy(fontStyle = FontStyle.Italic, color = colors.onSurfaceVariant))
        }

        is MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 10.dp), color = colors.outlineVariant)

        is MdBlock.Code -> Text(
            block.text,
            style = textStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(colors.surfaceContainerHighest, RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        is MdBlock.Paragraph -> Text(inlineMarkdown(block.text), style = textStyle)
        is MdBlock.Blank -> Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ListRow(indent: Int, content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = (8 + indent * 20).dp, top = 2.dp, bottom = 2.dp)
    ) { content() }
}

// ---------------------------------------------------------------------------------------------
// Live syntax highlighting while editing (text is unchanged, only styled)
// ---------------------------------------------------------------------------------------------

class MarkdownHighlighter(
    private val markerColor: Color,
    private val accentColor: Color,
    private val codeBackground: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val styled = buildAnnotatedString {
            append(raw)
            var offset = 0
            raw.split("\n").forEach { line ->
                styleLine(line, offset)
                offset += line.length + 1
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.styleLine(line: String, start: Int) {
        val marker = SpanStyle(color = markerColor)
        headingLine.find(line)?.let {
            val size = when (it.groupValues[1].length) { 1 -> 24.sp; 2 -> 21.sp; else -> 18.sp }
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size), start, start + line.length)
            addStyle(marker, start, start + it.groupValues[1].length)
            return
        }
        checkboxLine.find(line)?.let {
            val markerEnd = line.indexOf(']') + 1
            addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), start, start + markerEnd)
            if (it.groupValues[2] != " ") {
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = markerColor), start + markerEnd, start + line.length)
            }
        } ?: bulletLine.find(line)?.let {
            val markerEnd = it.groupValues[1].length + 1
            addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), start, start + markerEnd)
        } ?: numberedLine.find(line)?.let {
            val markerEnd = line.indexOfFirst { c -> c == '.' || c == ')' } + 1
            addStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold), start, start + markerEnd)
        } ?: quoteLine.find(line)?.let {
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, start + line.length)
            addStyle(SpanStyle(color = accentColor), start, start + 1)
        }

        inlineToken.findAll(line).forEach { m ->
            val s = start + m.range.first
            val e = start + m.range.last + 1
            val g = m.groups
            when {
                g[2] != null || g[4] != null -> {
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, e)
                    addStyle(marker, s, s + 2); addStyle(marker, e - 2, e)
                }
                g[6] != null -> {
                    addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), s, e)
                    addStyle(marker, s, s + 2); addStyle(marker, e - 2, e)
                }
                g[8] != null -> addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground), s, e)
                g[10] != null -> addStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline), s, e)
                g[13] != null || g[15] != null -> {
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), s, e)
                    addStyle(marker, s, s + 1); addStyle(marker, e - 1, e)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Editing helpers
// ---------------------------------------------------------------------------------------------

/**
 * When Enter is pressed at the end of a list item, continue the list ("- [ ] ", "- ", "2. ").
 * Pressing Enter on an empty item ends the list instead.
 */
fun continueListOnNewline(old: String, new: String, cursor: Int): Pair<String, Int>? {
    if (cursor <= 0 || cursor > new.length || new[cursor - 1] != '\n') return null
    // Keyboards often commit the composing (auto-corrected) word together with the newline, so the
    // edit can be longer than one char. Require exactly one new line break, typed at the cursor.
    if (new.count { it == '\n' } != old.count { it == '\n' } + 1) return null
    if (!old.endsWith(new.substring(cursor))) return null
    val lineStart = new.lastIndexOf('\n', cursor - 2) + 1
    val previous = new.substring(lineStart, cursor - 1)

    val continuation = checkboxLine.matchEntire(previous)?.let {
        if (it.groupValues[3].isBlank()) "" to true else "${it.groupValues[1]}- [ ] " to false
    } ?: bulletLine.matchEntire(previous)?.let {
        val bullet = previous.trimStart().first()
        if (it.groupValues[2].isBlank()) "" to true else "${it.groupValues[1]}$bullet " to false
    } ?: numberedLine.matchEntire(previous)?.let {
        if (it.groupValues[3].isBlank()) "" to true
        else "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}. " to false
    } ?: return null

    val (prefix, endList) = continuation
    return if (endList) {
        // Remove the empty marker line and the newline just typed.
        val text = new.substring(0, lineStart) + new.substring(cursor)
        text to lineStart
    } else {
        val text = new.substring(0, cursor) + prefix + new.substring(cursor)
        text to cursor + prefix.length
    }
}
