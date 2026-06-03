package com.youtube.rating.android.domain.usecase

interface SetGoogleDriveAutoBackupUseCase {
    suspend operator fun invoke(enabled: Boolean, uri: String? = null)
}
