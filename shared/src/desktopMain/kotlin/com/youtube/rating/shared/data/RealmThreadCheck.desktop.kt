package com.youtube.rating.shared.data

actual fun isMainThread(): Boolean {
    // Desktop/JVM doesn't have a single UI main thread guarantee here.
    return false
}
