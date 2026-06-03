package com.youtube.rating.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow

object BackupPrefs : BasePrefs() {
    private val KEY_GDRIVE_AUTO_BACKUP_ENABLED = booleanPreferencesKey("gdrive_auto_backup_enabled")
    private val KEY_GDRIVE_AUTO_BACKUP_URI = stringPreferencesKey("gdrive_auto_backup_uri")
    private val KEY_LAST_BACKUP = longPreferencesKey("last_backup_timestamp")

    @Volatile
    private var cachedGDriveAutoEnabled: Boolean? = null
    @Volatile
    private var cachedGDriveAutoUri: String? = null

    fun googleDriveAutoBackupEnabledFlow(context: Context): Flow<Boolean> =
        prefsFlow(context, KEY_GDRIVE_AUTO_BACKUP_ENABLED, false)

    fun googleDriveAutoBackupUriFlow(context: Context): Flow<String> =
        prefsFlow(context, KEY_GDRIVE_AUTO_BACKUP_URI, "")

    suspend fun getGoogleDriveAutoBackupEnabled(context: Context): Boolean {
        return cachedGDriveAutoEnabled
            ?: readPref(context, KEY_GDRIVE_AUTO_BACKUP_ENABLED, false).also { cachedGDriveAutoEnabled = it }
    }

    suspend fun getGoogleDriveAutoBackupUri(context: Context): String {
        return cachedGDriveAutoUri
            ?: readPref(context, KEY_GDRIVE_AUTO_BACKUP_URI, "").also { cachedGDriveAutoUri = it }
    }

    suspend fun setGoogleDriveAutoBackupEnabled(context: Context, enabled: Boolean) {
        editPref(context) { it[KEY_GDRIVE_AUTO_BACKUP_ENABLED] = enabled }
        cachedGDriveAutoEnabled = enabled
    }

    suspend fun setGoogleDriveAutoBackupUri(context: Context, uri: String?) {
        editPref(context) {
            if (uri.isNullOrBlank()) it.remove(KEY_GDRIVE_AUTO_BACKUP_URI) else it[KEY_GDRIVE_AUTO_BACKUP_URI] = uri
        }
        cachedGDriveAutoUri = uri ?: ""
    }

    suspend fun getLastBackupTimestamp(context: Context): Long =
        readPref(context, KEY_LAST_BACKUP, 0L)

    suspend fun setLastBackupTimestamp(context: Context, value: Long) {
        editPref(context) { it[KEY_LAST_BACKUP] = value }
    }
}