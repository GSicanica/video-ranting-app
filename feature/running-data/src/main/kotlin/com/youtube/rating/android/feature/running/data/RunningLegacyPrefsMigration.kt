package com.youtube.rating.android.feature.running.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey

class RunningLegacyPrefsMigration(
    @Suppress("UNUSED_PARAMETER") context: Context,
) : DataMigration<Preferences> {

    private val legacyEntriesKey = stringPreferencesKey("running_entries_json")
    private val legacyActiveSessionKey = stringPreferencesKey("running_active_session_json")
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        // Avoid opening a second DataStore instance for app_prefs.preferences_pb.
        // Returning false keeps migration as a no-op and prevents startup crashes.
        return false
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        return mutablePreferencesOf().apply {
            currentData[legacyEntriesKey]?.let { this[legacyEntriesKey] = it }
            currentData[legacyActiveSessionKey]?.let { this[legacyActiveSessionKey] = it }
        }
    }

    override suspend fun cleanUp() {
        // Keep the legacy store untouched; migration is copy-only.
    }
}
