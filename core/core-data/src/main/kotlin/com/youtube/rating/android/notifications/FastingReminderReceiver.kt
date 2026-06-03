package com.youtube.rating.android.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.youtube.rating.core.data.R
import com.youtube.rating.android.storage.FastingManager
import com.youtube.rating.android.utils.FastingReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FastingReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // BroadcastReceiver runs on the main thread; keep it fast to avoid ANR.
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val manager = FastingManager.getInstance(appContext)
                val settings = manager.getReminderSettings()
                if (!settings.enabled) return@launch

                NotificationChannels.ensureChannels(appContext)

                val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                if (!canNotify) {
                    FastingReminderScheduler.schedule(appContext, settings)
                    return@launch
                }

                val notification = NotificationCompat.Builder(appContext, NotificationChannels.CHANNEL_FASTING_REMINDERS).apply {
                    val pendingIntent = NotificationNavigator.createContentPendingIntent(
                        context = appContext,
                        destination = NotificationNavigator.Destination.TRAINING,
                        requestCode = NOTIFICATION_ID
                    )
                    setSmallIcon(R.mipmap.ic_launcher)
                    setContentTitle("Podsjetnik za post")
                    setContentText(context.getString(R.string.fasting_reminder_text))
                    setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    setContentIntent(pendingIntent)
                    setAutoCancel(true)
                }.build()

                showNotification(appContext, notification)

                FastingReminderScheduler.schedule(appContext, settings)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 7202

        @SuppressLint("MissingPermission")
        private fun showNotification(context: Context, notification: Notification) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
