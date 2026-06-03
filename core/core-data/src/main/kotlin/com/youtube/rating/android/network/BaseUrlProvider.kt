package com.youtube.rating.android.network

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.util.Log
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.youtube.rating.android.data.prefs.FeatureFlagsPrefs
import com.youtube.rating.android.data.prefs.GenericPrefs
import kotlinx.coroutines.flow.first

/**
 * Dynamic Base URL provider.
 *
 * - Stores override locally (SharedPreferences + best-effort DataStore mirror)
 * - Fetches latest base URL from server endpoint
 */
object BaseUrlProvider {
    private const val TAG = "BaseUrlProvider"
    private const val PREFS_NAME = "base_url_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_DATA_BASE_URL = "data_base_url"
    // Prefer full settings endpoint (feature flags + base urls). Keep parsing backwards-compatible.
    private const val CONFIG_PATH = "/api/config/app-settings.php"

    @Volatile
    private var baseUrl: String = normalize(url = BuildConfig.BASE_URL)
    @Volatile
    private var dataBaseUrl: String = normalize(url = BuildConfig.BASE_URL)
    @Volatile
    private var callsTabEnabled: Boolean = true

    private val callsTabEnabledState = kotlinx.coroutines.flow.MutableStateFlow(callsTabEnabled)

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    @JvmStatic
    fun getBaseUrl(): String = baseUrl

    @JvmStatic
    fun getDataBaseUrl(): String = dataBaseUrl

    @JvmStatic
    fun getCallsTabEnabled(): Boolean = callsTabEnabled

    fun callsTabEnabledFlow(): kotlinx.coroutines.flow.StateFlow<Boolean> = callsTabEnabledState

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_BASE_URL, null)
        val storedData = prefs.getString(KEY_DATA_BASE_URL, null)
        if (!stored.isNullOrBlank()) {
            baseUrl = normalize(url = stored)
        } else {
            baseUrl = normalize(url = BuildConfig.BASE_URL)
        }
        if (!storedData.isNullOrBlank()) {
            dataBaseUrl = normalize(url = storedData)
        } else {
            dataBaseUrl = baseUrl
        }
        callsTabEnabled = true
        callsTabEnabledState.value = true
        scope.launch {
            runCatching {
                val enabled = FeatureFlagsPrefs.callsTabEnabledFlow(context).first()
                this@BaseUrlProvider.callsTabEnabled = enabled
                callsTabEnabledState.value = enabled
            }
        }
    }

    fun setBaseUrl(context: Context, url: String, dataUrl: String? = null): Boolean {
        val normalized = normalize(url = url)
        val normalizedData = dataUrl?.let { normalize(url = it) } ?: dataBaseUrl
        if (normalized == baseUrl && normalizedData == dataBaseUrl) return false
        baseUrl = normalized
        dataBaseUrl = normalizedData

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalized)
            .putString(KEY_DATA_BASE_URL, normalizedData)
            .apply()

        // Best-effort mirror to DataStore (non-blocking)
        scope.launch {
            runCatching { GenericPrefs.setString(context, KEY_BASE_URL, normalized) }
            runCatching { GenericPrefs.setString(context, KEY_DATA_BASE_URL, normalizedData) }
        }
        return true
    }

    suspend fun refreshFromServer(context: Context): Boolean {
        val configBase = normalize(url = baseUrl)
        val fallbackBase = normalize(url = BuildConfig.BASE_URL)
        val primaryUrl = configBase + CONFIG_PATH
        val fallbackUrl = fallbackBase + CONFIG_PATH

        fun buildRequest(targetUrl: String) = Request.Builder()
            .url(targetUrl)
            .get()
            .build()

        fun fetchConfig(targetUrl: String): Boolean = runCatching {
            httpClient.newCall(buildRequest(targetUrl = targetUrl)).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Config fetch failed: ${resp.code}")
                    return@runCatching false
                }
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@runCatching false

                val json = JSONObject(body)
                val success = json.optBoolean("success", false)
                val newBase = json.optString("baseUrl", "").trim()
                val newDataBase = json.optString("dataBaseUrl", "").trim()
                val featureFlagsObj = json.optJSONObject("featureFlags")
                val popularTimeVideosEnabled = readBooleanFlexibleTree(json = featureFlagsObj ?: json, keys = listOf("popularTimeVideosEnabled", "popular_time_videos_enabled"), default = true)
                val callsTabEnabled = readBooleanFlexibleTree(json = featureFlagsObj ?: json, keys = listOf("callsTabEnabled", "calls_tab_enabled", "callsTab", "calls_tab"), default = true)
                val webViewJsAutomationEnabled = readBooleanFlexibleTree(
                    json = featureFlagsObj ?: json,
                    keys = listOf("webViewJsAutomationEnabled", "webview_js_automation_enabled", "webviewJsAutomationEnabled"),
                    default = true
                )
                persistFeatureFlags(
                    context = context,
                    popularTimeVideosEnabled = popularTimeVideosEnabled,
                    callsTabEnabled = callsTabEnabled,
                    webViewJsAutomationEnabled = webViewJsAutomationEnabled
                )
                if (!success || newBase.isBlank()) {
                    return@runCatching false
                }

                setBaseUrl(context = context, url = newBase, dataUrl = newDataBase.ifBlank { null })
                true
            }
        }.getOrElse {
            Log.w(TAG, "Config fetch error: ${it.message}")
            false
        }

        val primaryResult = fetchConfig(targetUrl = primaryUrl)
        if (primaryResult) return true
        if (fallbackBase == configBase) return false
        return fetchConfig(targetUrl = fallbackUrl)
    }

    private fun persistFeatureFlags(
        context: Context,
        popularTimeVideosEnabled: Boolean,
        callsTabEnabled: Boolean,
        webViewJsAutomationEnabled: Boolean
    ) {
        this@BaseUrlProvider.callsTabEnabled = callsTabEnabled
        callsTabEnabledState.value = callsTabEnabled
        scope.launch {
            runCatching { FeatureFlagsPrefs.setPopularTimeVideosEnabled(context, popularTimeVideosEnabled) }
            runCatching { FeatureFlagsPrefs.setCallsTabEnabled(context, callsTabEnabled) }
            runCatching { FeatureFlagsPrefs.setWebViewJsAutomationEnabled(context, webViewJsAutomationEnabled) }
        }
    }

    private fun normalize(url: String): String = url.trimEnd('/')

    private fun readBooleanFlexible(json: JSONObject, keys: List<String>, default: Boolean): Boolean {
        for (key in keys) {
            if (!json.has(key)) continue
            val v = json.opt(key)
            when (v) {
                is Boolean -> return v
                is Number -> return v.toInt() != 0
                is String -> {
                    val s = v.trim().lowercase()
                    if (s == "true" || s == "1" || s == "yes") return true
                    if (s == "false" || s == "0" || s == "no") return false
                }
            }
        }
        return default
    }

    private fun readBooleanFlexibleTree(json: JSONObject, keys: List<String>, default: Boolean): Boolean {
        val root = readBooleanFlexible(json = json, keys = keys, default = default)
        if (root != default) return root
        val containers = listOf("data", "config", "flags", "featureFlags", "features")
        for (c in containers) {
            val obj = json.optJSONObject(c) ?: continue
            val v = readBooleanFlexible(json = obj, keys = keys, default = default)
            if (v != default) return v
        }
        return default
    }
}
