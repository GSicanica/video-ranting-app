package com.youtube.rating.android.feature.running.domain

interface RunningTrackingServiceController {
    fun start(): Boolean
    fun stop(): Boolean
}
