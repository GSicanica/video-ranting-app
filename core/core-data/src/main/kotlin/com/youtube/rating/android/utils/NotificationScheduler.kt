package com.youtube.rating.android.utils

import android.content.Context

interface NotificationScheduler {
    fun scheduleNewVideoNotifications(context: Context)
    fun cancelNewVideoNotifications(context: Context)
}
