package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

object SaintsPrefs : BasePrefs() {
    private val KEY_SAINT_OF_DAY_DATE = stringPreferencesKey("saint_of_day_date")
    private val KEY_SAINT_OF_DAY_JSON = stringPreferencesKey("saint_of_day_json")
    private val KEY_SAINT_OF_DAY_DISMISSED_DATE = stringPreferencesKey("saint_of_day_dismissed_date")
    private val KEY_SAINT_OF_DAY_NOTIFICATIONS_ENABLED =
        booleanPreferencesKey("saint_of_day_notifications_enabled")
    private val KEY_SAINTS_JSON = stringPreferencesKey("saints_json")

    suspend fun getSaintOfDayDate(context: Context): String? =
        readPrefNullable(context, KEY_SAINT_OF_DAY_DATE)

    suspend fun setSaintOfDayDate(context: Context, value: String) {
        editPref(context) { it[KEY_SAINT_OF_DAY_DATE] = value }
    }

    suspend fun getSaintOfDayJson(context: Context): String? =
        readPrefNullable(context, KEY_SAINT_OF_DAY_JSON)

    suspend fun setSaintOfDayJson(context: Context, value: String) {
        editPref(context) { it[KEY_SAINT_OF_DAY_JSON] = value }
    }

    suspend fun getSaintOfDayDismissedDate(context: Context): String? =
        readPrefNullable(context, KEY_SAINT_OF_DAY_DISMISSED_DATE)

    suspend fun setSaintOfDayDismissedDate(context: Context, value: String) {
        editPref(context) { it[KEY_SAINT_OF_DAY_DISMISSED_DATE] = value }
    }

    fun saintOfDayNotificationsEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_SAINT_OF_DAY_NOTIFICATIONS_ENABLED, true)

    suspend fun getSaintOfDayNotificationsEnabled(context: Context): Boolean =
        readPref(context, KEY_SAINT_OF_DAY_NOTIFICATIONS_ENABLED, true)

    suspend fun setSaintOfDayNotificationsEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_SAINT_OF_DAY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun getSaintsJson(context: Context): String =
        readPref(context, KEY_SAINTS_JSON, "[]")

    fun saintsJsonFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_SAINTS_JSON, "[]")

    suspend fun setSaintsJson(context: Context, json: String) {
        editPref(context) { it[KEY_SAINTS_JSON] = json }
    }
}