package com.example.lifeorganizer.waypoints.ui

import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.WpStr
import com.example.lifeorganizer.core.i18n.text
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.lifeorganizer.waypoints.data.WaypointWithLabels

@Composable
fun WaypointCard(
    waypointWithLabels: WaypointWithLabels,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onMarkUsed: () -> Unit,
    cardScale: Float,
    useHomeAsOrigin: Boolean = false,
    homeAddress: String = "",
    transportMode: String = "driving",
    hideAddress: Boolean = false,
    modifier: Modifier = Modifier,
    onPlanEvent: (() -> Unit)? = null
) {
    val waypoint = waypointWithLabels.waypoint
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val themeStyle = com.example.lifeorganizer.core.theme.LocalThemeStyle.current

    CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density * cardScale, LocalDensity.current.fontScale * cardScale)
    ) {
        val labelColorStr = waypointWithLabels.labels.firstOrNull()?.colorHex
        val borderColor = if (labelColorStr != null) {
            try {
                Color(android.graphics.Color.parseColor(labelColorStr))
            } catch (e: Exception) {
                Color.Transparent
            }
        } else {
            Color.Transparent
        }

        val shortAddress = if (hideAddress) {
            "••••••••••••••••"
        } else {
            waypoint.address
                .replace(Regex("-Bezirk[\\w\\s-]+"), "")
                .replace("Deutschland", "DE")
                .replace("Germany", "DE")
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = waypoint.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (waypointWithLabels.labels.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            val firstLabel = waypointWithLabels.labels.first()
                            val customColor = if (borderColor != Color.Transparent) borderColor else MaterialTheme.colorScheme.primary

                            val labelBgColor: Color
                            val labelTextColor: Color
                            val labelBorderColor: Color?
                            val labelShape: androidx.compose.ui.graphics.Shape
                            val labelPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                            when (themeStyle) {
                                "neon" -> {
                                    labelBgColor = MaterialTheme.colorScheme.surface
                                    labelTextColor = MaterialTheme.colorScheme.primary
                                    labelBorderColor = MaterialTheme.colorScheme.primary
                                    labelShape = androidx.compose.foundation.shape.RoundedCornerShape(50) // Pill
                                }
                                "midnight" -> {
                                    labelBgColor = Color(0xFF1A1A1A)
                                    labelTextColor = Color(0xFF808080)
                                    labelBorderColor = null
                                    labelShape = androidx.compose.foundation.shape.RoundedCornerShape(50) // Pill
                                }
                                "white_light" -> {
                                    labelBgColor = Color(0xFFF0F0F0)
                                    labelTextColor = Color(0xFF555555)
                                    labelBorderColor = null
                                    labelShape = androidx.compose.foundation.shape.RoundedCornerShape(50) // Pill
                                }
                                else -> { // pastel
                                    labelBgColor = customColor.copy(alpha = 0.2f)
                                    labelTextColor = MaterialTheme.colorScheme.onSurface
                                    labelBorderColor = null
                                    labelShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                }
                            }

                            Surface(
                                color = labelBgColor,
                                shape = labelShape,
                                border = if (labelBorderColor != null) androidx.compose.foundation.BorderStroke(1.dp, labelBorderColor) else null
                            ) {
                                Text(
                                    text = firstLabel.name,
                                    modifier = Modifier.padding(labelPadding),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = labelTextColor
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = shortAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    if (waypoint.notes.isNotBlank()) {
                        Text(
                            text = waypoint.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (waypoint.isPinned) Icons.Filled.LocationOn else Icons.Outlined.LocationOn,
                            contentDescription = if (waypoint.isPinned) WpStr.unpin.text() else WpStr.pin.text(),
                            tint = if (waypoint.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = WpStr.moreOptions.text(), modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (onPlanEvent != null) {
                                DropdownMenuItem(text = { Text(WpStr.planEvent.text()) }, onClick = { showMenu = false; onPlanEvent() })
                            }
                            DropdownMenuItem(text = { Text(Str.edit.text()) }, onClick = { showMenu = false; onEdit() })
                            DropdownMenuItem(text = { Text(Str.delete.text()) }, onClick = { showMenu = false; showDeleteConfirm = true })
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    IconButton(
                        onClick = {
                            onMarkUsed()
                            val travelModeStr = when (transportMode) {
                                "car" -> "driving"
                                "bike" -> "bicycling"
                                "transit" -> "transit"
                                else -> "driving"
                            }
                            val uriStr = if (useHomeAsOrigin && homeAddress.isNotBlank()) {
                                "https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(homeAddress)}&destination=${Uri.encode(waypoint.address)}&travelmode=$travelModeStr"
                            } else {
                                "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(waypoint.address)}&travelmode=$travelModeStr"
                            }
                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                            mapIntent.setPackage("com.google.android.apps.maps")
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)))
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = WpStr.navigate.text(), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(WpStr.deleteTitle.text().format(waypoint.name)) },
            text = { Text(WpStr.deleteMessage.text()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Str.delete.text())
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Str.cancel.text())
                }
            }
        )
    }
}
