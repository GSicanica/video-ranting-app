package com.youtube.rating.android.feature.running.domain

data class RunningStats(
    val totalRuns: Int,
    val totalDistanceKm: Double,
    val totalDurationMinutes: Int,
    val averagePaceMinPerKm: Double,
)

