package com.example.lifeorganizer.documents.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.lifeorganizer.documents.R
import com.example.lifeorganizer.documents.ui.components.LabelChip
import com.example.lifeorganizer.documents.ui.components.PdfViewer
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    documentId: Long,
    viewModel: DocumentViewModel,
    navController: NavController
) {
    val allDocs by viewModel.allDocuments.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val document = allDocs.find { it.id == documentId }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    // The list starts empty while the DB loads – wait briefly instead of flashing "not found".
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(600); waited = true }
    if (document == null) {
        if (waited) Text(stringResource(R.string.doc_not_found), modifier = Modifier.padding(16.dp))
        return
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_document)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(stringResource(R.string.title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = groupDropdownExpanded,
                        onExpandedChange = { groupDropdownExpanded = !groupDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.group)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = groupDropdownExpanded,
                            onDismissRequest = { groupDropdownExpanded = false }
                        ) {
                            groups.forEach { group ->
                                val displayName = when (group) {
                                    "Allgemein" -> stringResource(R.string.general)
                                    "Arbeit" -> stringResource(R.string.work)
                                    "Privat" -> stringResource(R.string.private_group)
                                    else -> group
                                }
                                DropdownMenuItem(
                                    text = { Text(displayName) },
                                    onClick = {
                                        editCategory = group
                                        groupDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editTitle.isNotBlank() && editCategory.isNotBlank()) {
                        viewModel.updateDocument(document, editTitle.trim(), editCategory.trim())
                    }
                    showEditDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_document)) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteDocument(document)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = {
                    editTitle = document.title
                    editCategory = document.category
                    showEditDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_document),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_document),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val displayCategory = when (document.category) {
            "Allgemein" -> stringResource(R.string.general)
            "Arbeit" -> stringResource(R.string.work)
            "Privat" -> stringResource(R.string.private_group)
            else -> document.category
        }
        LabelChip(text = displayCategory, customColor = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(24.dp))

        if (document.isPdf) {
            PdfViewer(
                uri = Uri.parse(document.uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            AsyncImage(
                model = Uri.parse(document.uri),
                contentDescription = document.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Fit
            )
        }
    }
}
