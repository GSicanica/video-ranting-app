package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.utils.RestoreResult

interface ImportFromGoogleDriveUseCase {
    suspend operator fun invoke(uri: String): RestoreResult
}
