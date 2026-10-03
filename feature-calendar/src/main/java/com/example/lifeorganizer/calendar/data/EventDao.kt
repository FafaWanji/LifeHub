package com.example.lifeorganizer.calendar.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM event_templates")
    suspend fun getEventTemplatesSync(): List<EventTemplate>

    @Transaction
    @Query("SELECT * FROM events ORDER BY startTimeMillis ASC")
    fun getEventsWithReminders(): Flow<List<EventWithReminders>>

    @Transaction
    @Query("SELECT * FROM events ORDER BY startTimeMillis ASC")
    suspend fun getEventsWithRemindersSync(): List<EventWithReminders>

    @Transaction
    @Query("SELECT * FROM events WHERE startTimeMillis BETWEEN :start AND :end ORDER BY startTimeMillis ASC")
    fun getEventsInRange(start: Long, end: Long): Flow<List<EventWithReminders>>

    @Transaction
    @Query("SELECT * FROM events WHERE title LIKE '%' || :query || '%' ORDER BY startTimeMillis ASC")
    fun searchEvents(query: String): Flow<List<EventWithReminders>>

    @Transaction
    @Query("SELECT * FROM events WHERE isBirthday = 1 ORDER BY startTimeMillis ASC")
    fun getBirthdays(): Flow<List<EventWithReminders>>

    @Query("SELECT * FROM events WHERE startTimeMillis >= :fromMillis ORDER BY startTimeMillis ASC LIMIT :limit")
    suspend fun getUpcomingEvents(fromMillis: Long, limit: Int): List<Event>

    @Insert
    suspend fun insertEvent(event: Event): Long

    @Insert
    suspend fun insertReminders(reminders: List<Reminder>)

    @Update
    suspend fun updateEvent(event: Event)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersByEventId(eventId: Long)

    @Transaction
    suspend fun insertEventWithReminders(event: Event, reminders: List<Reminder>) {
        val eventId = insertEvent(event)
        val remindersWithId = reminders.map { it.copy(eventId = eventId) }
        insertReminders(remindersWithId)
    }

    @Transaction
    suspend fun updateEventWithReminders(event: Event, reminders: List<Reminder>) {
        updateEvent(event)
        deleteRemindersByEventId(event.id)
        val remindersWithId = reminders.map { it.copy(eventId = event.id) }
        insertReminders(remindersWithId)
    }

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM reminders WHERE eventId = :eventId")
    suspend fun getRemindersForEvent(eventId: Long): List<Reminder>

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getCategoriesSync(): List<Category>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByNameSync(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM event_templates ORDER BY name ASC")
    fun getEventTemplates(): Flow<List<EventTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventTemplate(template: EventTemplate): Long

    @Delete
    suspend fun deleteEventTemplate(template: EventTemplate)
}
