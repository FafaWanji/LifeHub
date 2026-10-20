package com.example.lifeorganizer.habits.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lifeorganizer.habits.domain.TaskDeadline

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

/**
 * A one-off to-do with a deadline (day + time), e.g. "Exercise 5 by 15 Oct 14:00".
 * Tasks from before deadlines were required may have none and keep working without.
 */
@Entity(tableName = "tasks", indices = [Index("dueEpochDay")])
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** Deadline day, null = no deadline (only old tasks). */
    val dueEpochDay: Long? = null,
    /** Deadline time as minutes after midnight on [dueEpochDay]. */
    val dueMinute: Int = TaskDeadline.END_OF_DAY,
    /** Reminders as minutes before the deadline, e.g. "1440,60"; one at the deadline itself always comes on top. */
    val reminderOffsets: String = TaskDeadline.DEFAULT_OFFSETS,
    /** Fixed reminder time (epoch millis) of old tasks without a deadline, null = none. */
    val reminderAt: Long? = null,
    /** When it was ticked off, null = still open. */
    val doneAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Reminders up to this time (epoch millis) are handled; later ones missed while the phone was off are caught up. */
    val lastReminderAt: Long? = null
)

/**
 * Points earned with a task. Kept when the task is deleted (so the total stays), removed when it is unticked.
 * [taskId] is not a foreign key on purpose; task ids are never reused, so undoing a delete finds its points again.
 */
@Entity(tableName = "task_points", indices = [Index(value = ["taskId"], unique = true)])
data class TaskPoints(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val title: String,
    val points: Int,
    val earnedAt: Long
)
