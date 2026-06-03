package com.youtube.rating.shared.utils

import platform.Foundation.NSLog

/**
 * iOS-specific logging implementation using NSLog
 */
internal actual fun internalPlatformLog(entry: LogEntry) {
    val formattedMessage = entry.format()
    
    // NSLog automatically adds timestamp and process info
    NSLog(formattedMessage)
    
    // Print stack trace for errors
    entry.throwable?.printStackTrace()
}
