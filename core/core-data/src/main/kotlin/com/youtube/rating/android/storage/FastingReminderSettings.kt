package com.youtube.rating.android.storage

data class FastingReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 6,
    val minute: Int = 0
)
