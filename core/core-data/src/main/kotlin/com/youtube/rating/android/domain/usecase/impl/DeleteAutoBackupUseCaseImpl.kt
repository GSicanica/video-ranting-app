package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.DeleteAutoBackupUseCase
import com.youtube.rating.android.utils.BackupService

class DeleteAutoBackupUseCaseImpl(
    private val backupService: BackupService
) : DeleteAutoBackupUseCase {
    override suspend operator fun invoke(filePath: String): Boolean {
        return backupService.deleteAutoBackup(filePath)
    }
}
