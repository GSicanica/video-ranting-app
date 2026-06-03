package com.youtube.rating.android.utils

import android.content.Context
import android.net.Uri
import com.youtube.rating.android.data.prefs.BackupPrefs

class BackupServiceImpl(
    private val appContext: Context,
    private val autoBackupManager: AutoBackupManager,
    private val googleDriveBackupManager: GoogleDriveBackupManager
) : BackupService {
    override suspend fun loadAutoBackups(): List<AutoBackupInfo> {
        return autoBackupManager.getAutoBackups()
    }

    override suspend fun createBackup(): BackupResult {
        return DataBackup.createBackup(appContext)
    }

    override suspend fun restoreBackup(backupPath: String): RestoreResult {
        return DataBackup.restoreBackup(appContext, backupPath)
    }

    override suspend fun autoBackupIfNeeded() {
        DataBackup.autoBackupIfNeeded(appContext)
    }

    override suspend fun restoreLatestBackup(): RestoreResult {
        return autoBackupManager.restoreLatestBackup()
    }

    override suspend fun getLatestBackupForExport(): String? {
        return autoBackupManager.getLatestBackupForExport()
    }

    override suspend fun importBackupFromExternal(backupContent: String): RestoreResult {
        return autoBackupManager.importBackupFromExternal(backupContent)
    }

    override suspend fun deleteAutoBackup(filePath: String): Boolean {
        return autoBackupManager.deleteAutoBackup(filePath)
    }

    override suspend fun exportToGoogleDrive(uri: String): Result<Unit> {
        val parsedUri = Uri.parse(uri)
        return runCatching {
            val jsonString = googleDriveBackupManager.exportBackupToString()
            appContext.contentResolver.openOutputStream(parsedUri)?.use { output ->
                java.io.OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                }
            }
        }
    }

    override fun getSuggestedBackupFilename(): String {
        return googleDriveBackupManager.getSuggestedFilename()
    }

    override suspend fun importFromGoogleDrive(uri: String): RestoreResult {
        val parsedUri = Uri.parse(uri)
        val jsonString = appContext.contentResolver.openInputStream(parsedUri)?.use { input ->
            java.io.InputStreamReader(input, Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        } ?: return RestoreResult.Failed("Ne mogu pročitati datoteku")

        return googleDriveBackupManager.restoreFromString(jsonString)
    }

    override suspend fun setGoogleDriveAutoBackup(enabled: Boolean, uri: String?) {
        uri?.let { BackupPrefs.setGoogleDriveAutoBackupUri(appContext, it) }
        BackupPrefs.setGoogleDriveAutoBackupEnabled(appContext, enabled)
    }
}
