package com.youtube.rating.android.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.launch
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * Manages admin mode state across the app (Koin Singleton)
 * Admin mode is activated by clicking "O aplikaciji" version 10 times in SettingsScreen
 */
class AdminManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = AppScope.get()
    @Volatile
    private var cachedAdmin: Boolean = false
    @Volatile
    private var initialized: Boolean = false
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "admin_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            // Migrate from old SharedPreferences if present (deferred to first use)
            val legacyPrefs = appContext.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)
            if (legacyPrefs.contains("is_admin_mode")) {
                val legacyValue = legacyPrefs.getBoolean("is_admin_mode", false)
                cachedAdmin = legacyValue
                scope.launch { AdminPrefs.setAdminMode(appContext, legacyValue) }
                legacyPrefs.edit().clear().apply()
            }
            // Never block any thread (including binder/background threads). Admin mode is read often
            // from Compose on main; keep it cached and refresh async.
            scope.makeIOCall { cachedAdmin = AdminPrefs.getAdminMode(appContext) }
        }
    }

    fun isAdminMode(): Boolean {
        ensureInitialized()
        return cachedAdmin
    }
    
    fun setAdminMode(enabled: Boolean) {
        ensureInitialized()
        cachedAdmin = enabled
        scope.launch { AdminPrefs.setAdminMode(appContext, enabled) }
        Logger.info("AdminManager", "Admin mode ${if (enabled) "ENABLED" else "DISABLED"}")
    }
    
    fun toggleAdminMode(): Boolean {
        ensureInitialized()
        val newState = !isAdminMode()
        setAdminMode(enabled = newState)
        return newState
    }

    /**
     * Securely store admin token using EncryptedSharedPreferences
     */
    fun setAdminToken(token: String) {
        ensureInitialized()
        encryptedPrefs.edit().putString("admin_token", token).apply()
        Logger.info("AdminManager", "Admin token securely stored")
    }

    /**
     * Retrieve admin token from secure storage
     * Returns null if no token is stored
     */
    fun getAdminToken(): String? {
        ensureInitialized()
        return encryptedPrefs.getString("admin_token", null)
    }

    /**
     * Clear stored admin token
     */
    fun clearAdminToken() {
        ensureInitialized()
        encryptedPrefs.edit().remove("admin_token").apply()
        Logger.info("AdminManager", "Admin token cleared")
    }

    /**
     * Check if admin token is available
     */
    fun hasAdminToken(): Boolean {
        ensureInitialized()
        return getAdminToken() != null
    }
}