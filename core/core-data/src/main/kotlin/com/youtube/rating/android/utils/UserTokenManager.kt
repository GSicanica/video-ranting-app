package com.youtube.rating.android.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.sentry.SentryUserContextProvider
import com.youtube.rating.shared.models.AnonymousRegisterResponse
import com.youtube.rating.android.data.prefs.InstallPrefs
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * Manages user token for server-based user identification
 * Stored securely using EncryptedSharedPreferences.
 *
 * Note:
 * - Android Auto Backup does not reliably restore Android Keystore keys across devices.
 * - Treat this as device-local persistence unless you implement server-side account restore.
 */
class UserTokenManager(context: Context, private val apiClient: RatingApiClient) {
    private val appContext = context.applicationContext
    private var cachedUserToken: String? = null

    // Lazy load the encrypted prefs and token
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "user_token_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getCachedToken(): String? {
        if (cachedUserToken == null) {
            cachedUserToken = encryptedPrefs.getString("user_token", null)
        }
        return cachedUserToken
    }

    /**
     * Get user token, registering anonymously if needed (DEPRECATED)
     * Kept for backwards-compat with older call sites.
     */
    @Deprecated(
        message = "Use getUserTokenAsync() instead (suspend). This method returns only the cached token and will not auto-register.",
        replaceWith = ReplaceWith("getUserTokenAsync()"),
        level = DeprecationLevel.WARNING
    )
    fun getUserToken(): String? {
        // Return cached token if available
        return getCachedToken()
    }

    /**
     * Get user token asynchronously - returns cached token or null if not set
     * No longer automatically registers anonymous tokens
     */
    suspend fun getUserTokenAsync(): String? {
        // Return cached token if available
        return cachedUserToken ?: getCachedToken()
    }

    /**
     * Get user token asynchronously, registering anonymously if needed (DEPRECATED)
     * @deprecated Use getUserTokenAsync() and handle null case, or setUserToken() to provide user token
     */
    @Deprecated(
        message = "Use getUserTokenAsync() and handle null case, or setUserProvidedToken() to provide token. This auto-registers anonymously as a fallback.",
        replaceWith = ReplaceWith("getUserTokenAsync()"),
        level = DeprecationLevel.WARNING
    )
    suspend fun getUserTokenAsyncAutoRegister(): String {
        // Return cached token if available
        cachedUserToken?.let { return it }

        // Register anonymously and cache the token
        return try {
            val response: AnonymousRegisterResponse = apiClient.anonymousRegister()
            val token = response.userToken
            setUserToken(token = token)
            token
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // Fallback: generate local UUID if network fails
            val fallbackToken = java.util.UUID.randomUUID().toString()
            setUserToken(token = fallbackToken)
            fallbackToken
        }
    }

    /**
     * Get cached user token without network call
     */
    fun getCachedUserToken(): String? = getCachedToken()

    /**
     * Set user token (used after successful registration)
     */
    private fun setUserToken(token: String) {
        encryptedPrefs.edit().putString("user_token", token).apply()
        cachedUserToken = token
        updateSentryUserContext(token = token)
    }

    /**
     * Set user-provided token
     * This method allows users to manually set their token
     */
    fun setUserProvidedToken(token: String) {
        if (token.isBlank()) {
            com.youtube.rating.android.sentry.SentryLogger.captureMessage(
                "setUserProvidedToken: blank token ignored"
            )
            return
        }
        setUserToken(token = token.trim())
    }

    /**
     * Check if user token exists
     */
    fun hasUserToken(): Boolean = getCachedToken() != null

    /**
     * TEMPORARY: Set test user token for testing tab usage
     * Debug-only helper. Use setUserProvidedToken instead.
     */
    @Deprecated("Use setUserProvidedToken instead")
    fun setTestUserToken(token: String) {
        if (!BuildConfig.DEBUG) return
        if (token.isNotBlank()) setUserProvidedToken(token = token)
    }

    private fun updateSentryUserContext(token: String?) {
        AppScope.get().makeIOCall {
            val installId = InstallPrefs.getInstallId(appContext)
            SentryUserContextProvider.setUserContext(token = token, installId = installId)
        }
    }
}
