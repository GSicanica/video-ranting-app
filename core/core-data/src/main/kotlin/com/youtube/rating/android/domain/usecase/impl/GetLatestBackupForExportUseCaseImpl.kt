package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.GetLatestBackupForExportUseCase
import com.youtube.rating.android.utils.BackupService

class GetLatestBackupForExportUseCaseImpl(
    private val backupService: BackupService
) : GetLatestBackupForExportUseCase {
    override suspend operator fun invoke(): String? {
        return backupService.getLatestBackupForExport()
    }
}
