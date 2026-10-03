package com.example.lifeorganizer.documents.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifeorganizer.documents.R
import com.example.lifeorganizer.documents.data.local.Document
import com.example.lifeorganizer.documents.ui.components.DeleteDocumentDialog
import com.example.lifeorganizer.documents.ui.components.EditDocumentDialog
import com.example.lifeorganizer.documents.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navController: NavController,
    viewModel: DocumentViewModel,
    onMenuClick: () -> Unit = {}
) {
    val allDocs by viewModel.allDocuments.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val context = LocalContext.current

    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var docToEdit by remember { mutableStateOf<Document?>(null) }
    var docToDelete by remember { mutableStateOf<Document?>(null) }

    val filteredDocs = if (selectedGroup == null) {
        allDocs
    } else {
        allDocs.filter { it.category == selectedGroup }
    }

    // Several files at once (handy when moving over from DocPocket).
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val fallbackGroup = context.getString(R.string.general)
        uris.forEach { uri ->
            // Persist URI permissions so the file still opens after a restart
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val isPdf = context.contentResolver.getType(uri) == "application/pdf"
            viewModel.addDocument(
                title = displayName(context, uri) ?: uri.lastPathSegment ?: context.getString(R.string.new_doc),
                uri = uri.toString(),
                category = selectedGroup ?: fallbackGroup,
                isPdf = isPdf
            )
        }
    }

    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text(stringResource(R.string.new_group)) },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text(stringResource(R.string.group_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newGroupName.isNotBlank()) {
                        viewModel.addGroup(newGroupName.trim())
                        selectedGroup = newGroupName.trim()
                    }
                    showNewGroupDialog = false
                    newGroupName = ""
                }) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (docToEdit != null) {
        EditDocumentDialog(
            document = docToEdit!!,
            groups = groups,
            onDismiss = { docToEdit = null },
            onSave = { newTitle, newCategory ->
                viewModel.updateDocument(docToEdit!!, newTitle, newCategory)
                docToEdit = null
            }
        )
    }

    if (docToDelete != null) {
        DeleteDocumentDialog(
            onDismiss = { docToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(docToDelete!!)
                docToDelete = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_doc), tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.documents)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onMenuClick) {
                        androidx.compose.material3.Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.all_docs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Group Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                        label = { Text(stringResource(R.string.all)) }
                    )
                }
                items(groups.toList()) { group ->
                    val displayName = when (group) {
                        "Allgemein" -> stringResource(R.string.general)
                        "Arbeit" -> stringResource(R.string.work)
                        "Privat" -> stringResource(R.string.private_group)
                        else -> group
                    }
                    FilterChip(
                        selected = selectedGroup == group,
                        onClick = { selectedGroup = group },
                        label = { Text(displayName) }
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showNewGroupDialog = true },
                        label = { Text("+ ${stringResource(R.string.new_group)}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredDocs.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_docs_added),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDocs) { doc ->
                        DocumentCard(
                            document = doc,
                            onPinClick = { viewModel.togglePin(doc) },
                            onClick = { navController.navigate(Screen.Detail.createRoute(doc.id)) },
                            onEditClick = { docToEdit = doc },
                            onDeleteClick = { docToDelete = doc }
                        )
                    }
                }
            }
        }
    }
}

/** The file's real name (e.g. "Personalausweis.pdf") instead of a cryptic document id. */
private fun displayName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null
    }
}.getOrNull()
