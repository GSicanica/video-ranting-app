package com.youtube.rating.android.utils

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Context
import android.net.Uri
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.Logger
import com.youtube.rating.shared.common.getFromMemory
import com.youtube.rating.shared.common.orElse
import com.youtube.rating.shared.common.putInMemory
import com.youtube.rating.core.file.deleteSafely
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import com.youtube.rating.android.data.prefs.BackupPrefs

/**
 * Manages automatic backups after data changes
 * - Auto-saves after every change
 * - Keeps multiple versions (rolling backups)
 * - Allows easy restore to previous state
 * 
 * ✅ FIXED: Uses SupervisorJob for proper error isolation
 * ✅ FIXED: Added cleanup() method to prevent memory leaks
 */
class AutoBackupManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AutoBackupManager"

        private const val BACKUP_DELAY_MS = 2000L // Default delay (overridden by device profile)
        private const val MAX_AUTO_BACKUPS = 10 // Default max (overridden by device profile)
        private const val BACKUP_FILE_PREFIX = "auto_backup_"

        private const val MEM_KEY_GDRIVE_URI = "gdrive_auto_backup_uri"
    }
    
    // ✅ FIX: Use SupervisorJob instead of Job() for better error isolation
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    @Volatile
    private var backupJob: Job? = null

    private val perfProfile = PerformanceProfile.get(context)
    private val backupDelayMs: Long = when (perfProfile.deviceClass) {
        PerformanceProfile.DeviceClass.LOW -> 6000L
        PerformanceProfile.DeviceClass.MID -> 3000L
        PerformanceProfile.DeviceClass.HIGH -> BACKUP_DELAY_MS
    }
    private val maxAutoBackups: Int = when (perfProfile.deviceClass) {
        PerformanceProfile.DeviceClass.LOW -> 5
        PerformanceProfile.DeviceClass.MID -> 8
        PerformanceProfile.DeviceClass.HIGH -> MAX_AUTO_BACKUPS
    }

    @Volatile
    private var lastDriveBackupAt: Long = 0L
    
    // NAPOMENA: getExternalFilesDir() se brise pri deinstalaciji na Android 10+ (API 29+).
    // Za prezivljavanje deinstalacije, koristiti Google Drive backup ili rucni export.
    private val backupDir = if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED) {
        File(context.getExternalFilesDir(null), "auto_backups")
    } else {
        File(context.filesDir, "auto_backups")
    }
    
    init {
        // Ensure backup directory exists
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }
    
    /**
     * Trigger auto backup after a change
     * Uses debouncing to avoid too frequent backups
     */
    fun triggerAutoBackup(changeType: ChangeType) {
        // Cancel previous pending backup
        backupJob?.cancel()
        
        // Schedule new backup after delay
        backupJob = scope.launch {
            delay(backupDelayMs)
            performAutoBackup(changeType = changeType)
        }
    }
    
    /**
     * ✅ FIXED: Cleanup method to prevent memory leaks
     * Call this from Application.onTerminate()
     */
    fun cleanup() {
        backupJob?.cancel()
        scope.cancel()
        Logger.info(TAG, "AutoBackupManager cleaned up - scope cancelled")
    }
    
    /**
     * Perform the actual backup
     */
    private suspend fun performAutoBackup(changeType: ChangeType) {
        try {
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "$BACKUP_FILE_PREFIX${timestamp}.json")
            
            // Create backup data
            var backupData = DataBackup.createBackupData(context)

            // If change likely affected notes or offline videos and data not present yet, retry once after short delay
            if ((changeType == ChangeType.NOTE_ADDED || changeType == ChangeType.NOTE_UPDATED || changeType == ChangeType.NOTE_DELETED) &&
                (backupData.optJSONArray("notes")?.length().orElse(0)) == 0) {
                delay(500)
                backupData = DataBackup.createBackupData(context)
            }
            if ((changeType == ChangeType.OFFLINE_VIDEO_ADDED || changeType == ChangeType.OFFLINE_VIDEO_UPDATED || changeType == ChangeType.OFFLINE_VIDEO_REMOVED) &&
                (backupData.optJSONArray("offlineVideos")?.length().orElse(0)) == 0) {
                delay(500)
                backupData = DataBackup.createBackupData(context)
            }
            backupFile.writeText(backupData.toString())
            
            // Clean up old backups
            cleanupOldAutoBackups()

            // Optional: mirror latest auto-backup to a user-selected Google Drive file.
            maybeUploadToGoogleDrive(backupFile)
            
            if (LogConfig.ENABLE_LOGS) {
                Logger.info(TAG, "Auto-backup created for $changeType: ${backupFile.name}")
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            // ✅ FIX: Use Logger instead of printStackTrace
            Logger.error(TAG, "Auto-backup failed for $changeType", e)
        }
    }

    private suspend fun maybeUploadToGoogleDrive(backupFile: File) {
        try {
            val now = System.currentTimeMillis()
            // Throttle Drive writes: still "after each change", but debounced/batched.
            val throttleMs = if (perfProfile.deviceClass == PerformanceProfile.DeviceClass.LOW) 30_000L else 15_000L
            if (now - lastDriveBackupAt < throttleMs) return

            val enabled = BackupPrefs.getGoogleDriveAutoBackupEnabled(context)
            if (!enabled) return

            val uriStr = BackupPrefs.getGoogleDriveAutoBackupUri(context).trim()
            if (uriStr.isBlank()) return

            // Cache Uri parsing to avoid re-parsing on frequent backups.
            val cached: Pair<String, Uri>? = getFromMemory(MEM_KEY_GDRIVE_URI)
            val uri = if (cached != null && cached.first == uriStr) {
                cached.second
            } else {
                Uri.parse(uriStr).also { putInMemory(key = MEM_KEY_GDRIVE_URI, any = uriStr to it) }
            }
            val content = backupFile.readText()

            context.contentResolver.openOutputStream(uri, "rwt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return

            lastDriveBackupAt = now
            if (LogConfig.ENABLE_LOGS) {
                Logger.info(TAG, "Google Drive auto-backup updated (${backupFile.name})")
            }
        } catch (e: SecurityException) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Google Drive auto-backup permission error", e)
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            Logger.error(TAG, "Google Drive auto-backup failed", e)
        }
    }
    
    /**
     * Clean up old auto-backups, keeping only the most recent
     */
    private fun cleanupOldAutoBackups() {
        try {
            val autoBackups = backupDir.listFiles()
                ?.filter { it.name.startsWith(BACKUP_FILE_PREFIX) }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            if (autoBackups.size > maxAutoBackups) {
                autoBackups.drop(maxAutoBackups).forEach { file ->
                    if (file.deleteSafely()) {
                        if (LogConfig.ENABLE_LOGS) {
                            Logger.info(TAG, "Deleted old auto-backup: ${file.name}")
                        }
                    } else {
                        if (LogConfig.ENABLE_LOGS) {
                            Logger.warning(TAG, "Failed to delete auto-backup: ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            if (LogConfig.ENABLE_LOGS) {
                Logger.error(TAG, "Cleanup failed", e)
            }
        }
    }
    
    /**
     * Get list of available auto-backups for restore
     */
    fun getAutoBackups(): List<AutoBackupInfo> {
        if (!backupDir.exists()) return emptyList()
        
        return backupDir.listFiles()
            ?.filter { it.name.startsWith(BACKUP_FILE_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                AutoBackupInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    timestamp = file.lastModified(),
                    size = file.length()
                )
            }
            ?: emptyList()
    }
    
    /**
     * Restore from auto-backup
     */
    suspend fun restoreFromAutoBackup(backupFilePath: String): RestoreResult {
        return kotlinx.coroutines.withContext(ioDispatcher) {
            DataBackup.restoreBackup(context, backupFilePath)
        }
    }
    
    /**
     * Restore from latest auto-backup
     */
    suspend fun restoreLatestBackup(): RestoreResult {
        return kotlinx.coroutines.withContext(ioDispatcher) {
            val latestBackup = getAutoBackups().firstOrNull()
            if (latestBackup != null) {
                DataBackup.restoreBackup(context, latestBackup.filePath)
            } else {
                RestoreResult.Failed("Nema dostupnih sigurnosnih kopija")
            }
        }
    }
    
    /**
     * Delete a specific auto-backup
     */
    fun deleteAutoBackup(backupFilePath: String): Boolean {
        return try {
            File(backupFilePath).delete()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            false
        }
    }
    
    /**
     * Export latest backup to external location (user-selected via SAF)
     * Returns backup content as string for sharing
     */
    suspend fun getLatestBackupForExport(): String? {
        return kotlinx.coroutines.withContext(ioDispatcher) {
            try {
                val latestBackup = getAutoBackups().firstOrNull()
                latestBackup?.let { backup ->
                    File(backup.filePath).readText()
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                if (LogConfig.ENABLE_LOGS) {
                    Logger.error(TAG, "Failed to read backup for export", e)
                }
                null
            }
        }
    }
    
    /**
     * Import backup from external source (user-selected file content)
     */
    suspend fun importBackupFromExternal(backupContent: String): RestoreResult {
        return kotlinx.coroutines.withContext(ioDispatcher) {
            try {
                // Create temporary file
                val tempFile = File(context.cacheDir, "imported_backup_${System.currentTimeMillis()}.json")
                tempFile.writeText(backupContent)
                
                // Restore from temp file
                val result = DataBackup.restoreBackup(context, tempFile.absolutePath)

                // Clean up temp file
                if (!tempFile.delete()) {
                    if (LogConfig.ENABLE_LOGS) {
                        Logger.warning(TAG, "Failed to delete temp backup file: ${tempFile.absolutePath}")
                    }
                }

                result
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                if (LogConfig.ENABLE_LOGS) {
                    Logger.error(TAG, "Failed to import backup", e)
                }
                RestoreResult.Failed(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Delete all auto-backups
     */
    fun deleteAllAutoBackups(): Int {
        var deleted = 0
        backupDir.listFiles()?.forEach { file ->
            if (file.name.startsWith(BACKUP_FILE_PREFIX) && file.delete()) {
                deleted++
            }
        }
        return deleted
    }
}