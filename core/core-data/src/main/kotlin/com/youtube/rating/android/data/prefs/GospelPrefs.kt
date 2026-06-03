package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow

object GospelPrefs : BasePrefs() {
    private val KEY_GOSPEL_MARKED_DATES = stringSetPreferencesKey("gospel_marked_dates")

    fun gospelMarkedDatesFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_GOSPEL_MARKED_DATES, emptySet())

    suspend fun toggleGospelMarkedDate(context: Context, date: String) {
        if (date.isBlank()) return
        editPref(context) { prefs ->
            val current = prefs[KEY_GOSPEL_MARKED_DATES] ?: emptySet()
            val next = current.toMutableSet()
            if (date in next) next.remove(date) else next.add(date)
            prefs[KEY_GOSPEL_MARKED_DATES] = next
        }
    }
}