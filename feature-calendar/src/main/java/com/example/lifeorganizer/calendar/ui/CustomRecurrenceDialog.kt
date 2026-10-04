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
import androidx.compose.ui.window.Dialog

@Composable
fun CustomRecurrenceDialog(
    initialRule: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // Parse initialRule if any
    var freq by remember { mutableStateOf("WEEKLY") }
    var interval by remember { mutableStateOf("1") }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    
    LaunchedEffect(initialRule) {
        if (!initialRule.isNullOrBlank()) {
            val parts = initialRule.removePrefix("RRULE:").split(";")
            parts.forEach { part ->
                val kv = part.split("=")
                if (kv.size == 2) {
                    when (kv[0]) {
                        "FREQ" -> freq = kv[1]
                        "INTERVAL" -> interval = kv[1]
                        "BYDAY" -> selectedDays = kv[1].split(",").toSet()
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = Str.customRecurrence.text(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Frequency Dropdown
                var freqExpanded by remember { mutableStateOf(false) }
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = freqExpanded,
                    onExpandedChange = { freqExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = freq,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Str.frequency.text()) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { f ->
                            DropdownMenuItem(text = { Text(f) }, onClick = { freq = f; freqExpanded = false })
                        }
                    }
                }

                // Interval
                OutlinedTextField(
                    value = interval,
                    onValueChange = { if (it.all { c -> c.isDigit() }) interval = it },
                    label = { Text("Interval (e.g. 1 for every $freq)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // ByDay (only for WEEKLY)
                if (freq == "WEEKLY") {
                    Text("Repeat on:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val days = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
                        days.forEach { day ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    selectedDays = if (selectedDays.contains(day)) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                },
                                label = { Text(day.take(1)) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Str.cancel2.text())
                    }
                    Button(
                        onClick = {
                            val intervalInt = interval.toIntOrNull() ?: 1
                            val builder = java.lang.StringBuilder("FREQ=$freq")
                            if (intervalInt > 1) builder.append(";INTERVAL=$intervalInt")
                            if (freq == "WEEKLY" && selectedDays.isNotEmpty()) {
                                builder.append(";BYDAY=${selectedDays.joinToString(",")}")
                            }
                            onSave(builder.toString())
                        },
                        enabled = interval.isNotBlank() && (interval.toIntOrNull() ?: 0) > 0
                    ) {
                        Text(Str.save.text())
                    }
                }
            }
        }
    }
}
