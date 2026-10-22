package com.example.lifeorganizer.core.theme

import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.lifeorganizer.core.settings.MicroSettings
import com.example.lifeorganizer.core.settings.rememberMicro

private val rainbow = listOf(
    Color(0xFFFF4D6D), Color(0xFFFFB86B), Color(0xFFF9F871), Color(0xFF5CF2A6),
    Color(0xFF4CC9F0), Color(0xFF7B61FF), Color(0xFFE056FD), Color(0xFFFF4D6D)
).map { it.toArgb() }.toIntArray()

/**
 * A slowly turning rainbow glow drawn around the element, outside its edges. Needs room to shine:
 * it pads the element by [spread]. Blurred from Android 9 on; before that a plain coloured ring.
 */
@Composable
fun Modifier.rgbGlow(colors: IntArray = rainbow, corner: Dp = 28.dp, spread: Dp = 16.dp): Modifier {
    val angle by rememberInfiniteTransition(label = "rgbGlow").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "angle"
    )
    return this
        .drawBehind {
            val inset = spread.toPx() * 0.55f
            val radius = corner.toPx()
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    maskFilter = BlurMaskFilter(spread.toPx() * 0.7f, BlurMaskFilter.Blur.NORMAL)
                    shader = SweepGradient(center.x, center.y, colors, null).apply {
                        // Read here, so the animation only redraws instead of recomposing
                        setLocalMatrix(Matrix().apply { setRotate(angle, center.x, center.y) })
                    }
                    alpha = 210
                }
                canvas.nativeCanvas.drawRoundRect(inset, inset, size.width - inset, size.height - inset, radius, radius, paint)
            }
        }
        .padding(spread)
}

/** The glow colours as set in the appearance settings, null when off. */
@Composable
fun glowColors(): IntArray? {
    val mode = rememberMicro(MicroSettings.GLOW_MODE)
    val custom = rememberMicro(MicroSettings.GLOW_COLOR)
    val scheme = MaterialTheme.colorScheme
    return when (mode) {
        // The design's own colours, going round
        "theme" -> listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary).map { it.toArgb() }.toIntArray()
        "custom" -> if (custom == 0) rainbow else {
            // One colour, a lighter and a deeper shade of it going round
            val c = Color(custom)
            listOf(c, lerp(c, Color.White, 0.45f), c, lerp(c, Color.Black, 0.35f), c).map { it.toArgb() }.toIntArray()
        }
        else -> null
    }
}

/**
 * Material's AlertDialog, plus the optional RGB glow from the appearance settings. Same parameters, so
 * screens use it by importing this one instead of androidx.compose.material3.AlertDialog.
 */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    val colors = glowColors()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = if (colors != null) Modifier.rgbGlow(colors).then(modifier) else modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}
