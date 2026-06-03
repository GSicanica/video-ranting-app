package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Application-wide coroutine scope owned by the app process.
 * Cancelled in Application.onTerminate().
 */
object AppScope {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun get(): CoroutineScope = scope

    fun cancel() {
        scope.cancel()
    }
}
