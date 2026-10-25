package com.example.lifeorganizer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A side module (everything besides calendar and notes) the ring can open. */
data class LauncherItem(val key: String, val label: String, val icon: ImageVector, val open: () -> Unit)

private val ITEM_SIZE = 52.dp

/** Centres of the ring's items around [center] (root px): a quarter circle from straight up to straight left. */
fun ringPositions(count: Int, center: Offset, radiusPx: Float): List<Offset> = List(count) { i ->
    val angle = Math.toRadians(if (count == 1) 135.0 else 90.0 + 90.0 * i / (count - 1))
    Offset(center.x + (cos(angle) * radiusPx).toFloat(), center.y - (sin(angle) * radiusPx).toFloat())
}

/** Wide enough that neighbours (circle plus label) are about 80 dp apart along the quarter circle. */
fun ringRadius(count: Int) = (80f * (count - 1).coerceAtLeast(1) / (Math.PI.toFloat() / 2)).coerceAtLeast(130f).dp

/**
 * The calendar/notes switch's gesture. Tap: [onTap]. Hold: [onHoldStart], then the finger is followed
 * ([onHoldMove], root px) until it lifts ([onHoldEnd]).
 */
@Composable
fun Modifier.switchGesture(
    onTap: () -> Unit, onHoldStart: () -> Unit, onHoldMove: (Offset) -> Unit, onHoldEnd: (Offset) -> Unit
): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val tap by rememberUpdatedState(onTap)
    val start by rememberUpdatedState(onHoldStart)
    val move by rememberUpdatedState(onHoldMove)
    val end by rememberUpdatedState(onHoldEnd)
    return this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
                if (up != null) { tap(); return@awaitEachGesture }
                start()
                var last = origin + down.position
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    last = origin + change.position
                    change.consume()
                    if (!change.pressed) break
                    move(last)
                }
                end(last)
            }
        }
}

/**
 * The side modules fading in on a quarter circle around the switch at [center] (root px). Items can be
 * tapped; [hovered] marks the one under the finger while dragging. Tapping elsewhere closes the ring.
 */
@Composable
fun ModuleRing(items: List<LauncherItem>, open: Boolean, center: Offset, hovered: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val radiusPx = with(density) { ringRadius(items.size).toPx() }
    val halfItem = with(density) { (ITEM_SIZE / 2).toPx() }
    val labelWidth = 84.dp
    val halfLabel = with(density) { (labelWidth / 2).toPx() }

    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        AnimatedVisibility(open, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Box(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss)
            )
        }
        ringPositions(items.size, center, radiusPx).forEachIndexed { i, at ->
            val item = items[i]
            val active = i == hovered
            AnimatedVisibility(
                open,
                enter = fadeIn(tween(160, delayMillis = i * 35)) + scaleIn(tween(160, delayMillis = i * 35), initialScale = 0.5f),
                exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.5f),
                modifier = Modifier.offset { IntOffset((at.x - origin.x - halfLabel).roundToInt(), (at.y - origin.y - halfItem).roundToInt()) }
            ) {
                Column(Modifier.width(labelWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        onClick = { onPick(i) },
                        shape = CircleShape,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(if (active) ITEM_SIZE + 8.dp else ITEM_SIZE)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(item.icon, item.label, Modifier.size(24.dp)) }
                    }
                    Text(
                        item.label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 3.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/** Index of the ring item under [point] (root px), or -1. */
fun ringHit(count: Int, center: Offset, radiusPx: Float, point: Offset, hitPx: Float): Int =
    ringPositions(count, center, radiusPx).indexOfFirst { (it - point).getDistance() <= hitPx }
