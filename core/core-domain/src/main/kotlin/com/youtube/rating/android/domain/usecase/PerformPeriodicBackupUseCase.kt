package com.youtube.rating.android.domain.usecase

interface PerformPeriodicBackupUseCase {
    suspend operator fun invoke()
}

