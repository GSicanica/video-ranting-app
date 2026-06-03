package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.AutoBackupIfNeededUseCase
import com.youtube.rating.android.utils.BackupService

class AutoBackupIfNeededUseCaseImpl(
    private val backupService: BackupService
) : AutoBackupIfNeededUseCase {
    override suspend operator fun invoke() {
        backupService.autoBackupIfNeeded()
    }
}
