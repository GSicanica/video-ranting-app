package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.utils.RestoreResult

interface ImportBackupFromExternalUseCase {
    suspend operator fun invoke(backupContent: String): RestoreResult
}
