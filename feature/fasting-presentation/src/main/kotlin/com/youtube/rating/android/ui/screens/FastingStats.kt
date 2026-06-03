package com.youtube.rating.android.ui.screens

import java.util.Calendar
import java.util.Locale

fun buildMonthCells(year: Int, month: Int): List<Int?> {
    val cal = Calendar.getInstance().apply { set(year, month, 1) }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDay = cal.get(Calendar.DAY_OF_WEEK)
    val offset = when (firstDay) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val cells = MutableList<Int?>(offset) { null }
    for (day in 1..daysInMonth) {
        cells.add(day)
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells
}

fun dateKeyFor(cal: Calendar): String {
    return dateKeyFor(year = cal.get(Calendar.YEAR), month = cal.get(Calendar.MONTH), day = cal.get(Calendar.DAY_OF_MONTH))
}

fun dateKeyFor(year: Int, month: Int, day: Int): String {
    return String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
}

fun calculateCurrentStreak(days: Set<String>, todayKey: String): Int {
    if (days.isEmpty()) return 0
    val cal = Calendar.getInstance()
    var streak = 0
    while (true) {
        val key = dateKeyFor(cal = cal)
        if (!days.contains(key)) break
        streak += 1
        cal.add(Calendar.DAY_OF_MONTH, -1)
    }
    if (!days.contains(todayKey)) return 0
    return streak
}

fun calculateLongestStreak(days: Set<String>): Int {
    if (days.isEmpty()) return 0
    val sorted = days.sorted()
    var longest = 1
    var run = 1
    var prevKey: String? = null
    for (key in sorted) {
        if (prevKey == null) {
            run = 1
        } else {
            val nextKey = nextDayKey(key = prevKey)
            if (key == nextKey) {
                run += 1
            } else {
                run = 1
            }
        }
        if (run > longest) longest = run
        prevKey = key
    }
    return longest
}

fun nextDayKey(key: String): String {
    val parts = key.split("-")
    if (parts.size != 3) return key
    val year = parts[0].toIntOrNull() ?: return key
    val month = parts[1].toIntOrNull() ?: return key
    val day = parts[2].toIntOrNull() ?: return key
    if (month !in 1..12) return key
    if (day !in 1..31) return key

    val cal = Calendar.getInstance().apply {
        isLenient = false
        set(year, month - 1, day)
    }

    return try {
        // Force validation for invalid dates like 2026-02-31
        cal.time
        cal.add(Calendar.DAY_OF_MONTH, 1)
        dateKeyFor(cal = cal)
    } catch (e: Exception) {
        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
        key
    }
}

fun weekKeysFromToday(): List<String> {
    val cal = Calendar.getInstance()
    return (0..6).map {
        val key = dateKeyFor(cal = cal)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        key
    }.reversed()
}
