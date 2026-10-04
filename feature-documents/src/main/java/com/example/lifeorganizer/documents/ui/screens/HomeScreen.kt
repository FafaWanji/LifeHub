package com.example.lifeorganizer.documents.ui.screens

import com.example.lifeorganizer.core.i18n.text
import com.example.lifeorganizer.core.i18n.Str
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lifeorganizer.documents.R
import com.example.lifeorganizer.documents.data.local.Document
import com.example.lifeorganizer.documents.ui.components.DeleteDocumentDialog
import com.example.lifeorganizer.documents.ui.components.EditDocumentDialog
import com.example.lifeorganizer.documents.ui.components.LabelChip
import com.example.lifeorganizer.documents.ui.navigation.Screen

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: DocumentViewModel
) {
    val pinnedDocs by viewModel.pinnedDocuments.collectAsState()
    val groups by viewModel.groups.collectAsState()
    var docToEdit by remember { mutableStateOf<Document?>(null) }
    var docToDelete by remember { mutableStateOf<Document?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.pinned_docs),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (pinnedDocs.isEmpty()) {
            Text(
                text = stringResource(R.string.no_pinned_docs),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pinnedDocs) { doc ->
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentCard(
    document: Document,
    onPinClick: () -> Unit,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LabelChip(
                        text = document.category,
                        customColor = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPinClick) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = Str.pin2.text(),
                        tint = if (document.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_document)) },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_document)) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}
