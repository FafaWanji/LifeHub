package com.example.lifeorganizer.habits.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Something to do on fixed weekdays (every day by default), e.g. "Take medication". */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Bit 0 = Monday … bit 6 = Sunday; see [com.example.lifeorganizer.habits.domain.HabitSchedule]. */
    val weekdays: Int = 0b1111111,
    /** Daily reminder as minutes after midnight, null = no reminder. */
    val reminderMinutes: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/** One habit done on one day. */
@Entity(
    tableName = "habit_checks",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)]
)
data class HabitCheck(val habitId: Long, val epochDay: Long)

/** A one-off to-do, optionally with a deadline, e.g. "Exercise 5 by 15 Oct". */
@Entity(tableName = "tasks", indices = [Index("dueEpochDay")])
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** Deadline day, null = no deadline. */
    val dueEpochDay: Long? = null,
    /** When to send a reminder (epoch millis), null = none. */
    val reminderAt: Long? = null,
    /** When it was ticked off, null = still open. */
    val doneAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
