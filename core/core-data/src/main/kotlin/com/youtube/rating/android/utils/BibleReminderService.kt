package com.youtube.rating.android.utils

interface BibleReminderService {
    data class Settings(
        val enabled: Boolean = false,
        val hour: Int = 20,
        val minute: Int = 0
    )

    suspend fun getReminderSettings(): Settings
    suspend fun saveReminderSettings(settings: Settings)
}
