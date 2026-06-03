package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.SetGoogleDriveAutoBackupUseCase
import com.youtube.rating.android.utils.BackupService

class SetGoogleDriveAutoBackupUseCaseImpl(
    private val backupService: BackupService
) : SetGoogleDriveAutoBackupUseCase {
    override suspend operator fun invoke(enabled: Boolean, uri: String?) {
        backupService.setGoogleDriveAutoBackup(enabled, uri)
    }
}
