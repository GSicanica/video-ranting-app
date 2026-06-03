package com.youtube.rating.android.feature.running.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class RunningTrackingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                START_NOT_STICKY
            }
            else -> {
                val started = runCatching {
                    ensureNotificationChannel()
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        buildNotification(),
                        ServiceInfoFlags.LOCATION,
                    )
                }.isSuccess

                if (!started) {
                    stopSelf()
                }
                START_STICKY
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_RUNNING_TRACKING,
            "Trcanje GPS pracenje",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Prikazuje aktivnu GPS sesiju trcanja u pozadini."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPendingIntent = launchIntent?.let {
            androidx.core.app.PendingIntentCompat.getActivity(
                this,
                11001,
                it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )
        }

        val stopIntent = Intent(this, RunningTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = androidx.core.app.PendingIntentCompat.getService(
            this,
            11002,
            stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT,
            false,
        )

        return NotificationCompat.Builder(this, CHANNEL_RUNNING_TRACKING)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Trčanje - GPS aktivan")
            .setContentText("Praćenje rute je aktivno u pozadini")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Zaustavi", stopPendingIntent)
            .build()
    }

    private object ServiceInfoFlags {
        const val LOCATION = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    }

    companion object {
        const val CHANNEL_RUNNING_TRACKING = "running_tracking"
        const val ACTION_START = "com.youtube.rating.android.running.action.START"
        const val ACTION_STOP = "com.youtube.rating.android.running.action.STOP"
        const val NOTIFICATION_ID = 91021
    }
}
