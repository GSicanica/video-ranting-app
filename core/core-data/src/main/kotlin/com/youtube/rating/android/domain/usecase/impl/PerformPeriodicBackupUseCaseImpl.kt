package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.PerformPeriodicBackupUseCase
import com.youtube.rating.android.utils.BackupService

class PerformPeriodicBackupUseCaseImpl(
    private val backupService: BackupService
) : PerformPeriodicBackupUseCase {
    override suspend operator fun invoke() {
        backupService.createBackup()
    }
}
