package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow

object PrayerPrefs : BasePrefs() {
    private val KEY_PRAYER_SELECTED_TAB = intPreferencesKey("prayer_selected_tab")
    private val KEY_SAVED_LOCAL_PRAYERS = stringSetPreferencesKey("saved_local_prayers")

    @Volatile
    private var cachedPrayerSelectedTab: Int? = null

    fun peekPrayerSelectedTab(defaultValue: Int = 0): Int =
        cachedPrayerSelectedTab?.coerceIn(0, 3) ?: defaultValue.coerceIn(0, 3)

    suspend fun getPrayerSelectedTab(context: Context, defaultValue: Int = 0): Int {
        return cachedPrayerSelectedTab
            ?: readPref(context, KEY_PRAYER_SELECTED_TAB, defaultValue)
                .coerceIn(0, 3)
                .also { cachedPrayerSelectedTab = it }
    }

    suspend fun setPrayerSelectedTab(context: Context, tabIndex: Int) {
        val safe = tabIndex.coerceIn(0, 3)
        editPref(context) { it[KEY_PRAYER_SELECTED_TAB] = safe }
        cachedPrayerSelectedTab = safe
    }

    fun savedLocalPrayersFlow(context: Context): Flow<Set<String>> =
        prefsFlow(context, KEY_SAVED_LOCAL_PRAYERS, emptySet())

    suspend fun toggleSavedLocalPrayer(context: Context, prayerId: String) {
        val id = prayerId.trim()
        if (id.isBlank()) return
        editPref(context) { prefs ->
            val cur = prefs[KEY_SAVED_LOCAL_PRAYERS] ?: emptySet()
            prefs[KEY_SAVED_LOCAL_PRAYERS] = if (cur.contains(id)) (cur - id) else (cur + id)
        }
    }
}