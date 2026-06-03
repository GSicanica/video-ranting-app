package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey

object BrightnessPrefs : BasePrefs() {
    private val KEY_APP_BRIGHTNESS = floatPreferencesKey("app_brightness")

    suspend fun getAppBrightness(context: Context, defaultValue: Float = 1.0f): Float =
        readPref(context, KEY_APP_BRIGHTNESS, defaultValue)

    suspend fun setAppBrightness(context: Context, value: Float) {
        editPref(context) { it[KEY_APP_BRIGHTNESS] = value }
    }
}