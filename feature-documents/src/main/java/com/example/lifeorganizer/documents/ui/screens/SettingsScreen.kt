package com.example.lifeorganizer.documents.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifeorganizer.documents.R
import com.example.lifeorganizer.core.theme.NeonAccents
import com.example.lifeorganizer.core.theme.PastelAccents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DocumentViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val currentStyle by viewModel.settingsDataStore.themeStyleFlow.collectAsState(initial = "pastel")
    val isDarkMode by viewModel.settingsDataStore.isDarkModeFlow.collectAsState(initial = false)
    val accentIndex by viewModel.settingsDataStore.accentColorIndexFlow.collectAsState(initial = "0")
    val appLanguage by viewModel.appLanguage.collectAsState()

    val styles = listOf("pastel" to "Pastel", "midnight" to "Midnight", "neon" to "Neon", "white_light" to "White Light")
    var styleExpanded by remember { mutableStateOf(false) }

    val languages = listOf(
        "system" to stringResource(R.string.system_default),
        "de" to stringResource(R.string.german),
        "en" to stringResource(R.string.english),
        "es" to stringResource(R.string.spanish),
        "tr" to stringResource(R.string.turkish),
        "zh" to stringResource(R.string.chinese)
    )
    var languageExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Language Selector
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = languageExpanded,
            onExpandedChange = { languageExpanded = !languageExpanded }
        ) {
            OutlinedTextField(
                value = languages.find { it.first == appLanguage }?.second ?: stringResource(R.string.system_default),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false }
            ) {
                languages.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption.second) },
                        onClick = {
                            viewModel.setAppLanguage(selectionOption.first)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Design Style Selector
        Text(stringResource(R.string.design_style), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = styleExpanded,
            onExpandedChange = { styleExpanded = !styleExpanded }
        ) {
            OutlinedTextField(
                value = styles.find { it.first == currentStyle }?.second ?: "Pastel",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = styleExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = styleExpanded,
                onDismissRequest = { styleExpanded = false }
            ) {
                styles.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption.second) },
                        onClick = {
                            coroutineScope.launch {
                                viewModel.settingsDataStore.setThemeStyle(selectionOption.first)
                            }
                            styleExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Conditional UI based on Style
        if (currentStyle == "pastel") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dark_mode), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { 
                        coroutineScope.launch { viewModel.settingsDataStore.setDarkMode(it) } 
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (currentStyle == "pastel" || currentStyle == "neon") {
            Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val accents = if (currentStyle == "pastel") PastelAccents else NeonAccents
                accents.forEachIndexed { index, color ->
                    Spacer(modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                        .background(color, shape = MaterialTheme.shapes.small)
                        .padding(4.dp)
                        .background(
                            if (accentIndex == index.toString()) MaterialTheme.colorScheme.onBackground else androidx.compose.ui.graphics.Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable {
                            coroutineScope.launch { viewModel.settingsDataStore.setAccentColorIndex(index.toString()) }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
