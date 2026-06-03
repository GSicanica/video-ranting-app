package com.youtube.rating.android.storage

import android.content.Context
import com.youtube.rating.android.utils.BibleReminderScheduler
import com.youtube.rating.android.data.prefs.BiblePlannerPrefs

data class BibleReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0
)

class BiblePlannerManager(private val context: Context) {
    suspend fun getReminderSettings(): BibleReminderSettings {
        return BibleReminderSettings(
            enabled = BiblePlannerPrefs.getReminderEnabled(context),
            hour = BiblePlannerPrefs.getReminderHour(context),
            minute = BiblePlannerPrefs.getReminderMinute(context)
        )
    }

    suspend fun saveReminderSettings(settings: BibleReminderSettings) {
        val hour = settings.hour.coerceIn(0, 23)
        val minute = settings.minute.coerceIn(0, 59)
        BiblePlannerPrefs.setReminderTime(context, settings.enabled, hour, minute)
        BibleReminderScheduler.schedule(context, settings.copy(hour = hour, minute = minute))
    }

}
