package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.ExportToGoogleDriveUseCase
import com.youtube.rating.android.utils.BackupService

class ExportToGoogleDriveUseCaseImpl(
    private val backupService: BackupService
) : ExportToGoogleDriveUseCase {
    override suspend operator fun invoke(uri: String): Result<Unit> {
        return backupService.exportToGoogleDrive(uri)
    }
}
