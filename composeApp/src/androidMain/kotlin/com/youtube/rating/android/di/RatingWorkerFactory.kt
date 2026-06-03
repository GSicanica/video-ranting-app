package com.youtube.rating.android.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.youtube.rating.android.domain.usecase.PerformPeriodicBackupUseCase
import com.youtube.rating.android.workers.BackupWorker
import com.youtube.rating.android.workers.HomeCacheCleanupWorker
import com.youtube.rating.android.workers.NewVideoNotificationWorker
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.data.HomeScreenCacheRepository
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named

class RatingWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val koin = GlobalContext.getOrNull() ?: return null
        return when (workerClassName) {
            NewVideoNotificationWorker::class.java.name -> NewVideoNotificationWorker(
                appContext = appContext,
                workerParams = workerParameters,
                api = koin.get<RatingApiClient>()
            )

            BackupWorker::class.java.name -> BackupWorker(
                context = appContext,
                params = workerParameters,
                performPeriodicBackup = koin.get<PerformPeriodicBackupUseCase>()
            )

            HomeCacheCleanupWorker::class.java.name -> HomeCacheCleanupWorker(
                appContext,
                workerParameters,
                koin.get<HomeScreenCacheRepository>(qualifier = named("sharedHomeCacheRepo"))
            )

            else -> null
        }
    }
}
