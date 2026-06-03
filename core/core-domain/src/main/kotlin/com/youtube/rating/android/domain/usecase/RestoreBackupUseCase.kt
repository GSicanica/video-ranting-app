package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.utils.RestoreResult

interface RestoreBackupUseCase {
    suspend operator fun invoke(backupPath: String): RestoreResult
}
