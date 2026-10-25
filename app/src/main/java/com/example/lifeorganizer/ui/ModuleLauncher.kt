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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.zIndex
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A side module (everything besides calendar and notes) the ring can open. */
data class LauncherItem(val key: String, val label: String, val icon: ImageVector, val open: () -> Unit)

private val ITEM_SIZE = 52.dp

/** At most this many items share the quarter circle; with more the ring can be spun. */
const val RING_VISIBLE = 6

/** Degrees between neighbours on the quarter circle. */
fun ringStep(count: Int): Float = count.coerceAtMost(RING_VISIBLE).let { if (it > 1) 90f / (it - 1) else 0f }

/** How far the ring can be spun (negative degrees); 0 when everything fits. */
fun ringMinSpin(count: Int): Float = -(count - RING_VISIBLE).coerceAtLeast(0) * ringStep(count)

/** Angle of item [i] (90° = straight up, 180° = straight left), shifted by the spin. */
private fun ringAngle(i: Int, count: Int, spin: Float): Float = if (count == 1) 135f else 90f + ringStep(count) * i + spin

/** Only items on the quarter circle are shown and can be hit; spun-away ones are hidden. */
private fun onArc(angle: Float) = angle in 89f..181f

/** Centres of the ring's items around [center] (root px); null for items spun off the quarter circle. */
fun ringPositions(count: Int, center: Offset, radiusPx: Float, spin: Float = 0f): List<Offset?> = List(count) { i ->
    val deg = ringAngle(i, count, spin)
    if (!onArc(deg)) null else {
        val angle = Math.toRadians(deg.toDouble())
        Offset(center.x + (cos(angle) * radiusPx).toFloat(), center.y - (sin(angle) * radiusPx).toFloat())
    }
}

/** Wide enough that neighbours (circle plus label) are about 80 dp apart along the quarter circle. */
fun ringRadius(count: Int) = (80f * (count.coerceAtMost(RING_VISIBLE) - 1).coerceAtLeast(1) / (Math.PI.toFloat() / 2)).coerceAtLeast(130f).dp

/** Index of the ring item under [point] (root px), or -1. */
fun ringHit(count: Int, center: Offset, radiusPx: Float, point: Offset, hitPx: Float, spin: Float = 0f): Int =
    ringPositions(count, center, radiusPx, spin).indexOfFirst { it != null && (it - point).getDistance() <= hitPx }

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
 * The side modules fading in on a quarter circle around the switch at [center] (root px).
 * Tap an item to open it; hold one and drop it on another to swap them ([onSwap] with both keys);
 * with more than [RING_VISIBLE] items, dragging on the free space spins the ring ([spin], [onSpin]).
 * [hovered] marks the item under the finger while it comes from the switch. Tapping elsewhere closes the ring.
 */
@Composable
fun ModuleRing(
    items: List<LauncherItem>, open: Boolean, center: Offset, hovered: Int, spin: Float,
    onSpin: (Float) -> Unit, onSwap: (String, String) -> Unit, onPick: (Int) -> Unit, onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val radiusPx = with(density) { ringRadius(items.size).toPx() }
    val hitPx = with(density) { 40.dp.toPx() }
    val halfItem = with(density) { (ITEM_SIZE / 2).toPx() }
    val labelWidth = 84.dp
    val halfLabel = with(density) { (labelWidth / 2).toPx() }
    val positions = ringPositions(items.size, center, radiusPx, spin)
    // Item being moved by drag and drop, and where the finger is (root px)
    var dragging by remember { mutableIntStateOf(-1) }
    var dragAt by remember { mutableStateOf(Offset.Zero) }
    val dropTarget = if (dragging >= 0) ringHit(items.size, center, radiusPx, dragAt, hitPx, spin).takeIf { it != dragging } ?: -1 else -1
    val currentSpin by rememberUpdatedState(spin)
    val canSpin = items.size > RING_VISIBLE

    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        AnimatedVisibility(open, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Box(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss)
                    .then(
                        if (!canSpin) Modifier else Modifier.pointerInput(items.size) {
                            // Turning around the switch spins the items along the circle
                            detectDragGestures { change, _ ->
                                val now = origin + change.position
                                val before = now - (change.position - change.previousPosition)
                                fun deg(p: Offset) = Math.toDegrees(atan2(-(p.y - center.y), (p.x - center.x)).toDouble()).toFloat()
                                onSpin((currentSpin + deg(now) - deg(before)).coerceIn(ringMinSpin(items.size), 0f))
                            }
                        }
                    )
            )
        }
        positions.forEachIndexed { i, at ->
            if (at == null) return@forEachIndexed
            val item = items[i]
            val active = i == hovered || i == dropTarget
            val moving = i == dragging
            val shown = if (moving) dragAt else at
            AnimatedVisibility(
                open,
                enter = fadeIn(tween(160, delayMillis = i * 35)) + scaleIn(tween(160, delayMillis = i * 35), initialScale = 0.5f),
                exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.5f),
                modifier = Modifier
                    .zIndex(if (moving) 1f else 0f)
                    .offset { IntOffset((shown.x - origin.x - halfLabel).roundToInt(), (shown.y - origin.y - halfItem).roundToInt()) }
            ) {
                Column(
                    Modifier.width(labelWidth).alpha(if (moving) 0.85f else 1f)
                        .pointerInput(item.key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragging = i; dragAt = at },
                                onDrag = { change, amount -> change.consume(); dragAt += amount },
                                onDragEnd = {
                                    val target = ringHit(items.size, center, radiusPx, dragAt, hitPx, currentSpin)
                                    if (target >= 0 && target != i) onSwap(item.key, items[target].key)
                                    dragging = -1
                                },
                                onDragCancel = { dragging = -1 }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        onClick = { onPick(i) },
                        shape = CircleShape,
                        color = if (active || moving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (active || moving) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        shadowElevation = if (moving) 12.dp else 6.dp,
                        modifier = Modifier.size(if (active || moving) ITEM_SIZE + 8.dp else ITEM_SIZE)
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
