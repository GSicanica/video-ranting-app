package com.youtube.rating.android.feature.running.domain

import kotlinx.serialization.Serializable

@Serializable
data class RunningActiveSession(
    val startedAtEpochMillis: Long,
    val distanceKm: Double,
    val routePoints: List<RoutePoint>,
)

