package com.youtube.rating.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.youtube.rating.android.domain.usecase.PerformPeriodicBackupUseCase
import com.youtube.rating.shared.utils.Logger

/**
 * ✅ WorkManager Worker za periodične backupe
 * 
 * Izvršava se u pozadini svaka 24h (konfigurabilno)
 * Lifecycle-aware - automatski se cancela kada app nije u upotrebi
 * Preživljava app restarts i system reboots
 * 
 * Usage:
 * ```kotlin
 * val backupWork = PeriodicWorkRequestBuilder<BackupWorker>(
 *     repeatInterval = 24,
 *     repeatIntervalTimeUnit = TimeUnit.HOURS
 * )
 * .setConstraints(
 *     Constraints.Builder()
 *         .setRequiresBatteryNotLow(true)  // Only when battery OK
 *         .setRequiresStorageNotLow(true)  // Only when storage OK
 *         .build()
 * )
 * .build()
 * 
 * WorkManager.getInstance(context).enqueueUniquePeriodicWork(
 *     "periodic_backup",
 *     ExistingPeriodicWorkPolicy.KEEP,
 *     backupWork
 * )
 * ```
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
    private val performPeriodicBackup: PerformPeriodicBackupUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Logger.info("BackupWorker", "Starting periodic backup...")
            
            performPeriodicBackup()
            
            Logger.info("BackupWorker", "✅ Periodic backup completed successfully")
            Result.success()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("BackupWorker", "❌ Periodic backup failed: ${e.message}")
            
            // Retry on failure (WorkManager će automatski retry-ati)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
