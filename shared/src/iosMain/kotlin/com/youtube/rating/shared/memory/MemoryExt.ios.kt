package com.youtube.rating.shared.memory

actual fun getMemoryUsage(
    transform: (Long, Long, Long, Long, Int) -> String
): Pair<Int, String> {
    val total = 0L
    val free = 0L
    val max = 0L
    val usage = 0L
    val percent = 0
    return percent to transform(total, free, max, usage, percent)
}

