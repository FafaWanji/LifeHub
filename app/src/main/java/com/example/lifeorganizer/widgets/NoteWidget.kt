package com.example.lifeorganizer.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.lifeorganizer.MainActivity
import com.example.lifeorganizer.R
import com.example.lifeorganizer.notes.data.Note
import com.example.lifeorganizer.notes.data.NotesDatabase
import com.example.lifeorganizer.notes.ui.toggleCheckbox

/**
 * Shows the most recently edited pinned note. Checklist items can be ticked right on the home screen.
 */
class NoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val note = pinnedNote(context)
        provideContent { Content(context, note) }
    }

    @Composable
    private fun Content(context: Context, note: Note?) {
        val surface = ColorProvider(R.color.widget_surface)
        val accent = ColorProvider(R.color.widget_accent)
        val muted = ColorProvider(R.color.widget_muted)
        Column(
            modifier = GlanceModifier.fillMaxSize().background(surface).cornerRadius(20.dp).padding(14.dp)
        ) {
            if (note == null) {
                Text(
                    context.getString(R.string.note_widget_empty),
                    style = TextStyle(color = muted, fontSize = 14.sp),
                    modifier = GlanceModifier.clickable(actionStartActivity(openApp(context, null)))
                )
                return@Column
            }
            Text(
                note.title.ifBlank { context.getString(R.string.note_widget_untitled) },
                maxLines = 1,
                style = TextStyle(color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp)
                    .clickable(actionStartActivity(openApp(context, note.id)))
            )
            val lines = note.content.split("\n").withIndex().filter { it.value.isNotBlank() }.toList()
            LazyColumn {
                items(lines, itemId = { it.index.toLong() }) { (index, line) ->
                    val box = CHECKBOX.matchEntire(line)
                    if (box != null) {
                        val done = box.groupValues[1] != " "
                        Text(
                            (if (done) "☑ " else "☐ ") + box.groupValues[2],
                            style = TextStyle(
                                color = if (done) muted else ColorProvider(R.color.widget_on_surface),
                                fontSize = 14.sp,
                                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(
                                actionRunCallback<ToggleItem>(actionParametersOf(NOTE_ID to note.id, LINE to index))
                            )
                        )
                    } else {
                        Text(
                            line.trimStart('#', ' ', '-', '*'),
                            maxLines = 2,
                            style = TextStyle(color = ColorProvider(R.color.widget_on_surface), fontSize = 14.sp),
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)
                                .clickable(actionStartActivity(openApp(context, note.id)))
                        )
                    }
                }
            }
        }
    }

    private fun openApp(context: Context, noteId: Long?) = Intent(context, MainActivity::class.java).apply {
        action = if (noteId != null) MainActivity.ACTION_OPEN_NOTE else MainActivity.ACTION_NEW_NOTE
        noteId?.let { putExtra(com.example.lifeorganizer.calendar.alarm.NotificationHelper.EXTRA_NOTE_ID, it) }
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    companion object {
        private val CHECKBOX = Regex("^\\s*[-*+]\\s+\\[([ xX])]\\s?(.*)$")
        val NOTE_ID = ActionParameters.Key<Long>("noteId")
        val LINE = ActionParameters.Key<Int>("line")

        suspend fun pinnedNote(context: Context): Note? =
            NotesDatabase.getDatabase(context).noteDao().getAllNotesWithLabelsSync()
                .map { it.note }
                .filter { it.isPinned && !it.isDeleted }
                .maxByOrNull { it.updatedAt }

        suspend fun refresh(context: Context) = runCatching { NoteWidget().updateAll(context) }
    }
}

class ToggleItem : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[NoteWidget.NOTE_ID] ?: return
        val line = parameters[NoteWidget.LINE] ?: return
        val dao = NotesDatabase.getDatabase(context).noteDao()
        val note = dao.getNoteWithLabels(id)?.note ?: return
        // Keep updatedAt so ticking items does not change which note the widget shows.
        dao.updateNote(note.copy(content = toggleCheckbox(note.content, line)))
        NoteWidget().updateAll(context)
    }
}

class NoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteWidget()
}
