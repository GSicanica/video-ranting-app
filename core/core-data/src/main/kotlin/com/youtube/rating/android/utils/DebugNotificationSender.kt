package com.youtube.rating.android.utils

import android.content.Context

interface DebugNotificationSender {
    sealed class Result {
        data object Success : Result()
        data object MissingPermission : Result()
        data class Failed(val error: String) : Result()
    }

    fun sendDebugTestNotifications(context: Context): Result
}
