package com.example.lifeorganizer.calendar.ui

import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.i18n.Str
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.calendar.data.Category

@Composable
fun ExportOptionsDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onExport: (exportAll: Boolean, exportBirthdays: Boolean, selectedCategoryIds: Set<Long>) -> Unit
) {
    var exportAll by remember { mutableStateOf(true) }
    var exportBirthdays by remember { mutableStateOf(false) }
    var selectedCategoryIds by remember { mutableStateOf(emptySet<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Str.exportOptions.text()) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = exportAll,
                        onClick = { exportAll = true }
                    )
                    Text(Str.exportEverything.text(), modifier = Modifier.padding(start = 8.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = !exportAll,
                        onClick = { exportAll = false }
                    )
                    Text(Str.exportSpecific.text(), modifier = Modifier.padding(start = 8.dp))
                }

                if (!exportAll) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select what to export:", style = MaterialTheme.typography.labelMedium)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp)
                    ) {
                        Checkbox(
                            checked = exportBirthdays,
                            onCheckedChange = { exportBirthdays = it }
                        )
                        Text(Str.birthdays.text(), modifier = Modifier.padding(start = 8.dp))
                    }

                    categories.forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                        ) {
                            Checkbox(
                                checked = selectedCategoryIds.contains(category.id),
                                onCheckedChange = { isChecked ->
                                    selectedCategoryIds = if (isChecked) {
                                        selectedCategoryIds + category.id
                                    } else {
                                        selectedCategoryIds - category.id
                                    }
                                }
                            )
                            Text(category.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(exportAll, exportBirthdays, selectedCategoryIds)
                }
            ) {
                Text(Str.exportBtn2.text())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Str.cancel2.text())
            }
        }
    )
}
