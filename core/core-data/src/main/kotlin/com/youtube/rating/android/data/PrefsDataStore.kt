package com.youtube.rating.android.data

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import com.youtube.rating.android.sentry.SentryLogger

/**
 * Centralized DataStore initialization and access.
 *
 * Feature-specific preferences used to live in modular pref classes under
 * `com.youtube.rating.android.data.prefs`.
 */
object PrefsDataStore {
    private val dataStoreLock = Any()
    private var dataStore: DataStore<Preferences>? = null

    fun initialize(context: Context) {
        if (dataStore != null) return
        synchronized(dataStoreLock) {
            if (dataStore != null) return
            dataStore = createDataStore(context = context, file = File(context.filesDir, "datastore/app_prefs.preferences_pb"))
        }
    }

    private val Context.appDataStore: DataStore<Preferences>
        get() {
            dataStore?.let { return it }
            return synchronized(dataStoreLock) {
                dataStore?.let { return it }
                val primary = runCatching {
                    createDataStore(context = this, file = File(filesDir, "datastore/app_prefs.preferences_pb"))
                }.getOrElse { e ->
                    SentryLogger.captureException(e)
                    val fallbackFile = File(cacheDir, "datastore/app_prefs_fallback.preferences_pb")
                    runCatching { createDataStore(context = this, file = fallbackFile) }
                        .onFailure { SentryLogger.captureException(it) }
                        .getOrElse {
                            createDataStore(context = this, file = File(cacheDir, "datastore/app_prefs_last_resort.preferences_pb"))
                        }
                }
                dataStore = primary
                primary
            }
        }

    /**
     * Internal accessor for modules that want to reuse the same DataStore instance
     * without duplicating initialization logic.
     */
    internal fun dataStore(context: Context): DataStore<Preferences> = context.appDataStore

    private fun createDataStore(context: Context, file: File): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { file }
        )
    }
}
