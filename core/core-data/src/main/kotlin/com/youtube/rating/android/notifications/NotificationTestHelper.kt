package com.youtube.rating.android.notifications

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.youtube.rating.core.data.R
import org.koin.core.context.GlobalContext

object NotificationTestHelper {
    private const val TEST_GOSPEL_ID = 91001
    private const val TEST_FASTING_ID = 91002

    sealed class Result {
        data object Success : Result()
        data object MissingPermission : Result()
    }

    fun sendDebugTestNotifications(context: Context): Result {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasPermission) return Result.MissingPermission

        NotificationChannels.ensureChannels(context)

        val router: NotificationRouter = GlobalContext.get().get()
        val gospelPendingIntent = router.createContentPendingIntent(
            context = context,
            destination = NotificationDestination.GOSPEL_DAY,
            requestCode = TEST_GOSPEL_ID
        )
        val fastingPendingIntent = router.createContentPendingIntent(
            context = context,
            destination = NotificationDestination.TRAINING,
            requestCode = TEST_FASTING_ID
        )

        val bibleNotification = NotificationCompat.Builder(
            context,
            NotificationChannels.CHANNEL_BIBLE_REMINDERS
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Test: Evanđelje")
            .setContentText(context.getString(R.string.debug_test_notification_sent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(gospelPendingIntent)
            .setAutoCancel(true)
            .build()

        val fastingNotification = NotificationCompat.Builder(
            context,
            NotificationChannels.CHANNEL_FASTING_REMINDERS
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Test: Post")
            .setContentText(context.getString(R.string.debug_test_notification_sent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(fastingPendingIntent)
            .setAutoCancel(true)
            .build()

        showNotifications(context, bibleNotification, fastingNotification)
        return Result.Success
    }

    @SuppressLint("MissingPermission")
    private fun showNotifications(
        context: Context,
        bibleNotification: android.app.Notification,
        fastingNotification: android.app.Notification
    ) {
        val manager = NotificationManagerCompat.from(context)
        manager.notify(TEST_GOSPEL_ID, bibleNotification)
        manager.notify(TEST_FASTING_ID, fastingNotification)
    }
}
