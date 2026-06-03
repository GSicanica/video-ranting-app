package com.youtube.rating.android.domain.usecase

interface DeleteAutoBackupUseCase {
    suspend operator fun invoke(filePath: String): Boolean
}
