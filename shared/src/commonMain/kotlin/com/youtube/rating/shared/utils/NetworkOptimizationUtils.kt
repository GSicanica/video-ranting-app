package com.youtube.rating.shared.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple debouncer for network calls
 * Delays execution until no new events arrive within the specified time window
 * 
 * ✅ FIXED: Now accepts CoroutineScope for proper lifecycle management
 * 
 * Usage:
 * ```
 * val debouncer = Debouncer(500L, viewModelScope)
 * debouncer.debounce { performSearch() }
 * ```
 */
class Debouncer(
    private val delayMs: Long,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun debounce(action: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            action()
        }
    }

    fun cancel() {
        job?.cancel()
    }
}

/**
 * Request coalescer - batches multiple changes into single network call
 * Useful when user makes multiple filter changes rapidly
 * 
 * ✅ FIXED: Now accepts CoroutineScope for proper lifecycle management
 */
class RequestCoalescer<T>(
    private val delayMillis: Long = 500L,
    private val scope: CoroutineScope,
    private val onExecute: suspend (T) -> Unit
) {
    private var job: Job? = null
    private var pendingValue: T? = null

    fun submit(value: T) {
        pendingValue = value
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            pendingValue?.let { onExecute(it) }
        }
    }

    fun cancel() {
        job?.cancel()
        pendingValue = null
    }
}

/**
 * Simple request deduplicator - prevents duplicate simultaneous API calls
 * Tracks in-flight requests by string key
 */
class RequestDeduplicator {
    private val inFlightRequests = mutableSetOf<String>()
    private val mutex = Mutex()

    suspend fun shouldExecute(key: String): Boolean = mutex.withLock {
        if (key in inFlightRequests) {
            false // Duplicate request
        } else {
            inFlightRequests.add(key)
            true
        }
    }

    suspend fun markComplete(key: String) = mutex.withLock {
        inFlightRequests.remove(key)
    }

    suspend fun clear() = mutex.withLock {
        inFlightRequests.clear()
    }
}

/**
 * Smart retry with exponential backoff
 * Automatically retries failed network calls with increasing delays
 */
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000L,
    maxDelayMs: Long = 10000L,
    factor: Double = 2.0,
    shouldRetry: (Exception) -> Boolean = { true },
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            lastException = e
            
            // Don't retry if we've hit max attempts or if exception is not retryable
            if (attempt == maxRetries - 1 || !shouldRetry(e)) {
                throw e
            }

            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }

    throw lastException ?: IllegalStateException("Retry failed without exception")
}

/**
 * Network metrics tracker for monitoring optimization effectiveness
 */
class NetworkMetrics {
    private var totalCalls = 0
    private var cachedHits = 0
    private var deduplicatedCalls = 0
    private var failedCalls = 0
    private var totalLatencyMs = 0L

    fun logCall(
        endpoint: String,
        cached: Boolean = false,
        deduplicated: Boolean = false,
        latencyMs: Long = 0,
        success: Boolean = true
    ) {
        totalCalls++
        if (cached) cachedHits++
        if (deduplicated) deduplicatedCalls++
        if (!success) failedCalls++
        totalLatencyMs += latencyMs
    }

    fun reset() {
        totalCalls = 0
        cachedHits = 0
        deduplicatedCalls = 0
        failedCalls = 0
        totalLatencyMs = 0
    }

    fun getCacheHitRate(): Double =
        if (totalCalls > 0) cachedHits * 100.0 / totalCalls else 0.0
}
