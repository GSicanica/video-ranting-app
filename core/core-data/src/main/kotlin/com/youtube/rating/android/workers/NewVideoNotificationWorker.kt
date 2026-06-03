package com.youtube.rating.android.workers

import android.annotation.SuppressLint
import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.youtube.rating.core.data.R
import com.youtube.rating.android.notifications.NotificationChannels
import com.youtube.rating.android.notifications.NotificationNavigator
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.utils.Logger
import com.youtube.rating.android.data.prefs.VideoPrefs

class NewVideoNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val api: RatingApiClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!VideoPrefs.getNewVideoNotificationsEnabled(applicationContext)) {
                return Result.success()
            }

            if (!hasNotificationPermission()) {
                return Result.success()
            }

            val latestIdAndTitle = runCatching {
                val item = api.getVideosList(page = 1, limit = 1).data.videos.firstOrNull()
                item?.id?.trim().orEmpty() to (item?.title ?: "")
            }.getOrElse {
                Logger.error("NewVideoNotificationWorker", "getVideosList failed, fallback to getAllVideos: ${it.message}")
                val fallback = api.getAllVideos().firstOrNull()
                fallback?.videoId?.trim().orEmpty() to (fallback?.videoTitle ?: "")
            }

            val latestId = latestIdAndTitle.first
            if (latestId.isBlank()) return Result.success()
            val latestTitle = latestIdAndTitle.second

            val lastSeenId = VideoPrefs.getNewVideoLastSeenId(applicationContext)
            if (lastSeenId.isNullOrBlank()) {
                VideoPrefs.setNewVideoLastSeenId(applicationContext, latestId)
                return Result.success()
            }

            if (lastSeenId == latestId) {
                return Result.success()
            }

            NotificationChannels.ensureChannels(applicationContext)
            val pendingIntent = NotificationNavigator.createOpenVideoPendingIntent(
                context = applicationContext,
                videoId = latestId,
                requestCode = latestId.hashCode()
            )

            val notification = NotificationCompat.Builder(
                applicationContext,
                NotificationChannels.CHANNEL_VIDEO_UPDATES
            )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Novi video je objavljen")
                .setContentText(latestTitle.ifBlank { "Otvori novi video" })
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            applicationContext.getString(
                                R.string.new_video_notification,
                                latestTitle.ifBlank { latestId }
                            )
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                showNotification(latestId.hashCode(), notification)
            } catch (e: SecurityException) {
                // Permission can be revoked at runtime; avoid crashing the Worker.
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error("NewVideoNotificationWorker", "notify SecurityException: ${e.message}")
                return Result.success()
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                Logger.error("NewVideoNotificationWorker", "notify failed: ${e.message}")
                return Result.success()
            }

            VideoPrefs.setNewVideoLastSeenId(applicationContext, latestId)
            Result.success()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("NewVideoNotificationWorker", "Failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(notificationId: Int, notification: Notification) {
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }
}
