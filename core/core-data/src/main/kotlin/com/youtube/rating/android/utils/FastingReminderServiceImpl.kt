package com.youtube.rating.android.utils

import android.content.Context
import com.youtube.rating.android.storage.FastingReminderSettings

class FastingReminderServiceImpl : FastingReminderService {
    override fun schedule(context: Context, settings: FastingReminderSettings) {
        FastingReminderScheduler.schedule(context, settings)
    }
}
