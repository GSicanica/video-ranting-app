package com.youtube.rating.android.domain.usecase

interface ExportToGoogleDriveUseCase {
    suspend operator fun invoke(uri: String): Result<Unit>
}
