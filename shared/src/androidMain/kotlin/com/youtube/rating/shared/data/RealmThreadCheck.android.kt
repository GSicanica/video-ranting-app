package com.youtube.rating.shared.data

import android.os.Looper

actual fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread == Thread.currentThread()
}
