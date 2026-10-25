package com.example.lifeorganizer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A side module (everything besides calendar and notes) the launcher can open. */
data class LauncherItem(val key: String, val label: String, val icon: ImageVector, val open: () -> Unit)

/**
 * Button that unfolds the side modules above it. Tap: open / close the list, then tap a module.
 * Hold and drag: the list opens, slide onto a module and let go to open it (letting go elsewhere opens nothing).
 */
@Composable
fun ModuleLauncher(items: List<LauncherItem>, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, label: String) {
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var hovered by remember { mutableStateOf<String?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current
    val isExpanded by rememberUpdatedState(expanded)
    val currentItems by rememberUpdatedState(items)

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    val active = hovered == item.key
                    Surface(
                        onClick = { onExpandedChange(false); item.open() },
                        shape = RoundedCornerShape(50),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        shadowElevation = 4.dp,
                        modifier = Modifier.onGloballyPositioned { bounds[item.key] = it.boundsInRoot() }
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.icon, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(item.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        // Own gesture handling instead of a FAB's click, so a hold can turn into a drag
        Surface(
            shape = CircleShape,
            color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(40.dp)
                .onGloballyPositioned { origin = it.positionInRoot() }
                .semantics {
                    role = Role.Button
                    contentDescription = label
                    onClick { onExpandedChange(!isExpanded); true }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
                        if (up != null) {
                            onExpandedChange(!isExpanded)
                            return@awaitEachGesture
                        }
                        // Held: show the list and follow the finger
                        onExpandedChange(true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val at = origin + change.position
                            val over = bounds.entries.firstOrNull { it.value.contains(at) }?.key
                            if (over != hovered && over != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            hovered = over
                            change.consume()
                            if (!change.pressed) break
                        }
                        val pick = hovered
                        hovered = null
                        if (pick != null) {
                            onExpandedChange(false)
                            currentItems.firstOrNull { it.key == pick }?.open?.invoke()
                        }
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (expanded) Icons.Default.Close else Icons.Default.Apps, null, Modifier.size(22.dp))
            }
        }
    }
}
