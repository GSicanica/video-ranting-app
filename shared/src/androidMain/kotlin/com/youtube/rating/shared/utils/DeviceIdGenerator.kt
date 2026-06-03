package com.youtube.rating.shared.utils

import android.content.Context
import java.util.UUID

object AndroidDeviceIdGenerator {
    private const val PREFS_FILE_NAME = "secure_app_prefs"
    private const val INSTALL_ID_KEY = "install_id"
    private const val LEGACY_PREFS_FILE_NAME = "app_prefs"
    private const val LEGACY_DEVICE_ID_KEY = "device_id"

    fun generateInstallId(context: Context): String {
        return getOrCreateInstallId(context = context)
    }

    private fun getOrCreateInstallId(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

        // Prefer the legacy app-scoped ID if present so existing users keep history.
        // The Android app always maintains this under app_prefs/device_id.
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val legacyId = legacyPrefs.getString(LEGACY_DEVICE_ID_KEY, null)
        if (!legacyId.isNullOrEmpty()) {
            // Ensure we also store it under the new key for consistency.
            sharedPreferences.edit().putString(INSTALL_ID_KEY, legacyId).apply()
            return legacyId
        }

        // Check if install ID already exists
        val existingId = sharedPreferences.getString(INSTALL_ID_KEY, null)
        if (!existingId.isNullOrEmpty()) {
            return existingId
        }

        // Generate new UUID for first time
        val newInstallId = UUID.randomUUID().toString()

        // Save it
        sharedPreferences.edit()
            .putString(INSTALL_ID_KEY, newInstallId)
            .apply()

        return newInstallId
    }

    // Legacy method for backward compatibility - will be removed
    @Deprecated("Use generateInstallId instead", ReplaceWith("generateInstallId(context = context)"))
    fun generateDeviceId(context: Context): String = generateInstallId(context = context)
}
