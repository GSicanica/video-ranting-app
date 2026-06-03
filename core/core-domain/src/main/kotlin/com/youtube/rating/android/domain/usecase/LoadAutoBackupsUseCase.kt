package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.utils.AutoBackupInfo

interface LoadAutoBackupsUseCase {
    suspend operator fun invoke(): List<AutoBackupInfo>
}
