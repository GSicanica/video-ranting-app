package com.youtube.rating.android.sentry

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

object SentryCoroutineExceptionHandler : CoroutineExceptionHandler {
    override val key: CoroutineContext.Key<*> = CoroutineExceptionHandler

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        SentryLogger.captureException(exception, tags = mapOf("source" to "coroutines"))
    }
}
