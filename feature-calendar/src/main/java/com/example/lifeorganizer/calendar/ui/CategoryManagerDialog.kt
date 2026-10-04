package com.example.lifeorganizer.calendar.ui

import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.i18n.Str
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lifeorganizer.calendar.data.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onAdd: (name: String, color: Int) -> Unit,
    onDelete: (Category) -> Unit,
    onClose: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(Str.manageCategories.text(), style = MaterialTheme.typography.titleMedium) },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = Str.addCategory.text())
                        }
                        TextButton(onClick = onClose) { Text(Str.done2.text()) }
                    }
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        ListItem(
                            headlineContent = { Text(text = category.name) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(category.color))
                                )
                            },
                            trailingContent = {
                                if (!category.isDefault) {
                                    IconButton(onClick = { onDelete(category) }) {
                                        Icon(Icons.Default.Delete, contentDescription = Str.delete2.text(), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onSave = { name, color ->
                onAdd(name, color)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    onSave: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colors = listOf(
        0xFFF8BBD0.toInt(), 0xFFE1BEE7.toInt(), 0xFFD1C4E9.toInt(),
        0xFFC5CAE9.toInt(), 0xFFBBDEFB.toInt(), 0xFFB2EBF2.toInt(),
        0xFFB2DFDB.toInt(), 0xFFC8E6C9.toInt(), 0xFFDCEDC8.toInt(),
        0xFFFFF9C4.toInt(), 0xFFFFE0B2.toInt(), 0xFFFFCCBC.toInt()
    )
    var selectedColor by remember { mutableStateOf(colors[4]) }
    var showCustomPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Str.newCategory.text()) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Str.categoryName.text()) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(Str.selectColor.text(), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.take(6).forEach { colorInt ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .clickable { selectedColor = colorInt }
                                .padding(4.dp)
                        ) {
                            if (selectedColor == colorInt) {
                                Box(modifier = Modifier.matchParentSize().background(Color.White.copy(alpha=0.5f), CircleShape))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.drop(6).take(6).forEach { colorInt ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .clickable { selectedColor = colorInt }
                                .padding(4.dp)
                        ) {
                            if (selectedColor == colorInt) {
                                Box(modifier = Modifier.matchParentSize().background(Color.White.copy(alpha=0.5f), CircleShape))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showCustomPicker = true }) {
                    Text("Custom Color...")
                }
                
                // Optional: show a preview if a custom color is picked that isn't in the list
                if (selectedColor !in colors) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Selected: ")
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColor))
                        )
                    }
                }
            }

            if (showCustomPicker) {
                CustomColorPickerDialog(
                    initialColor = selectedColor,
                    onSave = { c -> 
                        selectedColor = c
                        showCustomPicker = false 
                    },
                    onDismiss = { showCustomPicker = false }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text(Str.save.text())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Str.cancel2.text())
            }
        }
    )
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialC = Color(initialColor)
    var red by remember { mutableStateOf(initialC.red * 255f) }
    var green by remember { mutableStateOf(initialC.green * 255f) }
    var blue by remember { mutableStateOf(initialC.blue * 255f) }

    val currentColor = Color(red.toInt(), green.toInt(), blue.toInt())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Str.customColor.text()) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(currentColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Red: ${red.toInt()}")
                Slider(value = red, onValueChange = { red = it }, valueRange = 0f..255f)
                
                Text("Green: ${green.toInt()}")
                Slider(value = green, onValueChange = { green = it }, valueRange = 0f..255f)
                
                Text("Blue: ${blue.toInt()}")
                Slider(value = blue, onValueChange = { blue = it }, valueRange = 0f..255f)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(currentColor.toArgb()) }) { Text(Str.save.text()) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Str.cancel2.text()) }
        }
    )
}
