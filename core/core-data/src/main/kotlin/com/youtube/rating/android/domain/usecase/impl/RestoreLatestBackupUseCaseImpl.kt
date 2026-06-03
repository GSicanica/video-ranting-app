package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.RestoreLatestBackupUseCase
import com.youtube.rating.android.utils.BackupService
import com.youtube.rating.android.utils.RestoreResult

class RestoreLatestBackupUseCaseImpl(
    private val backupService: BackupService
) : RestoreLatestBackupUseCase {
    override suspend operator fun invoke(): RestoreResult {
        return backupService.restoreLatestBackup()
    }
}
