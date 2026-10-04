package com.example.lifeorganizer.calendar.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val isAllDay: Boolean = false,
    val targetAddress: String? = null,
    val arrivalBufferMinutes: Int = 0,
    val alarmLeadMinutes: Int = 0,
    val color: Int? = null, // ARGB color for event category
    val recurrenceRule: String? = null, // RRULE-like string for recurring events
    val timezone: String = java.time.ZoneId.systemDefault().id,
    val isBirthday: Boolean = false, // true for birthday events
    val birthYear: Int? = null, // birth year for age calculation
    val categoryId: Long? = null, // FK to categories
    val linkedNoteId: Long? = null, // note this event reminds about (note reminders)
    val exDates: String? = null // repeating events: skipped dates, "2026-10-13,2026-10-20"
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val reminderTimeMillis: Long,
    val type: String, // e.g., "1 day before", "exact time"
    val timezone: String = java.time.ZoneId.systemDefault().id
)

@Entity(tableName = "event_templates")
data class EventTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val title: String,
    val description: String,
    val isAllDay: Boolean = false,
    val targetAddress: String? = null,
    val arrivalBufferMinutes: Int = 0,
    val alarmLeadMinutes: Int = 0,
    val categoryId: Long? = null,
    val recurrenceRule: String? = null
)

data class EventWithReminders(
    @Embedded val event: Event,
    @Relation(
        parentColumn = "id",
        entityColumn = "eventId"
    )
    val reminders: List<Reminder>,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category? = null
)

data class RecurrenceType(
    val displayName: String,
    val rrule: String?
) {
    companion object {
        val NONE = RecurrenceType("None", null)
        val DAILY = RecurrenceType("Daily", "FREQ=DAILY")
        val WEEKLY = RecurrenceType("Weekly", "FREQ=WEEKLY")
        val BIWEEKLY = RecurrenceType("Bi-weekly", "FREQ=WEEKLY;INTERVAL=2")
        val MONTHLY = RecurrenceType("Monthly", "FREQ=MONTHLY")
        val YEARLY = RecurrenceType("Yearly", "FREQ=YEARLY")
        val ALL = listOf(NONE, DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY)
    }
}

data class EventColor(
    val name: String,
    val color: Int
) {
    companion object {
        val DEFAULT = EventColor("Default", 0xFF2196F3.toInt())
        val WORK = EventColor("Work", 0xFF4CAF50.toInt())
        val PERSONAL = EventColor("Personal", 0xFFFF9800.toInt())
        val HEALTH = EventColor("Health", 0xFFE91E63.toInt())
        val IMPORTANT = EventColor("Important", 0xFFF44336.toInt())
        val TRAVEL = EventColor("Travel", 0xFF9C27B0.toInt())
        val BIRTHDAY = EventColor("Birthday", 0xFFFF6B9D.toInt()) // pink
        val ALL = listOf(DEFAULT, WORK, PERSONAL, HEALTH, IMPORTANT, TRAVEL, BIRTHDAY)
    }
}
