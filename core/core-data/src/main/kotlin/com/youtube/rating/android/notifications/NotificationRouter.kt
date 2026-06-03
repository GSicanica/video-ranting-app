package com.youtube.rating.android.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

interface NotificationRouter {
    fun createContentPendingIntent(context: Context, destination: NotificationDestination, requestCode: Int): PendingIntent
    fun createOpenVideoPendingIntent(context: Context, videoId: String, requestCode: Int): PendingIntent
    fun resolveDestination(intent: Intent?): NotificationDestination?
}
