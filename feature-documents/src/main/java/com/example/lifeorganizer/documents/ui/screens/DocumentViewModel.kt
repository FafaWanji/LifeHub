package com.example.lifeorganizer.documents.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeorganizer.documents.data.local.AppDatabase
import com.example.lifeorganizer.documents.data.local.Document
import com.example.lifeorganizer.documents.data.local.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val documentDao = AppDatabase.getDatabase(application).documentDao()
    val settingsDataStore = SettingsDataStore(application)

    val allDocuments: StateFlow<List<Document>> = documentDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pinnedDocuments: StateFlow<List<Document>> = documentDao.getPinnedDocuments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val groups: StateFlow<Set<String>> = settingsDataStore.groupsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, setOf("Allgemein", "Arbeit", "Privat"))

    val appLanguage: StateFlow<String> = settingsDataStore.appLanguageFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "system")

    fun searchDocuments(query: String): StateFlow<List<Document>> {
        return documentDao.searchDocuments(query)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun addDocument(title: String, uri: String, category: String, isPdf: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.insertDocument(
                Document(
                    title = title,
                    uri = uri,
                    category = category,
                    isPdf = isPdf
                )
            )
        }
    }

    fun togglePin(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.updateDocument(document.copy(isPinned = !document.isPinned))
        }
    }

    fun updateDocument(document: Document, newTitle: String, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.updateDocument(document.copy(title = newTitle, category = newCategory))
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            documentDao.deleteDocument(document)
        }
    }

    fun addGroup(groupName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataStore.addGroup(groupName)
        }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataStore.setAppLanguage(language)
        }
    }
}
