package com.youtube.rating.android.utils

import android.content.Context
import com.youtube.rating.android.storage.FastingReminderSettings

interface FastingReminderService {
    fun schedule(context: Context, settings: FastingReminderSettings)
}
