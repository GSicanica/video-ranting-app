package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.RestoreBackupUseCase
import com.youtube.rating.android.utils.BackupService
import com.youtube.rating.android.utils.RestoreResult

class RestoreBackupUseCaseImpl(
    private val backupService: BackupService
) : RestoreBackupUseCase {
    override suspend operator fun invoke(backupPath: String): RestoreResult {
        return backupService.restoreBackup(backupPath)
    }
}
