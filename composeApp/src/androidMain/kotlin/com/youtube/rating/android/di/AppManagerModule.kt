package com.youtube.rating.android.di

import com.youtube.rating.android.AppBrightnessManager
import com.youtube.rating.android.cache.GospelCache
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.storage.FavoritesGatewayImpl
import com.youtube.rating.android.utils.AdminManager
import com.youtube.rating.android.utils.BackupTrigger
import com.youtube.rating.android.utils.BackupTriggerImpl
import com.youtube.rating.android.utils.SaintOfDayManager
import com.youtube.rating.android.utils.ViewPreferencesManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appManagerModule = module {
    single { AdminManager(context = androidContext()) }

    single {
        com.youtube.rating.android.utils.AnalyticsManager(
            context = androidContext(),
            apiClient = get(),
            userTokenManager = get(),
        )
    }

    single {
        ViewPreferencesManager.init(androidContext())
        ViewPreferencesManager
    }

    single {
        com.youtube.rating.android.storage.NotesManager(
            context = androidContext(),
            repository = get(qualifier = named("sharedNotesRepo")),
            backupTrigger = get(),
        )
    }

    single {
        com.youtube.rating.android.storage.FavoritesManager(
            context = androidContext(),
            repository = get(),
            autoBackup = get(),
        )
    }
    single<FavoritesGateway> { FavoritesGatewayImpl(manager = get()) }

    single { com.youtube.rating.android.storage.TaskManager(androidContext()) }
    single { com.youtube.rating.android.storage.BiblePlannerManager(androidContext()) }
    single {
        com.youtube.rating.android.storage.OfflineVideoManager(
            context = androidContext(),
            repository = get(qualifier = named("sharedOfflineVideosRepo")),
            backupTrigger = get(),
        )
    }

    single<com.youtube.rating.android.utils.BibleReminderService> {
        com.youtube.rating.android.utils.BibleReminderServiceImpl(get())
    }
    single<com.youtube.rating.android.utils.FastingReminderService> {
        com.youtube.rating.android.utils.FastingReminderServiceImpl()
    }

    single { com.youtube.rating.android.utils.AutoBackupManager(androidContext()) }
    single<BackupTrigger> { BackupTriggerImpl(autoBackupManager = get()) }

    single { com.youtube.rating.android.storage.RosaryManager(androidContext()) }

    single {
        com.youtube.rating.android.utils.GoogleDriveBackupManager(
            context = androidContext(),
            favoritesManager = get(),
            rosaryManager = get(),
            notesRepository = get(qualifier = named("sharedNotesRepo")),
            offlineVideosRepository = get(qualifier = named("sharedOfflineVideosRepo")),
            taskManager = get(),
        )
    }
    single<com.youtube.rating.android.utils.BackupService> {
        com.youtube.rating.android.utils.BackupServiceImpl(
            appContext = androidContext(),
            autoBackupManager = get(),
            googleDriveBackupManager = get(),
        )
    }

    single<com.youtube.rating.android.utils.NotificationScheduler> {
        com.youtube.rating.android.utils.NotificationSchedulerImpl()
    }
    single<com.youtube.rating.android.utils.DebugNotificationSender> {
        com.youtube.rating.android.utils.DebugNotificationSenderImpl()
    }
    single<com.youtube.rating.android.utils.DebugConfigProvider> {
        com.youtube.rating.android.utils.DebugConfigProviderImpl()
    }
    factory<com.youtube.rating.android.utils.ContentLanguageDialogController> {
        com.youtube.rating.android.utils.ContentLanguageDialogControllerImpl(get())
    }
    single<com.youtube.rating.android.notifications.NotificationRouter> {
        com.youtube.rating.android.notifications.NotificationRouterImpl()
    }

    single { GospelCache(apiClient = get()) }
    single { AppBrightnessManager(context = get()) }
    single { SaintOfDayManager(context = androidContext(), apiClient = get()) }
}
