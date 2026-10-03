package com.example.lifeorganizer.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ThemedLabelChip(
    text: String,
    customColor: Color,
    modifier: Modifier = Modifier
) {
    val designStyle = LocalDesignStyle.current

    when (designStyle) {
        "neon" -> {
            val neonColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, neonColor, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = neonColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        "midnight" -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF808080),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        "white_light" -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF0F0F0))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF555555),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        else -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(customColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
