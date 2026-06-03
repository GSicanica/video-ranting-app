package com.youtube.rating.android.domain.usecase

interface GetLatestBackupForExportUseCase {
    suspend operator fun invoke(): String?
}
