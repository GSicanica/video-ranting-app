package com.youtube.rating.core.generators

internal fun stableSeedFromString(input: String): Long {
    // Simple stable hash->seed; good enough for visual randomness.
    var h = 1125899906842597L
    for (c in input) h = 31L * h + c.code
    return h
}
