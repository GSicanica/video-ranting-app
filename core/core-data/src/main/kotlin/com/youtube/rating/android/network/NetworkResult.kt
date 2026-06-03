package com.youtube.rating.android.network

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : NetworkResult<Nothing>
    data class Retryable(
        val message: String,
        val throwable: Throwable? = null,
        val retryAfterMs: Long? = null
    ) : NetworkResult<Nothing>
}

inline fun <T> NetworkResult<T>.onSuccess(block: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) block(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(block: (String, Throwable?) -> Unit): NetworkResult<T> {
    when (this) {
        is NetworkResult.Error -> block(message, throwable)
        is NetworkResult.Retryable -> block(message, throwable)
        is NetworkResult.Success -> Unit
    }
    return this
}

fun Throwable.toNetworkResult(
    defaultMessage: String = "Network request failed",
    isRetryable: (Throwable) -> Boolean = { false },
    retryAfterMs: Long? = null
): NetworkResult<Nothing> {
    val message = this.message ?: defaultMessage
    return if (isRetryable(this)) {
        NetworkResult.Retryable(message = message, throwable = this, retryAfterMs = retryAfterMs)
    } else {
        NetworkResult.Error(message = message, throwable = this)
    }
}
