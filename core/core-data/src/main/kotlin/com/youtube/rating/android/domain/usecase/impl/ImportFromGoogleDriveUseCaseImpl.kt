package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.ImportFromGoogleDriveUseCase
import com.youtube.rating.android.utils.BackupService
import com.youtube.rating.android.utils.RestoreResult

class ImportFromGoogleDriveUseCaseImpl(
    private val backupService: BackupService
) : ImportFromGoogleDriveUseCase {
    override suspend operator fun invoke(uri: String): RestoreResult {
        return backupService.importFromGoogleDrive(uri)
    }
}
