package com.youtube.rating.habittracker.feature.habits.data

import com.youtube.rating.habittracker.core.domain.DataError
import com.youtube.rating.habittracker.core.domain.EmptyResult
import com.youtube.rating.habittracker.core.domain.Result
import com.youtube.rating.habittracker.feature.habits.domain.CompletionRecord
import com.youtube.rating.habittracker.feature.habits.domain.Habit
import com.youtube.rating.habittracker.feature.habits.domain.HabitLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.ZonedDateTime

class RoomHabitDataSource(
    private val dao: HabitDao
) : HabitLocalDataSource {

    override fun getHabitsForDayOfWeek(dayOfWeek: DayOfWeek): Flow<List<Habit>> {
        val entityFlow = when (dayOfWeek) {
            DayOfWeek.MONDAY -> dao.getHabitsForMonday()
            DayOfWeek.TUESDAY -> dao.getHabitsForTuesday()
            DayOfWeek.WEDNESDAY -> dao.getHabitsForWednesday()
            DayOfWeek.THURSDAY -> dao.getHabitsForThursday()
            DayOfWeek.FRIDAY -> dao.getHabitsForFriday()
            DayOfWeek.SATURDAY -> dao.getHabitsForSaturday()
            DayOfWeek.SUNDAY -> dao.getHabitsForSunday()
        }
        return entityFlow.map { entities ->
            entities.map { it.toHabit() }
        }
    }

    override fun getCompletedHabitIdsForDate(date: ZonedDateTime): Flow<Set<Long>> {
        return dao.getCompletedHabitIdsForDate(date.toEpochMillis())
            .map { it.toSet() }
    }

    override suspend fun toggleCompletion(habitId: Long, date: ZonedDateTime) {
        val millis = date.toEpochMillis()
        if (dao.isCompleted(habitId, millis)) {
            dao.deleteCompletion(habitId, millis)
        } else {
            dao.insertCompletion(
                HabitCompletionEntity(habitId = habitId, date = millis)
            )
        }
    }

    override suspend fun upsertHabit(habit: Habit): EmptyResult<DataError.Local> {
        return try {
            dao.upsertHabit(habit.toHabitEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun deleteHabit(habitId: Long) {
        dao.deleteHabit(habitId)
    }

    override suspend fun getHabitById(habitId: Long): Habit? {
        return dao.getHabitById(habitId)?.toHabit()
    }

    override fun getActiveHabitCount(): Flow<Int> {
        return dao.getActiveHabitCount()
    }

    override suspend fun getAllHabits(): List<Habit> {
        return dao.getAllHabits().map { it.toHabit() }
    }

    override suspend fun getCompletionsInRange(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<CompletionRecord> {
        return dao.getCompletionsInRange(
            startMillis = start.toEpochMillis(),
            endMillis = end.toEpochMillis()
        ).map { it.toCompletionRecord() }
    }
}
