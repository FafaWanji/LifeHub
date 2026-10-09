package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The one look for "delete" across LifeHub: red outline with a bin icon where deleting is offered,
 * filled red where a deletion is confirmed.
 */
@Composable
fun DeleteButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 0.6f else 0.2f))
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

/** Bin icon for delete actions in lists and toolbars, same red as [DeleteButton]. */
@Composable
fun DeleteIconButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Delete, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.error)
    }
}

/** Confirm button of a "really delete?" dialog. */
@Composable
fun ConfirmDeleteButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}
