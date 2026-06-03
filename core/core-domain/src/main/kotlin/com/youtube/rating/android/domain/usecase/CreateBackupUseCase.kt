package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.utils.BackupResult

interface CreateBackupUseCase {
    suspend operator fun invoke(): BackupResult
}
