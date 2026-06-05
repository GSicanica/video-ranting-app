package com.youtube.rating.android.di

import com.youtube.rating.android.viewmodel.AnalyticsViewModel
import com.youtube.rating.android.viewmodel.BibleHomeViewModel
import com.youtube.rating.android.viewmodel.LocalBibleBookViewModel
import com.youtube.rating.android.viewmodel.LocalBibleChapterViewModel
import com.youtube.rating.android.viewmodel.LocalBibleLibraryViewModel
import com.youtube.rating.android.viewmodel.OfflineVideoPlayerViewModel
import com.youtube.rating.android.viewmodel.PrayerViewModel
import com.youtube.rating.android.viewmodel.SaintsViewModel
import com.youtube.rating.android.viewmodel.VideoDetailsDepsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appViewModelModule = module {
    viewModel {
        PrayerViewModel(appContext = androidContext(), repo = get())
    }

    viewModel {
        SaintsViewModel(appContext = androidContext())
    }

    viewModel {
        AnalyticsViewModel(
            apiClient = get(),
            userTokenManager = get(),
        )
    }

    viewModel { LocalBibleLibraryViewModel() }
    viewModel { LocalBibleBookViewModel() }
    viewModel { LocalBibleChapterViewModel(apiClient = get()) }

    viewModel {
        com.youtube.rating.android.viewmodel.SettingsViewModel(
            settingsRepository = get(),
            loadAutoBackupsUseCase = get(),
            createBackupUseCase = get(),
            restoreBackupUseCase = get(),
            deleteAutoBackupUseCase = get(),
            setGoogleDriveAutoBackupUseCase = get(),
            restoreLatestBackupUseCase = get(),
            getLatestBackupForExportUseCase = get(),
            importBackupFromExternalUseCase = get(),
            exportToGoogleDriveUseCase = get(),
            getSuggestedBackupFilenameUseCase = get(),
            importFromGoogleDriveUseCase = get(),
            notificationScheduler = get(),
            backupTrigger = get(),
            apiClient = get(),
            userTokenManager = get(),
            bibleReminderService = get(),
        )
    }

    viewModel { com.youtube.rating.android.viewmodel.FavoritesViewModel(get()) }
    viewModel { com.youtube.rating.android.viewmodel.GalleryViewModel(get()) }
    viewModel { BibleHomeViewModel(app = androidApplication()) }
    viewModel { com.youtube.rating.android.viewmodel.RosaryViewModel(get()) }
    viewModel { com.youtube.rating.android.viewmodel.NotesViewModel(get()) }

    viewModel { com.youtube.rating.android.viewmodel.OfflineViewModel(offlineVideoManager = get()) }

    viewModel {
        com.youtube.rating.android.viewmodel.AppViewModel(
            androidApplication(),
            get(),
            get(),
        )
    }

    viewModel {
        com.youtube.rating.android.viewmodel.WatchHistoryViewModel(
            manager = get(),
            repository = get(),
        )
    }

    factory {
        VideoDetailsDepsViewModel(
            apiClient = get(),
            userTokenManager = get(),
            youTubeInfoService = get(),
            watchHistoryRepository = get(),
        )
    }

    viewModel {
        OfflineVideoPlayerViewModel(watchHistoryRepository = get())
    }
}
