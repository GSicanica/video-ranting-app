package com.youtube.rating.android.domain.usecase

interface AutoBackupIfNeededUseCase {
    suspend operator fun invoke()
}

