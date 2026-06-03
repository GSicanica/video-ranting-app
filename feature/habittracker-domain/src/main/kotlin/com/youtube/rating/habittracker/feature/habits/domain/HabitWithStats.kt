package com.youtube.rating.habittracker.feature.habits.domain

data class HabitWithStats(
    val habit: Habit,
    val currentStreak: Int,
    val bestStreak: Int
)
