package com.example.lifeorganizer.documents.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val uri: String,
    val category: String,
    val expiryDate: Long? = null,
    val isPinned: Boolean = false,
    val isPdf: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
