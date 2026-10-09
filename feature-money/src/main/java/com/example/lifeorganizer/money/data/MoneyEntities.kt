package com.example.lifeorganizer.money.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lifeorganizer.money.domain.RecurringInterval

enum class CategoryKind { INCOME, EXPENSE }
enum class TxSource { MANUAL, SMART_ADD, CSV, RECURRING }

@Entity(tableName = "money_categories")
data class MoneyCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    /** Key for [com.example.lifeorganizer.money.ui.categoryIcon]. */
    val icon: String,
    val kind: CategoryKind,
    val monthlyBudgetCents: Long? = null,
    val sortOrder: Int = 0,
    /** "Other" of each kind: cannot be deleted and receives everything unassigned. */
    val isFallback: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["importHash"], unique = true),
        Index("epochDay"),
        // One booking per fixed cost and due date, however often the worker runs
        Index(value = ["recurringId", "recurringDueDay"], unique = true)
    ]
)
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** Negative = expense, positive = income. */
    val amountCents: Long,
    val title: String,
    val note: String = "",
    val categoryId: Long? = null,
    val source: TxSource = TxSource.MANUAL,
    val recurringId: Long? = null,
    val recurringDueDay: Long? = null,
    val importHash: String? = null
)

@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val categoryId: Long? = null,
    val interval: RecurringInterval = RecurringInterval.MONTHLY,
    val dayOfMonth: Int,
    val startEpochDay: Long,
    val nextDueEpochDay: Long = 0,
    val showInCalendar: Boolean = false,
    val calendarEventId: Long? = null
)

@Entity(tableName = "payee_rules")
data class PayeeRule(
    @PrimaryKey val normalizedPayee: String,
    val categoryId: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
