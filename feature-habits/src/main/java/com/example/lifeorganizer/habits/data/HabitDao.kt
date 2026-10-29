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
}
