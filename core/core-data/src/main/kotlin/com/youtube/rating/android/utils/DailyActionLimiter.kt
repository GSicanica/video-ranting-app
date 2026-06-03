package com.youtube.rating.android.utils

import android.content.Context
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.youtube.rating.android.data.prefs.GenericPrefs

class DailyActionLimiter(context: Context) {
    private val appContext = context.applicationContext

    suspend fun canPerform(actionKey: String): Boolean {
        if (BuildConfig.DEBUG) return true
        return !getFlagWithMigration(actionKey = actionKeyForToday(actionKey))
    }

    suspend fun markPerformed(actionKey: String) {
        GenericPrefs.setBoolean(appContext, keyFor(actionKeyForToday(actionKey = actionKey)), true)
    }

    private fun keyFor(actionKey: String) = actionKey

    private suspend fun getFlagWithMigration(actionKey: String): Boolean {
        val key = keyFor(actionKey)
        val stored = GenericPrefs.getBoolean(appContext, key, false)
        if (stored) return true
        val baseKey = actionKey.substringBefore(":")
        val legacy = appContext.getSharedPreferences("one_time_action_limits", Context.MODE_PRIVATE)
            .getBoolean(baseKey, false)
        if (legacy) {
            GenericPrefs.setBoolean(appContext, key, true)
        }
        return legacy
    }

    private fun actionKeyForToday(actionKey: String): String {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "$actionKey:$today"
    }
}
