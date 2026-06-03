package com.youtube.rating.android.feature.running.domain

import kotlinx.serialization.Serializable

/**
 * Represents a single running session tracked inside the standalone Running feature.
 */
@Serializable
data class RunningEntry(
    val id: Long,
    val distanceKm: Double,
    val durationMinutes: Int,
    val paceMinPerKm: Double,
    val createdAtMillis: Long,
    val routePoints: List<RoutePoint> = emptyList(),
)

