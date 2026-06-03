package com.youtube.rating.shared.data

import platform.Foundation.NSThread

actual fun isMainThread(): Boolean {
    return NSThread.isMainThread
}
