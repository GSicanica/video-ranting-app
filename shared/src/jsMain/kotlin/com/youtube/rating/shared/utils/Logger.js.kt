package com.youtube.rating.shared.utils

/**
 * JS/browser logging implementation.
 *
 * `println()` is routed to the browser console by Kotlin/JS.
 */
internal actual fun internalPlatformLog(entry: LogEntry) {
    println(entry.format())
}

