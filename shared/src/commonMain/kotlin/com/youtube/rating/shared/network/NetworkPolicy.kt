package com.youtube.rating.shared.network

import kotlin.random.Random

object NetworkPolicy {
    const val MAX_RETRIES = 3
    const val INITIAL_BACKOFF_MS = 500L
    const val MAX_BACKOFF_MS = 8_000L

    const val REQUEST_TIMEOUT_MS = 20_000L
    const val CONNECT_TIMEOUT_MS = 10_000L
    const val SOCKET_TIMEOUT_MS = 20_000L

    /**
     * Exponential backoff with ±25 % jitter to prevent thundering-herd retries.
     */
    fun nextBackoffMs(currentMs: Long): Long {
        val doubled = (currentMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        val jitter = (doubled * 0.25 * (Random.nextDouble() * 2 - 1)).toLong() // ±25 %
        return (doubled + jitter).coerceIn(INITIAL_BACKOFF_MS, MAX_BACKOFF_MS)
    }
}

