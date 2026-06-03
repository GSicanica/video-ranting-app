package com.youtube.rating.shared.utils

import android.util.Log

/**
 * Android-specific logging implementation
 */
internal actual fun internalPlatformLog(entry: LogEntry) {
    val message = entry.message
    val tag = entry.tag
    
    when (entry.level) {
        LogLevel.DEBUG, LogLevel.UI -> {
            if (entry.throwable != null) {
                Log.d(tag, message, entry.throwable)
            } else {
                Log.d(tag, message)
            }
        }
        LogLevel.INFO, LogLevel.NETWORK, LogLevel.DATABASE -> {
            if (entry.throwable != null) {
                Log.i(tag, message, entry.throwable)
            } else {
                Log.i(tag, message)
            }
        }
        LogLevel.WARNING -> {
            if (entry.throwable != null) {
                Log.w(tag, message, entry.throwable)
            } else {
                Log.w(tag, message)
            }
        }
        LogLevel.ERROR -> {
            if (entry.throwable != null) {
                Log.e(tag, message, entry.throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
}

