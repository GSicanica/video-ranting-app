package com.youtube.rating.android.feature.running.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class RunningPrefsDataStore(
    val dataStore: DataStore<Preferences>,
)

object RunningPrefs {
    private val KEY_RUNNING_ENTRIES_JSON = stringPreferencesKey("running_entries_json")
    private val KEY_RUNNING_ACTIVE_SESSION_JSON = stringPreferencesKey("running_active_session_json")

    fun entriesJsonFlow(dataStoreHolder: RunningPrefsDataStore): Flow<String> =
        dataStoreHolder.dataStore.data.map { it[KEY_RUNNING_ENTRIES_JSON].orEmpty() }

    fun activeSessionJsonFlow(dataStoreHolder: RunningPrefsDataStore): Flow<String> =
        dataStoreHolder.dataStore.data.map { it[KEY_RUNNING_ACTIVE_SESSION_JSON].orEmpty() }

    suspend fun setEntriesJson(dataStoreHolder: RunningPrefsDataStore, value: String) {
        dataStoreHolder.dataStore.edit { prefs ->
            prefs[KEY_RUNNING_ENTRIES_JSON] = value
        }
    }

    suspend fun setActiveSessionJson(dataStoreHolder: RunningPrefsDataStore, value: String) {
        dataStoreHolder.dataStore.edit { prefs ->
            prefs[KEY_RUNNING_ACTIVE_SESSION_JSON] = value
        }
    }
}
