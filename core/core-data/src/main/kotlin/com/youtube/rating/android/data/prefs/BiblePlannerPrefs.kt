package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

/**
 * Bible planner preferences module
 * 
 * Manages all preferences related to the Bible planner feature:
 * - Daily reading count and goal
 * - Streak tracking
 * - Notes and reminders
 */
object BiblePlannerPrefs : BasePrefs() {
    
    // ---- Key Definitions ----
    private val KEY_READ_TODAY_COUNT = intPreferencesKey("bible_planner_read_today_count")
    private val KEY_READ_TODAY_DATE = stringPreferencesKey("bible_planner_read_today_date")
    private val KEY_GOAL_MIN = intPreferencesKey("bible_planner_goal_min")
    private val KEY_NOTE = stringPreferencesKey("bible_planner_note")
    private val KEY_STREAK = intPreferencesKey("bible_planner_streak")
    private val KEY_LAST_DATE = stringPreferencesKey("bible_planner_last_date")
    private val KEY_REMINDER_ENABLED = booleanPreferencesKey("bible_planner_reminder_enabled")
    private val KEY_REMINDER_HOUR = intPreferencesKey("bible_planner_reminder_hour")
    private val KEY_REMINDER_MINUTE = intPreferencesKey("bible_planner_reminder_minute")
    
    // ---- Read Today Count ----
    
    fun readTodayFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_READ_TODAY_COUNT, 0)
    
    suspend fun getReadToday(context: Context): Int =
        readPref(context, KEY_READ_TODAY_COUNT, 0)
    
    suspend fun incrementReadToday(context: Context, delta: Int = 1) {
        ensureReadTodayDate(context = context)
        val current = getReadToday(context = context)
        editPref(context) { it[KEY_READ_TODAY_COUNT] = (current + delta).coerceAtLeast(0) }
        TrainingPrefs.updateTrainingStatsToday(context)
    }
    
    suspend fun resetReadToday(context: Context) {
        ensureReadTodayDate(context = context)
        editPref(context) { it[KEY_READ_TODAY_COUNT] = 0 }
        TrainingPrefs.updateTrainingStatsToday(context)
    }
    
    // ---- Goal ----
    
    fun goalFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_GOAL_MIN, 15)
    
    suspend fun getGoal(context: Context): Int =
        readPref(context, KEY_GOAL_MIN, 15)
    
    suspend fun setGoal(context: Context, value: Int) {
        editPref(context) { it[KEY_GOAL_MIN] = value }
    }
    
    // ---- Streak ----
    
    fun streakFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_STREAK, 0)
    
    suspend fun getStreak(context: Context): Int =
        readPref(context, KEY_STREAK, 0)
    
    suspend fun setStreak(context: Context, value: Int) {
        editPref(context) { it[KEY_STREAK] = value }
    }
    
    // ---- Note ----
    
    fun noteFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_NOTE, "")
    
    suspend fun getNote(context: Context): String =
        readPref(context, KEY_NOTE, "")
    
    suspend fun setNote(context: Context, value: String) {
        editPref(context) { it[KEY_NOTE] = value }
    }
    
    // ---- Last Date ----
    
    fun lastDateFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_LAST_DATE, "")
    
    suspend fun getLastDate(context: Context): String =
        readPref(context, KEY_LAST_DATE, "")
    
    suspend fun setLastDate(context: Context, value: String) {
        editPref(context) { it[KEY_LAST_DATE] = value }
    }
    
    // ---- Reminders ----
    
    fun reminderEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_REMINDER_ENABLED, false)

    suspend fun setReminderEnabled(context: Context, value: Boolean) {
        editPref(context) { it[KEY_REMINDER_ENABLED] = value }
    }
    
    fun reminderHourFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_REMINDER_HOUR, 8)
    
    fun reminderMinuteFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_REMINDER_MINUTE, 0)
    
    suspend fun getReminderEnabled(context: Context): Boolean =
        readPref(context, KEY_REMINDER_ENABLED, false)
    
    suspend fun getReminderHour(context: Context): Int =
        readPref(context, KEY_REMINDER_HOUR, 8)
    
    suspend fun getReminderMinute(context: Context): Int =
        readPref(context, KEY_REMINDER_MINUTE, 0)
    
    suspend fun setReminderTime(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        editPref(context) {
            it[KEY_REMINDER_ENABLED] = enabled
            it[KEY_REMINDER_HOUR] = hour
            it[KEY_REMINDER_MINUTE] = minute
        }
    }
    
    // ---- Private Helpers ----
    
    private suspend fun ensureReadTodayDate(context: Context) {
        val today = todayIsoDate()
        val stored = readPref(context, KEY_READ_TODAY_DATE, "")
        if (stored == today) return
        
        editPref(context) { prefs ->
            prefs[KEY_READ_TODAY_DATE] = today
            prefs[KEY_READ_TODAY_COUNT] = 0
        }
    }
    
    private fun todayIsoDate(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return dateFormat.format(java.util.Date())
    }
}
