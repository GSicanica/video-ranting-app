package com.youtube.rating.android.utils

import android.content.Context
import com.youtube.rating.android.workers.WorkManagerHelper

class NotificationSchedulerImpl : NotificationScheduler {
    override fun scheduleNewVideoNotifications(context: Context) {
        WorkManagerHelper.scheduleNewVideoNotifications(context)
    }

    override fun cancelNewVideoNotifications(context: Context) {
        WorkManagerHelper.cancelNewVideoNotifications(context)
    }
}
