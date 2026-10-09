package com.example.lifeorganizer.money.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** Ring of [slices] (color, value); small gaps between segments. */
@Composable
fun DonutChart(slices: List<Pair<Color, Long>>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1)
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        val arc = Size(size.minDimension - stroke, size.minDimension - stroke)
        val topLeft = Offset((size.width - arc.width) / 2, (size.height - arc.height) / 2)
        var start = -90f
        slices.forEach { (color, value) ->
            val sweep = 360f * value / total
            val gap = if (slices.size > 1 && sweep > 2f) 1.5f else 0f
            drawArc(color, start + gap / 2, sweep - gap, false, topLeft, arc, style = Stroke(stroke))
            start += sweep
        }
    }
}
