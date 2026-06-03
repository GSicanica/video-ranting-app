package com.youtube.rating.android.di

import com.youtube.rating.android.domain.usecase.AutoBackupIfNeededUseCase
import com.youtube.rating.android.domain.usecase.CreateBackupUseCase
import com.youtube.rating.android.domain.usecase.DeleteAutoBackupUseCase
import com.youtube.rating.android.domain.usecase.ExportToGoogleDriveUseCase
import com.youtube.rating.android.domain.usecase.GetLatestBackupForExportUseCase
import com.youtube.rating.android.domain.usecase.GetSuggestedBackupFilenameUseCase
import com.youtube.rating.android.domain.usecase.ImportBackupFromExternalUseCase
import com.youtube.rating.android.domain.usecase.ImportFromGoogleDriveUseCase
import com.youtube.rating.android.domain.usecase.LoadAutoBackupsUseCase
import com.youtube.rating.android.domain.usecase.MigrationUseCase
import com.youtube.rating.android.domain.usecase.PerformPeriodicBackupUseCase
import com.youtube.rating.android.domain.usecase.RestoreBackupUseCase
import com.youtube.rating.android.domain.usecase.RestoreLatestBackupUseCase
import com.youtube.rating.android.domain.usecase.SetGoogleDriveAutoBackupUseCase
import com.youtube.rating.android.domain.usecase.SyncNewVideoNotificationsUseCase
import com.youtube.rating.android.domain.usecase.impl.AutoBackupIfNeededUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.CreateBackupUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.DeleteAutoBackupUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.ExportToGoogleDriveUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.GetLatestBackupForExportUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.GetSuggestedBackupFilenameUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.ImportBackupFromExternalUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.ImportFromGoogleDriveUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.LoadAutoBackupsUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.MigrationUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.PerformPeriodicBackupUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.RestoreBackupUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.RestoreLatestBackupUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.SetGoogleDriveAutoBackupUseCaseImpl
import com.youtube.rating.android.domain.usecase.impl.SyncNewVideoNotificationsUseCaseImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val backupUseCaseModule = module {
    single<MigrationUseCase> { MigrationUseCaseImpl(context = androidContext()) }
    single<AutoBackupIfNeededUseCase> { AutoBackupIfNeededUseCaseImpl(backupService = get()) }
    single<SyncNewVideoNotificationsUseCase> { SyncNewVideoNotificationsUseCaseImpl(context = androidContext()) }
    single<PerformPeriodicBackupUseCase> { PerformPeriodicBackupUseCaseImpl(backupService = get()) }
    single<LoadAutoBackupsUseCase> { LoadAutoBackupsUseCaseImpl(backupService = get()) }
    single<CreateBackupUseCase> { CreateBackupUseCaseImpl(backupService = get()) }
    single<RestoreBackupUseCase> { RestoreBackupUseCaseImpl(backupService = get()) }
    single<DeleteAutoBackupUseCase> { DeleteAutoBackupUseCaseImpl(backupService = get()) }
    single<SetGoogleDriveAutoBackupUseCase> { SetGoogleDriveAutoBackupUseCaseImpl(backupService = get()) }
    single<RestoreLatestBackupUseCase> { RestoreLatestBackupUseCaseImpl(backupService = get()) }
    single<GetLatestBackupForExportUseCase> { GetLatestBackupForExportUseCaseImpl(backupService = get()) }
    single<ImportBackupFromExternalUseCase> { ImportBackupFromExternalUseCaseImpl(backupService = get()) }
    single<ExportToGoogleDriveUseCase> { ExportToGoogleDriveUseCaseImpl(backupService = get()) }
    single<GetSuggestedBackupFilenameUseCase> { GetSuggestedBackupFilenameUseCaseImpl(backupService = get()) }
    single<ImportFromGoogleDriveUseCase> { ImportFromGoogleDriveUseCaseImpl(backupService = get()) }
}
