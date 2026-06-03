package com.youtube.rating.android.sentry

import io.sentry.Breadcrumb
import io.sentry.ITransaction
import io.sentry.NoOpTransaction
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SpanStatus

object SentryLogger {
    private fun metricsHandle(): Any? {
        return try {
            Sentry::class.java.getMethod("metrics").invoke(null)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            null
        }
    }

    private fun invokeMetric(methodName: String, name: String, value: Double, tags: Map<String, String> = emptyMap()) {
        val metrics = metricsHandle() ?: return
        try {
            val method = if (tags.isNotEmpty()) {
                metrics.javaClass.methods.firstOrNull {
                    it.name == methodName && it.parameterTypes.size == 3
                }
            } else {
                null
            } ?: metrics.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterTypes.size == 2
            } ?: return

            if (method.parameterTypes.size == 3) {
                method.invoke(metrics, name, value, tags)
            } else {
                method.invoke(metrics, name, value)
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Never crash the app because telemetry failed.
        }
    }

    fun metricCount(name: String, value: Double = 1.0, tags: Map<String, String> = emptyMap()) {
        invokeMetric("count", name, value, tags)
    }

    fun metricGauge(name: String, value: Double, tags: Map<String, String> = emptyMap()) {
        invokeMetric("gauge", name, value, tags)
    }

    fun metricDistribution(name: String, value: Double, tags: Map<String, String> = emptyMap()) {
        invokeMetric("distribution", name, value, tags)
    }

    fun updateContext(tags: Map<String, String> = emptyMap(), extras: Map<String, Any?> = emptyMap()) {
        if (tags.isEmpty() && extras.isEmpty()) return
        try {
            Sentry.configureScope { scope ->
                tags.forEach { (k, v) -> scope.setTag(k, v) }
                extras.forEach { (k, v) -> scope.setExtra(k, v?.toString()) }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Never crash the app because telemetry failed.
        }
    }

    fun captureException(
        throwable: Throwable,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any?> = emptyMap()
    ) {
        try {
            Sentry.withScope { scope ->
                tags.forEach { (k, v) -> scope.setTag(k, v) }
                extras.forEach { (k, v) -> scope.setExtra(k, v?.toString()) }
                Sentry.captureException(throwable)
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Never crash the app because telemetry failed.
        }
    }

    fun captureMessage(
        message: String,
        level: SentryLevel = SentryLevel.INFO,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any?> = emptyMap()
    ) {
        try {
            Sentry.withScope { scope ->
                tags.forEach { (k, v) -> scope.setTag(k, v) }
                extras.forEach { (k, v) -> scope.setExtra(k, v?.toString()) }
                Sentry.captureMessage(message, level)
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Never crash the app because telemetry failed.
        }
    }

    fun addBreadcrumb(category: String, message: String, data: Map<String, String> = emptyMap()) {
        try {
            val breadcrumb = Breadcrumb().apply {
                this.category = category
                this.message = message
                data.forEach { (k, v) -> setData(k, v) }
            }
            Sentry.addBreadcrumb(breadcrumb)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Never crash the app because telemetry failed.
        }
    }

    fun startTransaction(name: String, op: String): ITransaction {
        return try {
            Sentry.startTransaction(name, op)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Fallback no-op transaction so callers don't need to special-case.
            NoOpTransaction.getInstance()
        }
    }

    fun finishTransaction(transaction: ITransaction, status: SpanStatus = SpanStatus.OK) {
        try {
            transaction.finish(status)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Never crash the app because telemetry failed.
        }
    }
}
