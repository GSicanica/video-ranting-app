package com.youtube.rating.android

import android.content.Context
import com.youtube.rating.android.data.prefs.BrightnessPrefs
import com.youtube.rating.core.coroutines.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppBrightnessManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    // Persisted user preference (stored in DataStore).
    private val _userBrightness = MutableStateFlow(1.0f)

    // Temporary override used while app is backgrounded (do NOT persist).
    private val _overrideBrightness = MutableStateFlow<Float?>(null)

    // The effective value used by the UI overlay.
    private val _brightness = MutableStateFlow(1.0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private var dataStoreLoaded = false

    init {
        scope.launch {
            ensureDataStoreLoaded()
        }

        scope.launch {
            _userBrightness
                .combine(_overrideBrightness) { user, override -> override ?: user }
                .collect { effective ->
                    _brightness.value = effective.coerceIn(0.0f, 1.0f)
                }
        }
    }

    private suspend fun ensureDataStoreLoaded() {
        if (!dataStoreLoaded) {
            val legacy = withContext(ioDispatcher) {
                readLegacyBrightness().coerceIn(0.0f, 1.0f)
            }
            val stored = BrightnessPrefs.getAppBrightness(appContext, legacy).coerceIn(0.0f, 1.0f)
            if (stored == legacy && legacy != 1.0f) {
                runCatching { BrightnessPrefs.setAppBrightness(appContext, legacy) }
            }
            _userBrightness.value = stored
            dataStoreLoaded = true
        }
    }

    fun setBrightness(value: Float) {
        val clamped = value.coerceIn(0.0f, 1.0f)
        _userBrightness.value = clamped
        scope.launch {
            ensureDataStoreLoaded()
            runCatching { BrightnessPrefs.setAppBrightness(appContext, clamped) }
        }
    }

    suspend fun getCurrentBrightness(): Float {
        ensureDataStoreLoaded()
        return _userBrightness.value
    }

    fun setInBackground(inBackground: Boolean) {
        _overrideBrightness.value = if (inBackground) 1.0f else null
    }

    private fun readLegacyBrightness(): Float {
        return appContext.getSharedPreferences("brightness_prefs", Context.MODE_PRIVATE)
            .getFloat("app_brightness", 1.0f)
    }
}
