package com.youtube.rating.shared.utils

import kotlinx.datetime.Clock

/**
 * Centralized logging strategy for KMP
 * Supports different log levels and categories
 */
object Logger {
    
    var isEnabled: Boolean = true
    var minLevel: LogLevel = LogLevel.DEBUG
    
    // Thread-safe listener list – iterate without ConcurrentModificationException
    private val listeners = mutableListOf<LogListener>()

    fun addListener(listener: LogListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: LogListener) {
        listeners.remove(listener)
    }
    
    private fun snapshotListeners(): List<LogListener> =
        listeners.toList()
    
    fun debug(tag: String, message: String, throwable: Throwable? = null) {
        log(level = LogLevel.DEBUG, tag = tag, message = message, throwable = throwable)
    }
    
    fun info(tag: String, message: String, throwable: Throwable? = null) {
        log(level = LogLevel.INFO, tag = tag, message = message, throwable = throwable)
    }
    
    fun warning(tag: String, message: String, throwable: Throwable? = null) {
        log(level = LogLevel.WARNING, tag = tag, message = message, throwable = throwable)
    }
    
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log(level = LogLevel.ERROR, tag = tag, message = message, throwable = throwable)
    }
    
    fun network(tag: String, message: String) {
        log(level = LogLevel.NETWORK, tag = tag, message = message, throwable = null)
    }
    
    fun database(tag: String, message: String) {
        log(level = LogLevel.DATABASE, tag = tag, message = message, throwable = null)
    }
    
    fun ui(tag: String, message: String) {
        log(level = LogLevel.UI, tag = tag, message = message, throwable = null)
    }
    
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!isEnabled || level.priority < minLevel.priority) return
        
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val logEntry = LogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        // Platform-specific logging
        internalPlatformLog(entry = logEntry)
        
        // Notify listeners (snapshot to avoid CME)
        snapshotListeners().forEach { it.onLog(logEntry) }
    }
}

/**
 * Internal platform-specific logging - expect/actual function
 */
internal expect fun internalPlatformLog(entry: LogEntry)

/**
 * Log levels with priorities
 */
enum class LogLevel(val priority: Int, val symbol: String) {
    DEBUG(0, "🔍"),
    INFO(1, "ℹ️"),
    WARNING(2, "⚠️"),
    ERROR(3, "❌"),
    NETWORK(1, "🌐"),
    DATABASE(1, "💾"),
    UI(0, "🎨")
}

/**
 * Log entry data class
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable?
) {
    fun format(): String {
        val error = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
        return "${level.symbol} [$tag] $message$error"
    }
}

/**
 * Interface for log listeners (for crash reporting, analytics, etc.)
 */
interface LogListener {
    fun onLog(entry: LogEntry)
}
