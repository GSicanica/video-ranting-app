package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow

object PsalmPrefs : BasePrefs() {
    private val KEY_SAVED_PSALMS = stringSetPreferencesKey("saved_psalms")
    private val KEY_HIGHLIGHTS = stringSetPreferencesKey("psalm_highlights")

    fun savedPsalmsFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_SAVED_PSALMS, emptySet())

    suspend fun setSavedPsalms(context: Context, values: Set<String>) {
        editPref(context) { it[KEY_SAVED_PSALMS] = values }
    }

    fun highlightedPsalmLinesFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_HIGHLIGHTS, emptySet())

    suspend fun setHighlightedPsalmLines(context: Context, set: Set<String>) {
        editPref(context) { it[KEY_HIGHLIGHTS] = set }
    }
}