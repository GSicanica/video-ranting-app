package com.youtube.rating.android.utils

import android.os.SystemClock
import android.os.Trace
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.shared.utils.Logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Advanced coroutine tracing for debug builds.
 *
 * - Uses ContinuationInterceptor to track resume points (suspend state machine).
 * - Uses android.os.Trace for perf tools (Systrace/Perfetto).
 * - Cheap sampling to keep overhead low.
 */
object CoroutineTracing {
    private const val DEFAULT_SAMPLE_RATE = 0.1
    private val counter = AtomicLong(0)

    suspend fun <T> traceSuspend(
        name: String,
        sampleRate: Double = DEFAULT_SAMPLE_RATE,
        block: suspend () -> T
    ): T {
        if (!BuildConfig.DEBUG || !shouldSample(sampleRate = sampleRate)) {
            return block()
        }
        val interceptor = TraceContinuationInterceptor(name = name)
        val start = SystemClock.elapsedRealtimeNanos()
        Trace.beginSection("coro:$name")
        return try {
            withContext(interceptor) { block() }
        } finally {
            Trace.endSection()
            val tookMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
            Logger.debug("CoroutineTrace", "[$name] total=${"%.2f".format(tookMs)}ms id=${interceptor.id}")
        }
    }

    private fun shouldSample(sampleRate: Double): Boolean {
        if (sampleRate <= 0.0) return false
        if (sampleRate >= 1.0) return true
        return Random.nextDouble() <= sampleRate
    }

    private class TraceContinuationInterceptor(
        private val name: String
    ) : ContinuationInterceptor {
        override val key: CoroutineContext.Key<*> = ContinuationInterceptor
        val id: Long = counter.incrementAndGet()

        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
            return object : Continuation<T> {
                override val context: CoroutineContext = continuation.context
                override fun resumeWith(result: Result<T>) {
                    val start = SystemClock.elapsedRealtimeNanos()
                    Trace.beginSection("coro:$name#resume")
                    try {
                        continuation.resumeWith(result)
                    } finally {
                        Trace.endSection()
                        val tookMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                        Logger.debug(
                            "CoroutineTrace",
                            "[$name] resume=${"%.2f".format(tookMs)}ms id=$id"
                        )
                    }
                }
            }
        }
    }
}
