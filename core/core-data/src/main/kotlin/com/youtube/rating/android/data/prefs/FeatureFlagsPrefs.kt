package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow

object FeatureFlagsPrefs : BasePrefs() {
    private val KEY_POPULAR_TIME_VIDEOS_ENABLED = booleanPreferencesKey("popular_time_videos_enabled")
    private val KEY_CALLS_TAB_ENABLED = booleanPreferencesKey("calls_tab_enabled")
    private val KEY_WEBVIEW_JS_AUTOMATION_ENABLED = booleanPreferencesKey("webview_js_automation_enabled")

    fun popularTimeVideosEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_POPULAR_TIME_VIDEOS_ENABLED, true)

    suspend fun setPopularTimeVideosEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_POPULAR_TIME_VIDEOS_ENABLED] = enabled }
    }

    fun callsTabEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_CALLS_TAB_ENABLED, true)

    suspend fun setCallsTabEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_CALLS_TAB_ENABLED] = enabled }
    }

    /**
     * Enables/disables JS automation in WebViews (e.g. consent/autoplay helpers).
     * Useful as a kill-switch when upstream DOM changes break automation.
     */
    fun webViewJsAutomationEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_WEBVIEW_JS_AUTOMATION_ENABLED, true)

    suspend fun setWebViewJsAutomationEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_WEBVIEW_JS_AUTOMATION_ENABLED] = enabled }
    }
}
