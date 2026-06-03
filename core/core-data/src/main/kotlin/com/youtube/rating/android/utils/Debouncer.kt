package com.youtube.rating.android.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounce helper for search input and other rapid-fire events.
 * Prevents excessive API calls and improves performance.
 */
class Debouncer(
    private val delayMillis: Long = 300L,
    private val coroutineScope: CoroutineScope
) {
    @Volatile
    private var job: Job? = null

    /**
     * Debounces the action. Only the last call within the delay period will execute.
     */
    fun debounce(action: suspend () -> Unit) {
        job?.cancel()
        job = coroutineScope.launch {
            delay(delayMillis)
            action()
        }
    }

    /**
     * Cancel any pending debounced action
     */
    fun cancel() {
        job?.cancel()
        job = null
    }
}

/**
 * Throttle helper - ensures action is called at most once per time period
 * Useful for scroll events, rapid button clicks, etc.
 */
class Throttler(
    private val intervalMillis: Long = 500L
) {
    @Volatile
    private var lastExecutionTime = 0L

    /**
     * Throttles the action. Will only execute if enough time has passed since last execution.
     * @return true if action was executed, false if throttled
     */
    fun throttle(action: () -> Unit): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastExecutionTime >= intervalMillis) {
            lastExecutionTime = now
            action()
            return true
        }
        return false
    }
}
