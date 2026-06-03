package com.youtube.rating.android.domain.usecase.impl

import android.content.Context
import com.youtube.rating.android.domain.usecase.MigrationUseCase
import com.youtube.rating.android.migration.DataMigration
import com.youtube.rating.android.migration.MigrationResult

class MigrationUseCaseImpl(
    private val context: Context
) : MigrationUseCase {
    override suspend operator fun invoke(): MigrationResult =
        DataMigration.migrateIfNeeded(context)
}
