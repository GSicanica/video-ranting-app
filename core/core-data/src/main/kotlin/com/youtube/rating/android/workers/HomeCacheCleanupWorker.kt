package com.youtube.rating.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.youtube.rating.shared.data.DataResult
import com.youtube.rating.shared.data.HomeScreenCacheRepository
import com.youtube.rating.shared.utils.Logger

class HomeCacheCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repo: HomeScreenCacheRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val deleted = when (val result = repo.deleteExpiredEntries()) {
                is DataResult.Success -> result.data
                else -> 0
            }
            Logger.info("HomeCacheCleanup", "Expired entries cleaned: $deleted")
            Result.success()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error("HomeCacheCleanup", "Cleanup failed", e)
            Result.retry()
        }
    }
}
