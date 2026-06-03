package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.LoadAutoBackupsUseCase
import com.youtube.rating.android.utils.AutoBackupInfo
import com.youtube.rating.android.utils.BackupService

class LoadAutoBackupsUseCaseImpl(
    private val backupService: BackupService
) : LoadAutoBackupsUseCase {
    override suspend operator fun invoke(): List<AutoBackupInfo> {
        return backupService.loadAutoBackups()
    }
}
