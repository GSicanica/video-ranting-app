package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

object FastingPrefs : BasePrefs() {
    private val KEY_FASTING_ENTRIES = stringPreferencesKey("fasting_entries")
    private val KEY_FASTING_WEEKLY_GOAL = intPreferencesKey("fasting_weekly_goal")
    private val KEY_FASTING_REMINDER_ENABLED = booleanPreferencesKey("fasting_reminder_enabled")
    private val KEY_FASTING_REMINDER_HOUR = intPreferencesKey("fasting_reminder_hour")
    private val KEY_FASTING_REMINDER_MINUTE = intPreferencesKey("fasting_reminder_minute")

    fun fastingEntriesFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_FASTING_ENTRIES, "[]")

    suspend fun getFastingEntriesJson(context: Context): String =
        readPref(context, KEY_FASTING_ENTRIES, "[]")

    suspend fun setFastingEntriesJson(context: Context, json: String) {
        editPref(context) { it[KEY_FASTING_ENTRIES] = json }
    }

    suspend fun getFastingWeeklyGoal(context: Context, defaultValue: Int = 3): Int =
        readPref(context, KEY_FASTING_WEEKLY_GOAL, defaultValue)

    suspend fun setFastingWeeklyGoal(context: Context, value: Int) {
        editPref(context) { it[KEY_FASTING_WEEKLY_GOAL] = value }
    }

    suspend fun getFastingReminderEnabled(context: Context): Boolean =
        readPref(context, KEY_FASTING_REMINDER_ENABLED, false)

    suspend fun getFastingReminderHour(context: Context, defaultValue: Int = 6): Int =
        readPref(context, KEY_FASTING_REMINDER_HOUR, defaultValue)

    suspend fun getFastingReminderMinute(context: Context, defaultValue: Int = 0): Int =
        readPref(context, KEY_FASTING_REMINDER_MINUTE, defaultValue)

    suspend fun setFastingReminder(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        editPref(context) {
            it[KEY_FASTING_REMINDER_ENABLED] = enabled
            it[KEY_FASTING_REMINDER_HOUR] = hour
            it[KEY_FASTING_REMINDER_MINUTE] = minute
        }
    }
}