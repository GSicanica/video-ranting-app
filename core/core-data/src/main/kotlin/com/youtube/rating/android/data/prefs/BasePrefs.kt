package com.youtube.rating.android.data.prefs

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import com.youtube.rating.android.data.PrefsDataStore
import com.youtube.rating.android.sentry.SentryLogger
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Base class for all preference modules.
 * 
 * Provides common utilities for reading, writing, and observing preferences
 * from Android DataStore.
 * 
 * All preference modules should extend this class to ensure consistency
 * and avoid code duplication.
 * 
 * Example:
 * ```
 * object MyPrefs : BasePrefs() {
 *     private val KEY_MY_VALUE = stringPreferencesKey("my_value")
 *     
 *     fun myValueFlow(context: Context): Flow<String> =
 *         prefsFlow(context, KEY_MY_VALUE, "default")
 *     
 *     suspend fun setMyValue(context: Context, value: String) {
 *         editPref(context) { it[KEY_MY_VALUE] = value }
 *     }
 * }
 * ```
 */
abstract class BasePrefs {
    
    /**
     * Read a preference value synchronously (blocking)
     * 
     * @param context Android context
     * @param key Preferences key
     * @param defaultValue Default value if key not found
     * @return The preference value or default
     */
    protected suspend fun <T> readPref(
        context: Context,
        key: Preferences.Key<T>,
        defaultValue: T
    ): T = withContext(ioDispatcher) {
        PrefsDataStore.dataStore(context).data
            .map { it[key] ?: defaultValue }
            .first()
    }

    /**
     * Read a preference value that may be nullable.
     */
    protected suspend fun <T> readPrefNullable(
        context: Context,
        key: Preferences.Key<T>
    ): T? = withContext(ioDispatcher) {
        PrefsDataStore.dataStore(context).data
            .map { it[key] }
            .first()
    }
    
    /**
     * Edit a preference value
     * 
     * @param context Android context
     * @param block Suspend lambda to modify preferences
     */
    protected suspend fun editPref(
        context: Context,
        block: suspend (MutablePreferences) -> Unit
    ) = withContext(ioDispatcher) {
        runCatching {
            PrefsDataStore.dataStore(context).edit { block(it) }
        }.onFailure { t ->
            SentryLogger.captureException(t)
            Logger.error("BasePrefs", "Failed to edit DataStore preferences", t)
        }
    }
    
    /**
     * Create a Flow for observing a preference
     * 
     * @param context Android context
     * @param key Preferences key
     * @param defaultValue Default value if key not found
     * @return Flow that emits the preference value
     */
    protected fun <T> prefsFlow(
        context: Context,
        key: Preferences.Key<T>,
        defaultValue: T
    ): Flow<T> = PrefsDataStore.dataStore(context).data.map { it[key] ?: defaultValue }

    /**
     * Raw DataStore flow for advanced mapping.
     */
    protected fun dataStoreFlow(context: Context): Flow<Preferences> =
        PrefsDataStore.dataStore(context).data
}
