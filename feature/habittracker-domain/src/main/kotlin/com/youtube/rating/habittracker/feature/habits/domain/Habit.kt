package com.youtube.rating.habittracker.feature.habits.domain

import java.time.ZonedDateTime

data class Habit(
    val id: Long,
    val name: String,
    val icon: HabitIcon,
    val weekDays: WeekDays,
    val createdAt: ZonedDateTime
)
