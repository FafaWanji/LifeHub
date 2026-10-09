package com.example.lifeorganizer.money.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {
    // Transactions
    @Query("SELECT * FROM transactions WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay DESC, id DESC")
    fun transactionsBetween(from: Long, to: Long): Flow<List<MoneyTransaction>>

    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    suspend fun allTransactionsSync(): List<MoneyTransaction>

    /** Returns -1 when the row already exists (import hash or fixed-cost due date). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(tx: MoneyTransaction): Long

    @Update suspend fun updateTransaction(tx: MoneyTransaction)
    @Delete suspend fun deleteTransaction(tx: MoneyTransaction)

    @Query("SELECT importHash FROM transactions WHERE importHash IS NOT NULL")
    suspend fun importHashes(): List<String>

    @Query("SELECT * FROM transactions WHERE source = 'RECURRING' AND amountCents = :amount AND epochDay BETWEEN :from AND :to ORDER BY epochDay LIMIT 1")
    suspend fun findBookedRecurring(amount: Long, from: Long, to: Long): MoneyTransaction?

    // Categories
    @Query("SELECT * FROM money_categories ORDER BY kind DESC, sortOrder, name")
    fun categories(): Flow<List<MoneyCategory>>

    @Query("SELECT * FROM money_categories ORDER BY kind DESC, sortOrder, name")
    suspend fun categoriesSync(): List<MoneyCategory>

    @Insert suspend fun insertCategory(c: MoneyCategory): Long
    @Update suspend fun updateCategory(c: MoneyCategory)
    @Delete suspend fun deleteCategoryRow(c: MoneyCategory)

    @Query("UPDATE transactions SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveTransactions(from: Long, to: Long)

    @Query("UPDATE recurring SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveRecurring(from: Long, to: Long)

    @Query("DELETE FROM payee_rules WHERE categoryId = :categoryId")
    suspend fun deleteRulesFor(categoryId: Long)

    /** Transactions and fixed costs fall back to [fallbackId]; learned rules of the category go away. */
    @Transaction
    suspend fun deleteCategory(c: MoneyCategory, fallbackId: Long) {
        moveTransactions(c.id, fallbackId)
        moveRecurring(c.id, fallbackId)
        deleteRulesFor(c.id)
        deleteCategoryRow(c)
    }

    // Fixed costs
    @Query("SELECT * FROM recurring ORDER BY dayOfMonth, title")
    fun recurring(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurring ORDER BY dayOfMonth, title")
    suspend fun recurringSync(): List<Recurring>

    @Insert suspend fun insertRecurring(r: Recurring): Long
    @Update suspend fun updateRecurring(r: Recurring)
    @Delete suspend fun deleteRecurringRow(r: Recurring)

    @Query("UPDATE transactions SET recurringId = NULL, recurringDueDay = NULL WHERE recurringId = :id")
    suspend fun detachRecurring(id: Long)

    /** Booked months stay as normal transactions. */
    @Transaction
    suspend fun deleteRecurring(r: Recurring) {
        detachRecurring(r.id)
        deleteRecurringRow(r)
    }

    // Learned payee rules
    @Query("SELECT * FROM payee_rules ORDER BY normalizedPayee")
    fun rules(): Flow<List<PayeeRule>>

    @Query("SELECT * FROM payee_rules")
    suspend fun rulesSync(): List<PayeeRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: PayeeRule)

    @Delete suspend fun deleteRule(rule: PayeeRule)
}
