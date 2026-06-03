package com.youtube.rating.android.utils

import com.youtube.rating.android.storage.BiblePlannerManager
import com.youtube.rating.android.storage.BibleReminderSettings

class BibleReminderServiceImpl(
    private val biblePlannerManager: BiblePlannerManager
) : BibleReminderService {
    override suspend fun getReminderSettings(): BibleReminderService.Settings {
        val settings = biblePlannerManager.getReminderSettings()
        return BibleReminderService.Settings(
            enabled = settings.enabled,
            hour = settings.hour,
            minute = settings.minute
        )
    }

    override suspend fun saveReminderSettings(settings: BibleReminderService.Settings) {
        biblePlannerManager.saveReminderSettings(
            BibleReminderSettings(
                enabled = settings.enabled,
                hour = settings.hour,
                minute = settings.minute
            )
        )
    }
}
