package com.youtube.rating.android.notifications

import android.annotation.SuppressLint
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
import com.youtube.rating.android.storage.BiblePlannerManager
import com.youtube.rating.android.utils.BibleReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class BibleReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // BroadcastReceiver runs on the main thread; keep it fast to avoid ANR.
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val koin = GlobalContext.getOrNull() ?: return@launch
                val manager = koin.get<BiblePlannerManager>()
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
                    BibleReminderScheduler.schedule(appContext, settings)
                    return@launch
                }

                val notification = NotificationCompat.Builder(appContext, NotificationChannels.CHANNEL_BIBLE_REMINDERS).apply {
                    val router: NotificationRouter = koin.get()
                    val pendingIntent = router.createContentPendingIntent(
                        context = appContext,
                        destination = NotificationDestination.GOSPEL_DAY,
                        requestCode = NOTIFICATION_ID
                    )
                    setSmallIcon(R.mipmap.ic_launcher)
                    setContentTitle("Podsjetnik za Evanđelje")
                    setContentText(context.getString(R.string.bible_reminder_text))
                    setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    setContentIntent(pendingIntent)
                    setAutoCancel(true)
                }.build()

                showNotification(appContext, notification)

                BibleReminderScheduler.schedule(appContext, settings)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 7312

        @SuppressLint("MissingPermission")
        private fun showNotification(context: Context, notification: android.app.Notification) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
