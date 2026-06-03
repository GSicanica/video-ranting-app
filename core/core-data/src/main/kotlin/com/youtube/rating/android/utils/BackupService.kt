package com.youtube.rating.android.utils

interface BackupService {
    suspend fun loadAutoBackups(): List<AutoBackupInfo>
    suspend fun createBackup(): BackupResult
    suspend fun restoreBackup(backupPath: String): RestoreResult
    suspend fun autoBackupIfNeeded()
    suspend fun restoreLatestBackup(): RestoreResult
    suspend fun getLatestBackupForExport(): String?
    suspend fun importBackupFromExternal(backupContent: String): RestoreResult
    suspend fun deleteAutoBackup(filePath: String): Boolean

    suspend fun exportToGoogleDrive(uri: String): Result<Unit>
    fun getSuggestedBackupFilename(): String
    suspend fun importFromGoogleDrive(uri: String): RestoreResult

    suspend fun setGoogleDriveAutoBackup(enabled: Boolean, uri: String? = null)
}
