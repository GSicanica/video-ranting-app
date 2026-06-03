package com.youtube.rating.android.domain.usecase

interface SyncNewVideoNotificationsUseCase {
    suspend operator fun invoke()
}

