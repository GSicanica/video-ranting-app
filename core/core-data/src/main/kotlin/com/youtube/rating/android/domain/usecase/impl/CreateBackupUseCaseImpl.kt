package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.CreateBackupUseCase
import com.youtube.rating.android.utils.BackupResult
import com.youtube.rating.android.utils.BackupService

class CreateBackupUseCaseImpl(
    private val backupService: BackupService
) : CreateBackupUseCase {
    override suspend operator fun invoke(): BackupResult {
        return backupService.createBackup()
    }
}
