package com.youtube.rating.android.sentry

import com.youtube.rating.shared.utils.LogEntry
import com.youtube.rating.shared.utils.LogLevel
import com.youtube.rating.shared.utils.LogListener
import io.sentry.SentryLevel

class SentryLogListener : LogListener {
    override fun onLog(entry: LogEntry) {
        when (entry.level) {
            LogLevel.ERROR -> {
                entry.throwable?.let {
                    SentryLogger.captureException(it, tags = mapOf("tag" to entry.tag))
                } ?: SentryLogger.captureMessage(entry.message, SentryLevel.ERROR, tags = mapOf("tag" to entry.tag))
            }
            LogLevel.WARNING -> {
                entry.throwable?.let {
                    SentryLogger.captureException(it, tags = mapOf("tag" to entry.tag))
                } ?: SentryLogger.captureMessage(entry.message, SentryLevel.WARNING, tags = mapOf("tag" to entry.tag))
            }
            else -> Unit
        }
    }
}
