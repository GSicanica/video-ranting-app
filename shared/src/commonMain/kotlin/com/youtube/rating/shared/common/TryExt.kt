package com.youtube.rating.shared.common

inline fun tryOrIgnore(block: () -> Unit) {
    try {
        block()
    } catch (_: Throwable) {
        // ignore
    }
}

