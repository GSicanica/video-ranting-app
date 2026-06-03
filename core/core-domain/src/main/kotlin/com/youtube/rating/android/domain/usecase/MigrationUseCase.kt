package com.youtube.rating.android.domain.usecase

import com.youtube.rating.android.migration.MigrationResult

interface MigrationUseCase {
    suspend operator fun invoke(): MigrationResult
}

