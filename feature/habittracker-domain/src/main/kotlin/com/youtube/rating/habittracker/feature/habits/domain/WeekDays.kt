package com.youtube.rating.habittracker.feature.habits.domain

import java.time.DayOfWeek

data class WeekDays(
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
    val sunday: Boolean = false
) {
    fun isScheduledFor(dayOfWeek: DayOfWeek): Boolean {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> monday
            DayOfWeek.TUESDAY -> tuesday
            DayOfWeek.WEDNESDAY -> wednesday
            DayOfWeek.THURSDAY -> thursday
            DayOfWeek.FRIDAY -> friday
            DayOfWeek.SATURDAY -> saturday
            DayOfWeek.SUNDAY -> sunday
        }
    }

    fun hasAtLeastOneDay(): Boolean {
        return monday || tuesday || wednesday || thursday || friday || saturday || sunday
    }

    fun toggle(dayOfWeek: DayOfWeek): WeekDays {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> copy(monday = !monday)
            DayOfWeek.TUESDAY -> copy(tuesday = !tuesday)
            DayOfWeek.WEDNESDAY -> copy(wednesday = !wednesday)
            DayOfWeek.THURSDAY -> copy(thursday = !thursday)
            DayOfWeek.FRIDAY -> copy(friday = !friday)
            DayOfWeek.SATURDAY -> copy(saturday = !saturday)
            DayOfWeek.SUNDAY -> copy(sunday = !sunday)
        }
    }
}
