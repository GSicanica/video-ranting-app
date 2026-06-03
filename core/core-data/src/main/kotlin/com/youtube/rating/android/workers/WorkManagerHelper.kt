package com.youtube.rating.android.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.youtube.rating.shared.utils.Logger
import java.util.concurrent.TimeUnit
import com.youtube.rating.android.data.prefs.VideoPrefs
import com.youtube.rating.android.utils.AppScope
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * ✅ Helper object za WorkManager setup
 * 
 * Upravlja schedulingom background tasks-ova:
 * - Periodični backupi (svaka 24h)
 * - Database cleanup
 * - Cache cleanup
 * 
 * Sve task-ove su lifecycle-aware i battery-friendly
 */
object WorkManagerHelper {
    
    private const val BACKUP_WORK_NAME = "periodic_backup"
    private const val NEW_VIDEO_NOTIFY_WORK_NAME = "new_video_notify"
    private const val NEW_VIDEO_NOTIFY_ONCE_WORK_NAME = "new_video_notify_once"
    private const val HOME_CACHE_CLEANUP_WORK_NAME = "home_cache_cleanup"
    
    /**
     * Inicijalizuje sve periodične task-ove
     * 
     * Poziva se iz Application.onCreate()
     * Task-ovi preživljavaju app restarts i system reboots
     */
    fun schedulePeriodicTasks(context: Context) {
        val perfProfile = com.youtube.rating.android.utils.PerformanceProfile.get(context)
        schedulePeriodicBackup(context = context)
        scheduleHomeCacheCleanup(context = context)
        Logger.info("WorkManager", "✅ Periodic tasks scheduled")
    }

    fun syncNewVideoNotifications(context: Context) {
        // Run in background; don't block caller
        AppScope.get().makeIOCall {
            val enabled = runCatching { VideoPrefs.getNewVideoNotificationsEnabled(context) }
                .getOrDefault(false)
            if (enabled) {
                scheduleNewVideoNotifications(context = context)
            } else {
                cancelNewVideoNotifications(context = context)
            }
        }
    }

    fun scheduleNewVideoNotifications(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<NewVideoNotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(NEW_VIDEO_NOTIFY_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NEW_VIDEO_NOTIFY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )

        // Run one check immediately when enabled/synced so user does not wait for periodic window.
        val oneTimeWork = OneTimeWorkRequestBuilder<NewVideoNotificationWorker>()
            .setConstraints(constraints)
            .addTag(NEW_VIDEO_NOTIFY_ONCE_WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            NEW_VIDEO_NOTIFY_ONCE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWork
        )
        Logger.info("WorkManager", "New video notification work scheduled")
    }

    fun cancelNewVideoNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NEW_VIDEO_NOTIFY_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(NEW_VIDEO_NOTIFY_ONCE_WORK_NAME)
        Logger.info("WorkManager", "New video notification work cancelled")
    }

    /**
     * Schedule periodični backup svaka 24h
     * 
     * Constraints:
     * - Battery not low (ne izvršava se ako je baterija slaba)
     * - Storage not low (ne izvršava se ako je storage pun)
     * 
     * Retry policy:
     * - Do 3 pokušaja ako fail-a
     * - Exponential backoff između pokušaja
     */
    private fun schedulePeriodicBackup(context: Context) {
        val perfProfile = com.youtube.rating.android.utils.PerformanceProfile.get(context)
        val repeatHours = if (perfProfile.deviceClass == com.youtube.rating.android.utils.PerformanceProfile.DeviceClass.LOW) 48L else 24L
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)   // Battery-friendly
            .setRequiresStorageNotLow(true)   // Storage-aware
            .setRequiresCharging(true)        // Heavy I/O: run only when charging
            .build()
        
        val backupWork = PeriodicWorkRequestBuilder<BackupWorker>(
            repeatInterval = repeatHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(BACKUP_WORK_NAME)
            .build()
        
        // KEEP policy - ne cancela postojeći work ako je već scheduled
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWork
        )
        
        Logger.info("WorkManager", "Periodic backup scheduled (every ${repeatHours}h)")
    }

    private fun scheduleHomeCacheCleanup(context: Context) {
        val work = PeriodicWorkRequestBuilder<HomeCacheCleanupWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HOME_CACHE_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }
    
    /**
     * Cancel sve scheduled task-ove
     * 
     * Korisno za testing ili user preference (disable auto backup)
     */
    fun cancelPeriodicBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
        Logger.info("WorkManager", "Periodic backup cancelled")
    }
    
    /**
     * Provjeri status scheduled task-ova
     * 
     * Korisno za debug ili UI prikaz
     */
    fun getBackupWorkStatus(context: Context) {
        AppScope.get().makeIOCall {
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosForUniqueWork(BACKUP_WORK_NAME)
            runCatching { workInfos.get(5, TimeUnit.SECONDS) }
                .onSuccess { infos ->
                    infos.forEach { workInfo ->
                        Logger.debug("WorkManager", "Backup work status: ${workInfo.state}")
                    }
                }
                .onFailure { e ->
                    Logger.error("WorkManager", "Failed to query backup work status", e)
                }
        }
    }
}
