package com.youtube.rating.android.domain.usecase.impl

import android.content.Context
import com.youtube.rating.android.domain.usecase.SyncNewVideoNotificationsUseCase
import com.youtube.rating.android.workers.WorkManagerHelper

class SyncNewVideoNotificationsUseCaseImpl(
    private val context: Context
) : SyncNewVideoNotificationsUseCase {
    override suspend operator fun invoke() {
        WorkManagerHelper.syncNewVideoNotifications(context)
    }
}
