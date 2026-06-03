package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.ImportBackupFromExternalUseCase
import com.youtube.rating.android.utils.BackupService
import com.youtube.rating.android.utils.RestoreResult

class ImportBackupFromExternalUseCaseImpl(
    private val backupService: BackupService
) : ImportBackupFromExternalUseCase {
    override suspend operator fun invoke(backupContent: String): RestoreResult {
        return backupService.importBackupFromExternal(backupContent)
    }
}
