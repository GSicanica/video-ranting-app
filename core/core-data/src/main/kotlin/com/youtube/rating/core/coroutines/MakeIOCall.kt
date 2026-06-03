package com.youtube.rating.core.coroutines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.sentry.SentryLogger
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

inline fun <T> ViewModel.makeIOCall(
    crossinline onCallExecuted: () -> Unit = {},
    crossinline onErrorAction: (throwable: Throwable) -> Unit = { _ -> },
    crossinline ioCall: suspend () -> T,
    crossinline onCalled: (model: T) -> Unit
): Job {
    return viewModelScope.launch(mainDispatcher) {
        supervisorScope {
            try {
                val task = async(ioDispatcher) { ioCall() }
                onCalled(task.await())
            } catch (t: Throwable) {
                onErrorAction(t)
            } finally {
                onCallExecuted()
            }
        }
    }
}

inline fun <T> CoroutineScope.makeIOCall(
    crossinline onCallExecuted: () -> Unit = {},
    crossinline onErrorAction: (throwable: Throwable) -> Unit = { _ -> },
    crossinline ioCall: suspend () -> T,
    crossinline onCalled: (model: T) -> Unit
): Job {
    return launch(mainDispatcher) {
        supervisorScope {
            try {
                val task = async(ioDispatcher) { ioCall() }
                onCalled(task.await())
            } catch (t: Throwable) {
                onErrorAction(t)
            } finally {
                onCallExecuted()
            }
        }
    }
}

inline fun ViewModel.makeIOCall(
    crossinline onCallExecuted: () -> Unit = {},
    crossinline ioCall: suspend () -> Unit
): Job {
    return viewModelScope.launch(mainDispatcher) {
        supervisorScope {
            try {
                val task = async(ioDispatcher) { ioCall() }
                task.await()
            } catch (t: Throwable) {
                SentryLogger.captureException(t)
                Logger.error("makeIOCall", "Unhandled exception in ViewModel.makeIOCall", t)
            } finally {
                onCallExecuted()
            }
        }
    }
}

inline fun ViewModel.makeIOCall(
    crossinline onCallExecuted: () -> Unit = {},
    crossinline onErrorAction: (throwable: Throwable) -> Unit = { _ -> },
    crossinline ioCall: suspend () -> Unit
): Job {
    return viewModelScope.launch(mainDispatcher) {
        supervisorScope {
            try {
                val task = async(ioDispatcher) { ioCall() }
                task.await()
            } catch (t: Throwable) {
                onErrorAction(t)
            } finally {
                onCallExecuted()
            }
        }
    }
}

inline fun CoroutineScope.makeIOCall(
    crossinline onCallExecuted: () -> Unit = {},
    crossinline ioCall: suspend () -> Unit
): Job {
    return launch(mainDispatcher) {
        supervisorScope {
            try {
                val task = async(ioDispatcher) { ioCall() }
                task.await()
            } catch (t: Throwable) {
                SentryLogger.captureException(t)
                Logger.error("makeIOCall", "Unhandled exception in CoroutineScope.makeIOCall", t)
            } finally {
                onCallExecuted()
            }
        }
    }
}

inline fun CoroutineScope.makeIOCall(
    crossinline onCallExecuted: () -> Unit = {},
    crossinline onErrorAction: (throwable: Throwable) -> Unit = { _ -> },
    crossinline ioCall: suspend () -> Unit
): Job {
    return launch(mainDispatcher) {
        supervisorScope {
            try {
                val task = async(ioDispatcher) { ioCall() }
                task.await()
            } catch (t: Throwable) {
                onErrorAction(t)
            } finally {
                onCallExecuted()
            }
        }
    }
}
