package com.youtube.rating.habittracker.feature.habits.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity): Long

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT COUNT(*) FROM habits")
    fun getActiveHabitCount(): Flow<Int>

    @Query("SELECT * FROM habits ORDER BY created_at ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE monday = 1 ORDER BY created_at ASC")
    fun getHabitsForMonday(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE tuesday = 1 ORDER BY created_at ASC")
    fun getHabitsForTuesday(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE wednesday = 1 ORDER BY created_at ASC")
    fun getHabitsForWednesday(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE thursday = 1 ORDER BY created_at ASC")
    fun getHabitsForThursday(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE friday = 1 ORDER BY created_at ASC")
    fun getHabitsForFriday(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE saturday = 1 ORDER BY created_at ASC")
    fun getHabitsForSaturday(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE sunday = 1 ORDER BY created_at ASC")
    fun getHabitsForSunday(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId AND date = :dateMillis")
    suspend fun deleteCompletion(habitId: Long, dateMillis: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habit_id = :habitId AND date = :dateMillis)")
    suspend fun isCompleted(habitId: Long, dateMillis: Long): Boolean

    @Query("SELECT habit_id FROM habit_completions WHERE date = :dateMillis")
    fun getCompletedHabitIdsForDate(dateMillis: Long): Flow<List<Long>>

    @Query("SELECT habit_id, date FROM habit_completions WHERE date >= :startMillis AND date <= :endMillis")
    suspend fun getCompletionsInRange(startMillis: Long, endMillis: Long): List<HabitCompletionRaw>
}

data class HabitCompletionRaw(
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val date: Long
)
