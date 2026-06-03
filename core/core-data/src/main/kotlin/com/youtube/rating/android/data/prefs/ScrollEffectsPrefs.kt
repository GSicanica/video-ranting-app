package com.youtube.rating.android.data.prefs

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.youtube.rating.android.data.PrefsDataStore

/**
 * Scroll effects preferences module
 * 
 * Manages user preferences for scroll animations and visual effects.
 * By default, scroll effects are DISABLED for better performance on older devices.
 */
object ScrollEffectsPrefs {
    
    private val KEY_DISABLE_SCROLL_EFFECTS = booleanPreferencesKey("disable_scroll_effects")
    
    /**
     * Flow that emits whether scroll effects are disabled
     * @param context Android context
     * @return Flow<Boolean> - true if scroll effects are disabled (default), false if enabled
     */
    fun disableFlow(context: Context): Flow<Boolean> =
        PrefsDataStore.dataStore(context).data.map { it[KEY_DISABLE_SCROLL_EFFECTS] ?: true }
    
    /**
     * Get current scroll effects disabled state (synchronous)
     * @param context Android context
     * @return Boolean - true if scroll effects are disabled, false if enabled
     */
    suspend fun isDisabled(context: Context): Boolean =
        withContext(ioDispatcher) {
            PrefsDataStore.dataStore(context).data.map { it[KEY_DISABLE_SCROLL_EFFECTS] ?: true }.first()
        }
    
    /**
     * Set whether scroll effects should be disabled
     * @param context Android context
     * @param disabled Boolean - true to disable scroll effects, false to enable them
     */
    suspend fun setDisabled(context: Context, disabled: Boolean) {
        withContext(ioDispatcher) {
            PrefsDataStore.dataStore(context).edit { it[KEY_DISABLE_SCROLL_EFFECTS] = disabled }
        }
    }
    
    /**
     * Toggle the scroll effects setting
     * @param context Android context
     */
    suspend fun toggle(context: Context) {
        val current = isDisabled(context = context)
        setDisabled(context = context, disabled = !current)
    }
    
}