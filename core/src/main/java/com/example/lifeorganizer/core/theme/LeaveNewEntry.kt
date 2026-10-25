package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text

/**
 * Asked when a new, not yet saved entry is left with back: save it, throw it away or keep editing.
 * Back on this question throws the entry away. [onSave] null: the entry is not complete enough to save.
 */
@Composable
fun LeaveNewEntryDialog(onSave: (() -> Unit)?, onDiscard: () -> Unit, onKeep: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDiscard,
        title = { Text(Str.leaveNewEntry.text()) },
        text = { Text(Str.leaveNewEntryHint.text()) },
        confirmButton = {
            Row {
                TextButton(onClick = onDiscard) { Text(Str.discard.text()) }
                if (onSave != null) TextButton(onClick = onSave) { Text(Str.save.text()) }
            }
        },
        dismissButton = { TextButton(onClick = onKeep) { Text(Str.keepEditing.text()) } }
    )
}
