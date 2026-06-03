package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey

object InstallPrefs : BasePrefs() {
    private val KEY_INSTALL_ID = stringPreferencesKey("install_id")

    @Volatile
    private var cachedInstallId: String? = null

    suspend fun getInstallId(context: Context): String? {
        return cachedInstallId
            ?: readPrefNullable(context, KEY_INSTALL_ID).also { cachedInstallId = it }
    }

    suspend fun setInstallId(context: Context, installId: String) {
        editPref(context) { it[KEY_INSTALL_ID] = installId }
        cachedInstallId = installId
    }
}