package com.youtube.rating.shared.memory

actual fun getMemoryUsage(
    transform: (Long, Long, Long, Long, Int) -> String
): Pair<Int, String> {
    val total = Runtime.getRuntime().totalMemory()
    val free = Runtime.getRuntime().freeMemory()
    val max = Runtime.getRuntime().maxMemory()
    val usage = total - free
    val percent = ((usage.toDouble() / max) * 100).toInt()
    return percent to transform(total, free, max, usage, percent)
}

