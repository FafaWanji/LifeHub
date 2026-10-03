package com.example.lifeorganizer.documents.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents")
    fun getAllDocumentsSync(): List<Document>

    @Query("SELECT * FROM documents ORDER BY dateAdded DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE isPinned = 1 ORDER BY dateAdded DESC")
    fun getPinnedDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY dateAdded DESC")
    fun searchDocuments(query: String): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDocument(document: Document): Long

    @Update
    fun updateDocument(document: Document): Int

    @Delete
    fun deleteDocument(document: Document): Int
}
