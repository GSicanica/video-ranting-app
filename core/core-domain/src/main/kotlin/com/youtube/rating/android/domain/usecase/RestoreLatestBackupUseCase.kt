package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.utils.RestoreResult

interface RestoreLatestBackupUseCase {
    suspend operator fun invoke(): RestoreResult
}
