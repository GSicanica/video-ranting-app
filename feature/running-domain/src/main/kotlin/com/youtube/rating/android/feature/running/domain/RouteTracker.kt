package com.youtube.rating.android.feature.running.domain

import kotlinx.coroutines.flow.Flow

interface RouteTracker {
    fun routeUpdates(): Flow<RoutePoint>
}
