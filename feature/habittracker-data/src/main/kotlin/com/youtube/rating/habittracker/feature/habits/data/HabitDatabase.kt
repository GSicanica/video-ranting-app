package com.youtube.rating.habittracker.feature.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 1
)
abstract class HabitDatabase : RoomDatabase() {
    abstract val habitDao: HabitDao
}