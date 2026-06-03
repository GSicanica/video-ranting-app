package com.youtube.rating.habittracker.feature.habits.data

import com.youtube.rating.habittracker.feature.habits.domain.CompletionRecord
import com.youtube.rating.habittracker.feature.habits.domain.Habit
import com.youtube.rating.habittracker.feature.habits.domain.HabitIcon
import com.youtube.rating.habittracker.feature.habits.domain.WeekDays
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

fun HabitEntity.toHabit(): Habit {
    return Habit(
        id = id,
        name = name,
        icon = HabitIcon.valueOf(icon),
        weekDays = WeekDays(
            monday = monday,
            tuesday = tuesday,
            wednesday = wednesday,
            thursday = thursday,
            friday = friday,
            saturday = saturday,
            sunday = sunday
        ),
        createdAt = Instant.ofEpochMilli(createdAt)
            .atZone(ZoneId.of("UTC"))
    )
}

fun Habit.toHabitEntity(): HabitEntity {
    return HabitEntity(
        id = id,
        name = name,
        icon = icon.name,
        monday = weekDays.monday,
        tuesday = weekDays.tuesday,
        wednesday = weekDays.wednesday,
        thursday = weekDays.thursday,
        friday = weekDays.friday,
        saturday = weekDays.saturday,
        sunday = weekDays.sunday,
        createdAt = createdAt.toInstant().toEpochMilli()
    )
}

fun HabitCompletionRaw.toCompletionRecord(): CompletionRecord {
    return CompletionRecord(
        habitId = habitId,
        date = Instant.ofEpochMilli(date)
            .atZone(ZoneId.of("UTC"))
    )
}

fun ZonedDateTime.toEpochMillis(): Long {
    return toInstant().toEpochMilli()
}
