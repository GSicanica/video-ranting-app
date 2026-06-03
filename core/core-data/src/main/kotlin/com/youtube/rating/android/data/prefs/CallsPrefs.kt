package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object CallsPrefs : BasePrefs() {
    private val KEY_CALLS_GENDER = stringPreferencesKey("calls_gender")
    private val KEY_FAVORITE_PSALM = stringPreferencesKey("calls_favorite_psalm")
    private val KEY_DISPLAY_NAME = stringPreferencesKey("calls_display_name")
    private val KEY_AGE_YEARS = stringPreferencesKey("calls_age_years")

    fun genderFlow(context: Context): Flow<String?> =
        dataStoreFlow(context = context).map { it[KEY_CALLS_GENDER] }

    suspend fun setGender(context: Context, value: String?) {
        val normalized = value?.trim()?.lowercase()
        editPref(context) { prefs ->
            if (normalized.isNullOrBlank()) prefs.remove(KEY_CALLS_GENDER)
            else prefs[KEY_CALLS_GENDER] = normalized
        }
    }

    fun favoritePsalmFlow(context: Context): Flow<String?> =
        dataStoreFlow(context = context).map { it[KEY_FAVORITE_PSALM] }

    suspend fun setFavoritePsalm(context: Context, value: String?) {
        val trimmed = value?.trim()
        editPref(context) { prefs ->
            if (trimmed.isNullOrBlank()) prefs.remove(KEY_FAVORITE_PSALM)
            else prefs[KEY_FAVORITE_PSALM] = trimmed
        }
    }

    fun displayNameFlow(context: Context): Flow<String?> =
        dataStoreFlow(context = context).map { it[KEY_DISPLAY_NAME] }

    suspend fun setDisplayName(context: Context, value: String?) {
        val trimmed = value?.trim()
        editPref(context) { prefs ->
            if (trimmed.isNullOrBlank()) prefs.remove(KEY_DISPLAY_NAME)
            else prefs[KEY_DISPLAY_NAME] = trimmed
        }
    }

    fun ageYearsFlow(context: Context): Flow<String?> =
        dataStoreFlow(context = context).map { it[KEY_AGE_YEARS] }

    suspend fun setAgeYears(context: Context, value: String?) {
        val trimmed = value?.trim()
        editPref(context) { prefs ->
            if (trimmed.isNullOrBlank()) prefs.remove(KEY_AGE_YEARS)
            else prefs[KEY_AGE_YEARS] = trimmed
        }
    }
}
