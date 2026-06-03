package com.youtube.rating.android.domain.usecase.impl

import com.youtube.rating.android.domain.usecase.GetSuggestedBackupFilenameUseCase
import com.youtube.rating.android.utils.BackupService

class GetSuggestedBackupFilenameUseCaseImpl(
    private val backupService: BackupService
) : GetSuggestedBackupFilenameUseCase {
    override operator fun invoke(): String {
        return backupService.getSuggestedBackupFilename()
    }
}
