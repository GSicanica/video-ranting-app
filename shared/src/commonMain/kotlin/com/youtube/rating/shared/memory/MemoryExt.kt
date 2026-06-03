package com.youtube.rating.shared.memory

/**
 * Returns (percentUsed, formattedString). The [transform] receives:
 * totalBytes, freeBytes, maxBytes, usedBytes, percentUsed
 */
expect fun getMemoryUsage(
    transform: (Long, Long, Long, Long, Int) -> String
): Pair<Int, String>

fun getMemoryUsage(): Pair<Int, String> = getMemoryUsage { _, _, max, usage, percent ->
    "${usage / (1024 * 1024)} / ${max / (1024 * 1024)} MB in use ($percent%)"
}

private val fileSizeUnits = arrayOf("bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")

fun Long.toFileSizeString(): String {
    var bytesToCalculate = this.coerceAtLeast(0)
    var index = 0
    while (index < fileSizeUnits.size - 1) {
        if (bytesToCalculate < 1024) break
        bytesToCalculate /= 1024
        index++
    }
    return bytesToCalculate.toString() + " " + fileSizeUnits[index]
}

fun Long.toFileSize(): Long {
    var bytesToCalculate = this.coerceAtLeast(0)
    var index = 0
    while (index < fileSizeUnits.size - 1) {
        if (bytesToCalculate < 1024) break
        bytesToCalculate /= 1024
        index++
    }
    return bytesToCalculate
}
