package com.youtube.rating.shared.utils

/**
 * Desktop/JVM-specific logging implementation
 */
internal actual fun internalPlatformLog(entry: LogEntry) {
    // Simple console logging for desktop
    println(entry.format())
    
    entry.throwable?.printStackTrace()
}
