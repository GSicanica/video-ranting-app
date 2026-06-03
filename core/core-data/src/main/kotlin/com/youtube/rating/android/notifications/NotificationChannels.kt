package com.youtube.rating.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_BIBLE_REMINDERS = "bible_reminders"
    const val CHANNEL_FASTING_REMINDERS = "fasting_reminders"
    const val CHANNEL_VIDEO_UPDATES = "video_updates"
    const val CHANNEL_RUNNING_TRACKING = "running_tracking"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_BIBLE_REMINDERS,
                "Evanđelje podsjetnici",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Podsjetnici za dnevno čitanje evanđelja."
            },
            NotificationChannel(
                CHANNEL_FASTING_REMINDERS,
                "Post podsjetnici",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Podsjetnici za post."
            },
            NotificationChannel(
                CHANNEL_VIDEO_UPDATES,
                "Novi video obavijesti",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Obavijesti kada je objavljen novi video."
            },
            NotificationChannel(
                CHANNEL_RUNNING_TRACKING,
                "Trčanje GPS praćenje",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Prikazuje aktivnu GPS sesiju trčanja u pozadini."
            }
        )
        manager.createNotificationChannels(channels)
    }
}
