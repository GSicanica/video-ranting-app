package com.youtube.rating.shared.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Manages rate limiting information from API responses.
 * Thread-safe: all mutable state is guarded by [mutex].
 */
object RateLimitManager {
    private val mutex = Mutex()

    private var limit: Int = 100
    private var remaining: Int = 100
    private var resetTime: Long = 0L

    /**
     * Update rate limit information from response headers.
     */
    suspend fun updateLimits(newLimit: Int, newRemaining: Int, newResetTime: Long) {
        mutex.withLock {
            limit = newLimit
            remaining = newRemaining
            resetTime = newResetTime
        }
    }

    /**
     * Snapshot the current values without suspending.
     */
    private fun snapshot(): Triple<Int, Int, Long> = Triple(limit, remaining, resetTime)

    private fun nowSeconds(): Long = Clock.System.now().toEpochMilliseconds() / 1000

    /**
     * Check if rate limit is exceeded.
     */
    fun isRateLimitExceeded(): Boolean {
        val (_, rem, reset) = snapshot()
        return rem <= 0 && nowSeconds() < reset
    }

    /**
     * Get current rate limit status.
     */
    fun getRateLimitStatus(): RateLimitStatus {
        val (lim, rem, reset) = snapshot()
        val now = nowSeconds()
        val isExceeded = rem <= 0 && now < reset

        return RateLimitStatus(
            limit = lim,
            remaining = rem,
            resetTime = reset,
            isExceeded = isExceeded,
            resetInSeconds = if (now < reset) (reset - now).toInt() else 0
        )
    }

    /**
     * Get time until reset in seconds.
     */
    fun getTimeUntilReset(): Long {
        val now = nowSeconds()
        val reset = resetTime          // single volatile read
        return if (now < reset) reset - now else 0L
    }

    /** Reset to defaults (useful for testing). */
    suspend fun reset() {
        mutex.withLock {
            limit = 100
            remaining = 100
            resetTime = 0L
        }
    }
}

/**
 * Rate limit status information
 */
data class RateLimitStatus(
    val limit: Int,
    val remaining: Int,
    val resetTime: Long,
    val isExceeded: Boolean,
    val resetInSeconds: Int
)
