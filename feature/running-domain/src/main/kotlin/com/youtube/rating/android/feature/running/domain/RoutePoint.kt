package com.youtube.rating.android.feature.running.domain

import kotlinx.serialization.Serializable

@Serializable
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
)

