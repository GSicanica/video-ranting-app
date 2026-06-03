package com.youtube.rating.android.utils

import android.content.Context
import com.youtube.rating.android.notifications.NotificationTestHelper

class DebugNotificationSenderImpl : DebugNotificationSender {
    override fun sendDebugTestNotifications(context: Context): DebugNotificationSender.Result {
        return when (NotificationTestHelper.sendDebugTestNotifications(context)) {
            NotificationTestHelper.Result.Success -> DebugNotificationSender.Result.Success
            NotificationTestHelper.Result.MissingPermission -> DebugNotificationSender.Result.MissingPermission
        }
    }
}
