package com.youtube.rating.android.data.prefs

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.youtube.rating.android.data.PrefsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TrainingPrefs : BasePrefs() {
    private val KEY_TRAINING_PRAYER_GOAL = intPreferencesKey("training_prayer_goal")
    private val KEY_TRAINING_PRAYER_DONE_TODAY = intPreferencesKey("training_prayer_done_today")
    private val KEY_TRAINING_PRAYER_DATE = stringPreferencesKey("training_prayer_date")

    private val KEY_TRAINING_PSALMS_DONE_TODAY = stringSetPreferencesKey("training_psalms_done_today")
    private val KEY_TRAINING_PSALMS_DATE = stringPreferencesKey("training_psalms_date")
    private val KEY_TRAINING_PSALMS_TODAY = stringSetPreferencesKey("training_psalms_today")

    private val KEY_TRAINING_ROSARY_DONE_TODAY = intPreferencesKey("training_rosary_done_today")
    private val KEY_TRAINING_ROSARY_DATE = stringPreferencesKey("training_rosary_date")

    private val KEY_TRAINING_ENCOURAGEMENT_DONE_TODAY = intPreferencesKey("training_encouragement_done_today")
    private val KEY_TRAINING_ENCOURAGEMENT_DATE = stringPreferencesKey("training_encouragement_date")

    private val KEY_TRAINING_STATS_DAILY = stringPreferencesKey("training_stats_daily")
    private val KEY_CUSTOM_NOVENAS = stringSetPreferencesKey("custom_novenas")

    private val KEY_TRAINING_DRAWER_PINNED = stringSetPreferencesKey("training_drawer_pinned")

    private val DAY_KEY = stringPreferencesKey("day_key")
    private val BIBLE_MEDITATION_MIN_TODAY = intPreferencesKey("bible_meditation_min_today")

    // Shared with Bible planner stats
    private val KEY_BIBLE_PLANNER_READ_TODAY_COUNT = intPreferencesKey("bible_planner_read_today_count")

    fun trainingPrayerGoalFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_TRAINING_PRAYER_GOAL, 3)

    fun trainingPrayerDoneTodayFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_TRAINING_PRAYER_DONE_TODAY, 0)

    suspend fun setTrainingPrayerGoal(context: Context, value: Int) {
        editPref(context) { it[KEY_TRAINING_PRAYER_GOAL] = value.coerceAtLeast(0) }
    }

    suspend fun incrementTrainingPrayerDoneToday(context: Context, delta: Int) {
        if (delta == 0) return
        ensureTrainingPrayerToday(context = context)
        editPref(context) { prefs ->
            val current = prefs[KEY_TRAINING_PRAYER_DONE_TODAY] ?: 0
            prefs[KEY_TRAINING_PRAYER_DONE_TODAY] = (current + delta).coerceAtLeast(0)
        }
        updateTrainingStatsToday(context = context)
    }

    suspend fun resetTrainingPrayerDoneToday(context: Context) {
        ensureTrainingPrayerToday(context = context)
        editPref(context) { prefs -> prefs[KEY_TRAINING_PRAYER_DONE_TODAY] = 0 }
        updateTrainingStatsToday(context = context)
    }

    fun trainingRosaryDoneTodayFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_TRAINING_ROSARY_DONE_TODAY, 0)

    suspend fun incrementTrainingRosaryDoneToday(context: Context, delta: Int) {
        if (delta == 0) return
        ensureTrainingRosaryToday(context = context)
        editPref(context) { prefs ->
            val current = prefs[KEY_TRAINING_ROSARY_DONE_TODAY] ?: 0
            prefs[KEY_TRAINING_ROSARY_DONE_TODAY] = (current + delta).coerceAtLeast(0)
        }
        updateTrainingStatsToday(context = context)
    }

    suspend fun resetTrainingRosaryDoneToday(context: Context) {
        ensureTrainingRosaryToday(context = context)
        editPref(context) { prefs -> prefs[KEY_TRAINING_ROSARY_DONE_TODAY] = 0 }
        updateTrainingStatsToday(context = context)
    }

    fun trainingEncouragementDoneTodayFlow(context: Context): Flow<Int> =
        prefsFlow(context, KEY_TRAINING_ENCOURAGEMENT_DONE_TODAY, 0)

    suspend fun incrementTrainingEncouragementDoneToday(context: Context, delta: Int) {
        if (delta == 0) return
        ensureTrainingEncouragementToday(context = context)
        editPref(context) { prefs ->
            val current = prefs[KEY_TRAINING_ENCOURAGEMENT_DONE_TODAY] ?: 0
            prefs[KEY_TRAINING_ENCOURAGEMENT_DONE_TODAY] = (current + delta).coerceAtLeast(0)
        }
        updateTrainingStatsToday(context = context)
    }

    suspend fun resetTrainingEncouragementDoneToday(context: Context) {
        ensureTrainingEncouragementToday(context = context)
        editPref(context) { prefs -> prefs[KEY_TRAINING_ENCOURAGEMENT_DONE_TODAY] = 0 }
        updateTrainingStatsToday(context = context)
    }

    fun trainingPsalmsDoneTodayFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_TRAINING_PSALMS_DONE_TODAY, emptySet())

    fun trainingStatsJsonFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_TRAINING_STATS_DAILY, "{}")

    fun customNovenasFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_CUSTOM_NOVENAS, emptySet())

    suspend fun addCustomNovena(context: Context, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        editPref(context) { prefs ->
            val cur = prefs[KEY_CUSTOM_NOVENAS] ?: emptySet()
            prefs[KEY_CUSTOM_NOVENAS] = cur + trimmed
        }
    }

    suspend fun removeCustomNovena(context: Context, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        editPref(context) { prefs ->
            val cur = prefs[KEY_CUSTOM_NOVENAS] ?: emptySet()
            prefs[KEY_CUSTOM_NOVENAS] = cur - trimmed
        }
    }

    suspend fun getTrainingPsalmsToday(context: Context): Set<String> {
        ensureTrainingPsalmsToday(context = context)
        return readPref(context, KEY_TRAINING_PSALMS_TODAY, emptySet())
    }

    suspend fun setTrainingPsalmsToday(context: Context, values: Set<String>) {
        ensureTrainingPsalmsToday(context = context)
        editPref(context) { prefs ->
            prefs[KEY_TRAINING_PSALMS_TODAY] = values
        }
    }

    suspend fun toggleTrainingPsalmDoneToday(context: Context, psalm: Int) {
        ensureTrainingPsalmsToday(context = context)
        val token = psalm.toString()
        editPref(context) { prefs ->
            val cur = prefs[KEY_TRAINING_PSALMS_DONE_TODAY] ?: emptySet()
            prefs[KEY_TRAINING_PSALMS_DONE_TODAY] = if (cur.contains(token)) (cur - token) else (cur + token)
        }
        updateTrainingStatsToday(context = context)
    }

    suspend fun clearTrainingPsalmsDoneToday(context: Context) {
        ensureTrainingPsalmsToday(context = context)
        editPref(context) { prefs -> prefs[KEY_TRAINING_PSALMS_DONE_TODAY] = emptySet() }
        updateTrainingStatsToday(context = context)
    }

    fun trainingBibleMeditationMinTodayFlow(context: Context): Flow<Int> =
        dataStoreFlow(context = context).map { prefs -> (prefs[BIBLE_MEDITATION_MIN_TODAY] ?: 0).coerceAtLeast(0) }

    fun trainingDrawerPinnedFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_TRAINING_DRAWER_PINNED, defaultTrainingDrawerPinned())

    suspend fun toggleTrainingDrawerPinned(context: Context, id: String) {
        editPref(context) { prefs ->
            val current = prefs[KEY_TRAINING_DRAWER_PINNED] ?: defaultTrainingDrawerPinned()
            prefs[KEY_TRAINING_DRAWER_PINNED] = if (id in current) (current - id) else (current + id)
        }
    }

    suspend fun setTrainingDrawerPinned(context: Context, ids: Set<String>) {
        editPref(context) { prefs -> prefs[KEY_TRAINING_DRAWER_PINNED] = ids }
    }

    suspend fun incrementTrainingBibleMeditationMinToday(context: Context, delta: Int) {
        editPref(context) { prefs ->
            val cur = (prefs[BIBLE_MEDITATION_MIN_TODAY] ?: 0).coerceAtLeast(0)
            prefs[BIBLE_MEDITATION_MIN_TODAY] = (cur + delta).coerceAtLeast(0)
        }
    }

    suspend fun resetTrainingBibleMeditationMinToday(context: Context) {
        editPref(context) { prefs -> prefs[BIBLE_MEDITATION_MIN_TODAY] = 0 }
    }

    suspend fun ensureDailyCounters(context: Context) {
        val today = todayIsoDate()
        editPref(context) { prefs ->
            val lastDay = prefs[DAY_KEY]
            if (lastDay == null || lastDay != today) {
                prefs[DAY_KEY] = today
                prefs[BIBLE_MEDITATION_MIN_TODAY] = 0
            }
        }
    }

    private suspend fun ensureTrainingPrayerToday(context: Context) {
        val today = todayIsoDate()
        val stored = readPref(context, KEY_TRAINING_PRAYER_DATE, "")
        if (stored == today) return
        editPref(context) { prefs ->
            prefs[KEY_TRAINING_PRAYER_DATE] = today
            prefs[KEY_TRAINING_PRAYER_DONE_TODAY] = 0
        }
    }

    private suspend fun ensureTrainingPsalmsToday(context: Context) {
        val today = todayIsoDate()
        val stored = readPref(context, KEY_TRAINING_PSALMS_DATE, "")
        if (stored == today) return
        editPref(context) { prefs ->
            prefs[KEY_TRAINING_PSALMS_DATE] = today
            prefs[KEY_TRAINING_PSALMS_DONE_TODAY] = emptySet()
            prefs[KEY_TRAINING_PSALMS_TODAY] = emptySet()
        }
    }

    private suspend fun ensureTrainingRosaryToday(context: Context) {
        val today = todayIsoDate()
        val stored = readPref(context, KEY_TRAINING_ROSARY_DATE, "")
        if (stored == today) return
        editPref(context) { prefs ->
            prefs[KEY_TRAINING_ROSARY_DATE] = today
            prefs[KEY_TRAINING_ROSARY_DONE_TODAY] = 0
        }
    }

    private suspend fun ensureTrainingEncouragementToday(context: Context) {
        val today = todayIsoDate()
        val stored = readPref(context, KEY_TRAINING_ENCOURAGEMENT_DATE, "")
        if (stored == today) return
        editPref(context) { prefs ->
            prefs[KEY_TRAINING_ENCOURAGEMENT_DATE] = today
            prefs[KEY_TRAINING_ENCOURAGEMENT_DONE_TODAY] = 0
        }
    }

    internal suspend fun updateTrainingStatsToday(context: Context) {
        val today = todayIsoDate()
        val prefs = withContext(ioDispatcher) { PrefsDataStore.dataStore(context).data.first() }
        val psalmsDone = (prefs[KEY_TRAINING_PSALMS_DONE_TODAY] ?: emptySet()).size
        val bibleArticles = prefs[KEY_BIBLE_PLANNER_READ_TODAY_COUNT] ?: 0
        val prayerDone = prefs[KEY_TRAINING_PRAYER_DONE_TODAY] ?: 0
        val rosaryDone = prefs[KEY_TRAINING_ROSARY_DONE_TODAY] ?: 0
        val encouragementDone = prefs[KEY_TRAINING_ENCOURAGEMENT_DONE_TODAY] ?: 0
        val points = (bibleArticles * 3) +
            (if (prayerDone >= 1) 4 else 0) +
            (if (rosaryDone >= 1) 5 else 0) +
            (if (encouragementDone >= 1) 2 else 0)

        val json = prefs[KEY_TRAINING_STATS_DAILY] ?: "{}"
        val obj = try {
            JSONObject(json)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            JSONObject()
        }
        val dayObj = JSONObject().apply {
            put("psalms", psalmsDone)
            put("bibleArticles", bibleArticles)
            put("prayer", prayerDone)
            put("rosary", rosaryDone)
            put("encouragement", encouragementDone)
            put("points", points)
        }
        obj.put(today, dayObj)
        editPref(context) { it[KEY_TRAINING_STATS_DAILY] = obj.toString() }
    }

    private fun todayIsoDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun defaultTrainingDrawerPinned(): Set<String> = setOf(
        com.youtube.rating.android.ui.screens.TrainingPinnedItems.READING,
        com.youtube.rating.android.ui.screens.TrainingPinnedItems.MEDITATION,
        com.youtube.rating.android.ui.screens.TrainingPinnedItems.PSALMS,
        com.youtube.rating.android.ui.screens.TrainingPinnedItems.SEQUENTIAL,
        com.youtube.rating.android.ui.screens.TrainingPinnedItems.SEARCH,
    )
}
