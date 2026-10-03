package com.example.lifeorganizer.notes.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.core.i18n.LocalAppLanguage
import com.example.lifeorganizer.notes.data.NoteWithLabels
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    noteWithLabels: NoteWithLabels,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false
) {
    val note = noteWithLabels.note
    val lang = LocalAppLanguage.current
    val accent = note.colorLabel?.let { Color(it) }
    val container = accent?.let { tintFor(it) } ?: MaterialTheme.colorScheme.surfaceContainer
    val preview = remember(note.content) { markdownToPlainText(note.content) }
    val progress = remember(note.content) { checklistProgress(note.content) }
    val formatter = remember(lang) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container),
        border = when {
            selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            accent == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            else -> null
        }
    ) {
      Box {
        Column(modifier = Modifier.padding(14.dp)) {
            if (note.title.isNotBlank() || note.isPinned) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp).size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            progress?.let { p ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckBox, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { p.done.toFloat() / p.total },
                        modifier = Modifier.weight(1f).height(4.dp),
                        drawStopIndicator = {}
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("${p.done}/${p.total}", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (noteWithLabels.labels.isNotEmpty() || note.pinnedToDate != null) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    note.pinnedToDate?.let { millis ->
                        MiniChip(
                            text = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter),
                            dot = null,
                            icon = true
                        )
                    }
                    noteWithLabels.labels.forEach { label -> MiniChip(label.name, Color(label.color)) }
                }
            }
        }
        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
      }
    }
}

@Composable
private fun MiniChip(text: String, dot: Color?, icon: Boolean = false) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon) Icon(Icons.Default.Event, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
        dot?.let { Box(Modifier.size(8.dp).background(it, CircleShape)) }
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
