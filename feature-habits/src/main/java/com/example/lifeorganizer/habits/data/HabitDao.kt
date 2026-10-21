package com.example.lifeorganizer.habits.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    // Habits
    @Query("SELECT * FROM habits ORDER BY sortOrder, id")
    fun habits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY sortOrder, id")
    suspend fun habitsSync(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun habitById(id: Long): Habit?

    @Insert suspend fun insertHabit(habit: Habit): Long
    @Update suspend fun updateHabit(habit: Habit)
    @Delete suspend fun deleteHabit(habit: Habit)

    // Checks
    @Query("SELECT * FROM habit_checks WHERE epochDay >= :fromDay")
    fun checksSince(fromDay: Long): Flow<List<HabitCheck>>

    @Query("SELECT * FROM habit_checks")
    suspend fun checksSync(): List<HabitCheck>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: HabitCheck)

    @Delete suspend fun deleteCheck(check: HabitCheck)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY dueEpochDay IS NULL, dueEpochDay, dueMinute, createdAt")
    fun tasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks")
    suspend fun tasksSync(): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun taskById(id: Long): TaskItem?

    @Insert suspend fun insertTask(task: TaskItem): Long
    @Update suspend fun updateTask(task: TaskItem)
    @Delete suspend fun deleteTask(task: TaskItem)

    @Query("UPDATE tasks SET lastReminderAt = :at WHERE id = :id")
    suspend fun setLastReminder(id: Long, at: Long)

    // Points
    @Query("SELECT COALESCE(SUM(points), 0) FROM task_points")
    fun totalPoints(): Flow<Int>

    @Query("SELECT * FROM task_points")
    suspend fun pointsSync(): List<TaskPoints>

    @Query("SELECT * FROM task_points")
    fun points(): Flow<List<TaskPoints>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: TaskPoints)

    @Query("DELETE FROM task_points WHERE taskId = :taskId")
    suspend fun deletePointsOf(taskId: Long)
}
