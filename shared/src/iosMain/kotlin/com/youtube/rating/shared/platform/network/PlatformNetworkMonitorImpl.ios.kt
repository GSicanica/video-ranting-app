package com.youtube.rating.shared.platform.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class PlatformNetworkMonitor {
    private val state = MutableStateFlow(true)

    val isOnline: Flow<Boolean> = state.asStateFlow()

    fun isCurrentlyOnline(): Boolean = state.value
}

class PlatformNetworkMonitorImpl : PlatformNetworkMonitor()
