package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object BibleStatsPrefs : BasePrefs() {
    private val BIBLE_DAILY_GOAL_UNITS = intPreferencesKey("bible_daily_goal_units")
    private val BIBLE_TOTAL_READ = intPreferencesKey("bible_total_read")
    private val BIBLE_TOTAL_MEDITATION = intPreferencesKey("bible_total_meditation")
    private val BIBLE_BEST_STREAK = intPreferencesKey("bible_best_streak")
    private val BIBLE_DAYS_ACTIVE = intPreferencesKey("bible_days_active")
    private val KEY_BIBLE_ACTIVE_DAYS = stringSetPreferencesKey("bible_active_days")

    fun bibleDailyGoalUnitsFlow(context: Context): Flow<Int> =
        prefsFlow(context, BIBLE_DAILY_GOAL_UNITS, 3).map { it.coerceIn(1, 20) }

    suspend fun setBibleDailyGoalUnits(context: Context, units: Int) {
        editPref(context) { prefs -> prefs[BIBLE_DAILY_GOAL_UNITS] = units.coerceIn(1, 20) }
    }

    fun bibleTotalReadFlow(context: Context): Flow<Int> =
        prefsFlow(context, BIBLE_TOTAL_READ, 0)

    fun bibleTotalMeditationFlow(context: Context): Flow<Int> =
        prefsFlow(context, BIBLE_TOTAL_MEDITATION, 0)

    fun bibleBestStreakFlow(context: Context): Flow<Int> =
        prefsFlow(context, BIBLE_BEST_STREAK, 0)

    fun bibleDaysActiveFlow(context: Context): Flow<Int> =
        prefsFlow(context, BIBLE_DAYS_ACTIVE, 0)

    suspend fun addBibleReadTotal(context: Context, value: Int) {
        editPref(context) {
            val cur = it[BIBLE_TOTAL_READ] ?: 0
            it[BIBLE_TOTAL_READ] = (cur + value).coerceAtLeast(0)
        }
    }

    suspend fun addBibleMeditationTotal(context: Context, value: Int) {
        editPref(context) {
            val cur = it[BIBLE_TOTAL_MEDITATION] ?: 0
            it[BIBLE_TOTAL_MEDITATION] = (cur + value).coerceAtLeast(0)
        }
    }

    suspend fun updateBestStreak(context: Context, streak: Int) {
        editPref(context) {
            val best = it[BIBLE_BEST_STREAK] ?: 0
            if (streak > best) it[BIBLE_BEST_STREAK] = streak
        }
    }

    suspend fun addActiveDay(context: Context) {
        editPref(context) {
            val cur = it[BIBLE_DAYS_ACTIVE] ?: 0
            it[BIBLE_DAYS_ACTIVE] = cur + 1
        }
    }

    suspend fun removeActiveDay(context: Context, day: String) {
        editPref(context) { prefs ->
            val current = prefs[KEY_BIBLE_ACTIVE_DAYS] ?: emptySet()
            prefs[KEY_BIBLE_ACTIVE_DAYS] = current - day
        }
    }
}