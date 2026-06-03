package com.youtube.rating.habittracker.feature.habits.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object SampleDataSeeder {

    suspend fun seed(dao: HabitDao) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val sixWeeksAgo = today.minusWeeks(6)

        data class HabitDef(
            val name: String,
            val icon: String,
            val days: Set<DayOfWeek>,
            val createdAt: LocalDate,
            val completionPattern: (LocalDate) -> Boolean,
        )

        val habits = listOf(
            // Habit 1: Morning Run - M/W/F, created 6 weeks ago, started completing only last 2 weeks
            HabitDef(
                name = "Morning Run",
                icon = "RUN",
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                createdAt = sixWeeksAgo,
                completionPattern = { date ->
                    val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(date, today)
                    daysAgo < 14 // only completed in last 2 weeks
                },
            ),
            // Habit 2: Read - every day, created 5 weeks ago, about 50% completion
            HabitDef(
                name = "Read 30 min",
                icon = "READ",
                days = DayOfWeek.entries.toSet(),
                createdAt = sixWeeksAgo.plusWeeks(1),
                completionPattern = { date ->
                    date.dayOfYear % 2 == 0 // every other day
                },
            ),
            // Habit 3: Meditate - M-F, created 6 weeks ago, perfect streak (always done)
            HabitDef(
                name = "Meditate",
                icon = "MEDITATE",
                days = setOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                ),
                createdAt = sixWeeksAgo,
                completionPattern = { true },
            ),
            // Habit 4: Drink Water - every day, created 4 weeks ago, never completed (0% everywhere)
            HabitDef(
                name = "Drink 2L Water",
                icon = "WATER",
                days = DayOfWeek.entries.toSet(),
                createdAt = sixWeeksAgo.plusWeeks(2),
                completionPattern = { false },
            ),
            // Habit 5: Code - M-F, created 6 weeks ago, about 30% completion
            HabitDef(
                name = "Code 1 hour",
                icon = "CODE",
                days = setOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                ),
                createdAt = sixWeeksAgo,
                completionPattern = { date ->
                    date.dayOfYear % 3 == 0 // roughly every 3rd day
                },
            ),
            // Habit 6: Gym - T/Th/Sat, created 3 weeks ago, perfect
            HabitDef(
                name = "Go to Gym",
                icon = "GYM",
                days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                createdAt = sixWeeksAgo.plusWeeks(3),
                completionPattern = { true },
            ),
            // Habit 7: Journal - every day, created 6 weeks ago, only first 2 weeks
            HabitDef(
                name = "Journal",
                icon = "JOURNAL",
                days = DayOfWeek.entries.toSet(),
                createdAt = sixWeeksAgo,
                completionPattern = { date ->
                    val daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(sixWeeksAgo, date)
                    daysFromStart < 14
                },
            ),
        )

        for ((index, def) in habits.withIndex()) {
            val createdAt = ZonedDateTime.of(def.createdAt, LocalTime.NOON, zone)
            val habitId = dao.upsertHabit(
                HabitEntity(
                    name = def.name,
                    icon = def.icon,
                    monday = DayOfWeek.MONDAY in def.days,
                    tuesday = DayOfWeek.TUESDAY in def.days,
                    wednesday = DayOfWeek.WEDNESDAY in def.days,
                    thursday = DayOfWeek.THURSDAY in def.days,
                    friday = DayOfWeek.FRIDAY in def.days,
                    saturday = DayOfWeek.SATURDAY in def.days,
                    sunday = DayOfWeek.SUNDAY in def.days,
                    createdAt = createdAt.toInstant().toEpochMilli(),
                )
            )

            var day = def.createdAt
            while (!day.isAfter(today)) {
                if (day.dayOfWeek in def.days && def.completionPattern(day)) {
                    val completionDate = ZonedDateTime.of(day, LocalTime.NOON, zone)
                    dao.insertCompletion(
                        HabitCompletionEntity(
                            habitId = habitId,
                            date = completionDate.toInstant().toEpochMilli(),
                        )
                    )
                }
                day = day.plusDays(1)
            }
        }
    }
}
