package com.youtube.rating.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.network.OkHttpClients
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.utils.AutoBackupInfo
import com.youtube.rating.android.utils.BackupResult
import com.youtube.rating.android.data.settings.SettingsRepository
import com.youtube.rating.android.data.settings.SettingsState
import com.youtube.rating.android.domain.usecase.CreateBackupUseCase
import com.youtube.rating.android.domain.usecase.DeleteAutoBackupUseCase
import com.youtube.rating.android.domain.usecase.ExportToGoogleDriveUseCase
import com.youtube.rating.android.domain.usecase.GetLatestBackupForExportUseCase
import com.youtube.rating.android.domain.usecase.GetSuggestedBackupFilenameUseCase
import com.youtube.rating.android.domain.usecase.ImportBackupFromExternalUseCase
import com.youtube.rating.android.domain.usecase.ImportFromGoogleDriveUseCase
import com.youtube.rating.android.domain.usecase.LoadAutoBackupsUseCase
import com.youtube.rating.android.domain.usecase.RestoreBackupUseCase
import com.youtube.rating.android.domain.usecase.RestoreLatestBackupUseCase
import com.youtube.rating.android.domain.usecase.SetGoogleDriveAutoBackupUseCase
import com.youtube.rating.android.utils.BackupTrigger
import com.youtube.rating.android.utils.ChangeType
import com.youtube.rating.android.utils.BibleReminderService
import com.youtube.rating.android.utils.NotificationScheduler
import com.youtube.rating.shared.STREAM_UPLOAD_ENDPOINT
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.shared.debug.NetworkDebugStore
import com.youtube.rating.android.utils.RestoreResult
import com.youtube.rating.android.utils.RestoreStats
import com.youtube.rating.shared.models.SaintOfDayResponse
import com.youtube.rating.shared.utils.LogConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.Request
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.prefs.BibleReaderPrefs
import com.youtube.rating.android.data.prefs.HomePrefs
import com.youtube.rating.android.data.prefs.PsalmPrefs
import com.youtube.rating.android.data.prefs.SaintsPrefs
import com.youtube.rating.android.data.prefs.ScrollEffectsPrefs
import com.youtube.rating.android.data.prefs.ThemeColors
import com.youtube.rating.core.designsystem.theme.ThemeMode
import com.youtube.rating.android.data.prefs.VideoPrefs
import kotlinx.coroutines.flow.first
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * ViewModel for Settings Screen
 * Manages backup/restore operations and settings state
 * ✅ Uses DI-injected GoogleDriveBackupManager for consistent state
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val loadAutoBackupsUseCase: LoadAutoBackupsUseCase,
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val deleteAutoBackupUseCase: DeleteAutoBackupUseCase,
    private val setGoogleDriveAutoBackupUseCase: SetGoogleDriveAutoBackupUseCase,
    private val restoreLatestBackupUseCase: RestoreLatestBackupUseCase,
    private val getLatestBackupForExportUseCase: GetLatestBackupForExportUseCase,
    private val importBackupFromExternalUseCase: ImportBackupFromExternalUseCase,
    private val exportToGoogleDriveUseCase: ExportToGoogleDriveUseCase,
    private val getSuggestedBackupFilenameUseCase: GetSuggestedBackupFilenameUseCase,
    private val importFromGoogleDriveUseCase: ImportFromGoogleDriveUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val backupTrigger: BackupTrigger,
    private val apiClient: RatingApiClient,
    private val userTokenManager: UserTokenManager,
    private val bibleReminderService: BibleReminderService,
) : ViewModel() {

    val settingsState: StateFlow<SettingsState> = settingsRepository.state

    // Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Backup State
    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _backupError = MutableStateFlow<String?>(null)
    val backupError: StateFlow<String?> = _backupError.asStateFlow()

    // Auto-backup info
    private val _autoBackupInfo = MutableStateFlow<List<AutoBackupInfo>>(emptyList())
    val autoBackupInfo: StateFlow<List<AutoBackupInfo>> = _autoBackupInfo.asStateFlow()

    private val _isUploadServerTesting = MutableStateFlow(false)
    val isUploadServerTesting: StateFlow<Boolean> = _isUploadServerTesting.asStateFlow()

    private val _uploadServerTestResult = MutableStateFlow<String?>(null)
    val uploadServerTestResult: StateFlow<String?> = _uploadServerTestResult.asStateFlow()

    private val _isLiveKitTesting = MutableStateFlow(false)
    val isLiveKitTesting: StateFlow<Boolean> = _isLiveKitTesting.asStateFlow()

    private val _liveKitTestResult = MutableStateFlow<String?>(null)
    val liveKitTestResult: StateFlow<String?> = _liveKitTestResult.asStateFlow()

    private val _isGeneratingUserId = MutableStateFlow(false)
    val isGeneratingUserId: StateFlow<Boolean> = _isGeneratingUserId.asStateFlow()

    private val _generatedUserId = MutableStateFlow<String?>(null)
    val generatedUserId: StateFlow<String?> = _generatedUserId.asStateFlow()

    private val _reportStatus = MutableStateFlow<String?>(null)
    val reportStatus: StateFlow<String?> = _reportStatus.asStateFlow()

    private val _saintLookupStatus = MutableStateFlow<String?>(null)
    val saintLookupStatus: StateFlow<String?> = _saintLookupStatus.asStateFlow()

    private val _saintLookupResult = MutableStateFlow<SaintOfDayResponse?>(null)
    val saintLookupResult: StateFlow<SaintOfDayResponse?> = _saintLookupResult.asStateFlow()

    private val _psalmSyncStatus = MutableStateFlow<String?>(null)
    val psalmSyncStatus: StateFlow<String?> = _psalmSyncStatus.asStateFlow()

    fun clearMessages() {
        _backupMessage.value = null
        _backupError.value = null
    }

    fun setBackupError(message: String) {
        _backupError.value = message
    }

    fun setBackupMessage(message: String) {
        _backupMessage.value = message
    }

    fun clearGeneratedUserId() {
        _generatedUserId.value = null
    }

    fun clearReportStatus() {
        _reportStatus.value = null
    }

    fun clearPsalmSyncStatus() {
        _psalmSyncStatus.value = null
    }

    fun debugSyncPsalmHighlights(context: Context) {
        makeIOCall {
            _psalmSyncStatus.value = "Šaljem..."
            try {
                val highlights = PsalmPrefs.highlightedPsalmLinesFlow(context).first().toList()
                if (highlights.size > 100) {
                    _psalmSyncStatus.value = "Previše označenih (${highlights.size})"
                    return@makeIOCall
                }
                @Suppress("DEPRECATION")
                val userToken = userTokenManager.getUserTokenAsyncAutoRegister()
                val res = apiClient.syncPsalmHighlights(
                    userToken = userToken,
                    highlights = highlights,
                    displayName = "Debug User",
                    gender = "male",
                    notMarried = true
                )
                _psalmSyncStatus.value = if (res.success) {
                    "OK: poslano ${res.highlightCount}"
                } else {
                    "Greška: ${res.message ?: "nepoznato"}"
                }
            } catch (e: Exception) {
                _psalmSyncStatus.value = "Greška: ${e.message ?: "nepoznato"}"
            }
        }
    }

    fun setGoogleDriveAutoBackup(context: Context, enabled: Boolean, uri: String? = null) {
        makeIOCall {
            setGoogleDriveAutoBackupUseCase(enabled, uri)
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        makeIOCall {
            settingsRepository.setThemeMode(mode)
        }
    }

    @Deprecated("Use setThemeMode(mode = ThemeMode) instead.")
    fun setDarkMode(context: Context, darkMode: Boolean?) {
        makeIOCall {
            settingsRepository.setDarkMode(darkMode)
        }
    }

    fun applyThemeColor(context: Context, argb: Int?) {
        makeIOCall {
            val colors = if (argb == null) null else ThemeColors(primary = argb, secondary = argb, tertiary = argb)
            settingsRepository.setThemeColors(colors)
            runCatching { backupTrigger.trigger(ChangeType.THEME_CHANGED) }
        }
    }

    fun resetThemeColors(context: Context) = applyThemeColor(context = context, argb = null)

    fun setHomeScreenStyle(context: Context, key: String) {
        makeIOCall {
            settingsRepository.setHomeScreenStyle(key)
        }
    }

    fun setWatchHistoryEnabled(context: Context, enabled: Boolean) {
        makeIOCall {
            settingsRepository.setWatchHistoryEnabled(enabled)
        }
    }

    fun setResumePlaybackEnabled(context: Context, enabled: Boolean) {
        makeIOCall {
            settingsRepository.setResumePlaybackEnabled(enabled)
        }
    }

    fun setNewVideoNotificationsEnabled(context: Context, enabled: Boolean) {
        makeIOCall {
            settingsRepository.setNewVideoNotificationsEnabled(enabled)
            if (!enabled) {
                notificationScheduler.cancelNewVideoNotifications(context)
                return@makeIOCall
            }
            notificationScheduler.scheduleNewVideoNotifications(context)
        }
    }

    fun setSaintOfDayNotificationsEnabled(context: Context, enabled: Boolean) {
        makeIOCall {
            SaintsPrefs.setSaintOfDayNotificationsEnabled(context, enabled)
        }
    }

    fun saveBibleReminder(enabled: Boolean, hour: Int, minute: Int) {
        makeIOCall {
            bibleReminderService.saveReminderSettings(
                BibleReminderService.Settings(enabled = enabled, hour = hour, minute = minute)
            )
        }
    }

    fun setDisableScrollEffects(context: Context, enabled: Boolean) {
        makeIOCall {
            ScrollEffectsPrefs.setDisabled(context, !enabled)
        }
    }

    fun setLockFeaturedHome(context: Context, enabled: Boolean) {
        makeIOCall {
            HomePrefs.setLockFeaturedHome(context, enabled)
        }
    }

    fun setDebugUnlocked(context: Context, enabled: Boolean) {
        makeIOCall {
            AdminPrefs.setDebugUnlocked(context, enabled)
        }
    }

    fun setReleaseLogsEnabled(context: Context, enabled: Boolean) {
        makeIOCall {
            AdminPrefs.setReleaseLogsEnabled(context, enabled)
        }
    }

    fun saveQuickSearchTerms(context: Context, terms: List<String>) {
        makeIOCall {
            HomePrefs.setQuickSearchTerms(context, terms)
        }
    }

    fun clearCache(context: Context, onDone: () -> Unit) {
        makeIOCall {
            context.cacheDir.deleteRecursively()
            viewModelScope.launch(Dispatchers.Main) { onDone() }
        }
    }

    fun setOfflineBibleBook(context: Context, bookId: String?) {
        makeIOCall {
            BibleReaderPrefs.setOfflineBibleBook(context, bookId)
        }
    }

    fun resetSaintLookup(date: String) {
        _saintLookupStatus.value = null
        _saintLookupResult.value = null
        if (date.isNotBlank()) {
            // no-op, UI owns date field; this keeps API symmetric for intent-style calls
        }
    }

    fun testUploadServerPing() {
        if (_isUploadServerTesting.value) return
        makeIOCall {
            _isUploadServerTesting.value = true
            val result = runCatching {
                val conn = (URL(STREAM_UPLOAD_ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                conn.connect()
                val code = conn.responseCode
                conn.disconnect()
                "GET $code"
            }.getOrElse { "GREŠKA: ${it.message}" }
            _uploadServerTestResult.value = result
            NetworkDebugStore.add("UPLOAD SERVER PING $STREAM_UPLOAD_ENDPOINT => $result")
            _isUploadServerTesting.value = false
        }
    }

    fun testUploadServerPost() {
        if (_isUploadServerTesting.value) return
        makeIOCall {
            _isUploadServerTesting.value = true
            val result = runCatching {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .build()
                val request = Request.Builder()
                    .url(STREAM_UPLOAD_ENDPOINT)
                    .post(body)
                    .header("Expect", "")
                    .build()
                OkHttpClients.debugUploadTestClient.newCall(request).execute().use { resp ->
                    "POST ${resp.code}"
                }
            }.getOrElse { "GREŠKA: ${it.message}" }
            _uploadServerTestResult.value = result
            NetworkDebugStore.add("UPLOAD SERVER POST TEST $STREAM_UPLOAD_ENDPOINT => $result")
            _isUploadServerTesting.value = false
        }
    }

    fun testLiveKitPing() {
        if (_isLiveKitTesting.value) return
        makeIOCall {
            _isLiveKitTesting.value = true
            val token = userTokenManager.getUserTokenAsync()
            if (token.isNullOrBlank()) {
                _liveKitTestResult.value = "❌ Nema user tokena"
                _isLiveKitTesting.value = false
                return@makeIOCall
            }
            val result = runCatching {
                val response = apiClient.getLiveKitToken(
                    userToken = token,
                    room = "admin_ping",
                    identity = "admin_ping_${token.take(6)}",
                    name = "Admin Ping"
                )
                if (!response.token.isNullOrBlank()) {
                    "✅ Token OK"
                } else {
                    "❌ Prazan token"
                }
            }.getOrElse { "❌ Greška: ${it.message}" }
            _liveKitTestResult.value = result
            NetworkDebugStore.add("LIVEKIT PING => $result")
            _isLiveKitTesting.value = false
        }
    }

    fun generateNewUserId() {
        if (_isGeneratingUserId.value) return
        makeIOCall {
            _isGeneratingUserId.value = true
            val message = try {
                val response = apiClient.anonymousRegister()
                val newToken = response.userToken
                userTokenManager.setUserProvidedToken(newToken)
                "Novi User ID: $newToken"
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                "Greška: ${e.message}"
            }
            _generatedUserId.value = message
            _isGeneratingUserId.value = false
        }
    }

    fun submitVideoReport(videoId: String, reason: String) {
        if (videoId.isBlank()) {
            _reportStatus.value = "❌ Video ID je obavezan"
            return
        }
        makeIOCall {
            val token = userTokenManager.getUserTokenAsync()
            if (token.isNullOrBlank()) {
                _reportStatus.value = "❌ Nema user tokena"
                return@makeIOCall
            }
            _reportStatus.value = "⏳ Šaljem..."
            val resp = runCatching {
                apiClient.reportVideo(
                    videoId.trim(),
                    token,
                    reason.trim()
                )
            }.getOrNull()
            _reportStatus.value = if (resp?.success == true) {
                "✅ Poslano"
            } else {
                "❌ Greška: ${resp?.message ?: "nepoznato"}"
            }
        }
    }

    fun lookupSaintByDate(date: String) {
        val valid = runCatching { LocalDate.parse(date) }.isSuccess
        if (!valid) {
            _saintLookupStatus.value = "❌ Datum mora biti formata YYYY-MM-DD"
            return
        }
        makeIOCall {
            _saintLookupStatus.value = "⏳ Dohvat..."
            _saintLookupResult.value = null
            val resp = runCatching { apiClient.getSaintOfDay(date) }.getOrNull()
            if (resp?.success == true) {
                _saintLookupResult.value = resp
                _saintLookupStatus.value = "✅ Uspješno dohvaćen"
            } else {
                _saintLookupStatus.value = "❌ Greška: ${resp?.message ?: "nepoznato"}"
            }
        }
    }

    /**
     * Load auto-backup information
     * ✅ FIX: StateFlow updates are thread-safe
     */
    fun loadAutoBackupInfo() {
        makeIOCall {
            try {
                val backups = loadAutoBackupsUseCase()
                _autoBackupInfo.value = backups
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                LogConfig.logError("Error loading auto-backup info", e)
            }
        }
    }

    /**
     * Create backup manually
     * ✅ FIX: StateFlow updates are thread-safe
     */
    fun createBackup(context: Context) {
        makeIOCall {
            _isLoading.value = true
            _backupError.value = null
            _backupMessage.value = null

            try {
                when (val result = createBackupUseCase()) {
                    is BackupResult.Success -> {
                        val fileName = File(result.filePath).name
                        _backupMessage.value = Strings.backupCreatedSuccess(fileName)
                        
                        // Trigger auto-backup as well
                        // Note: auto-backup already handled by AutoBackupManager via BackupService
                    }
                    is BackupResult.Failed -> {
                        _backupError.value = Strings.genericError(result.error)
                    }
                }
                _isLoading.value = false
                loadAutoBackupInfo()
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _backupError.value = Strings.errorDuringCreation(e.message)
                _isLoading.value = false
                LogConfig.logError("❌ Backup creation error", e)
            }
        }
    }

    private fun formatRestoreMessage(
        prefix: String,
        stats: RestoreStats
    ): String {
        return "$prefix: ${stats.notesRestored} bilješke, " +
            "${stats.favoritesRestored} favoriti, " +
            "${stats.offlineVideosRestored} offline videa, " +
            "${stats.rosarySessionsRestored} krunčanih sesija, " +
            "${stats.tasksRestored} taskova"
    }

    /**
     * Restore from backup file
     * ✅ FIX: StateFlow updates are thread-safe
     */
    fun restoreBackup(context: Context, backupFile: File) {
        makeIOCall {
            _isLoading.value = true
            _backupError.value = null
            _backupMessage.value = null

            try {
                when (val result = restoreBackupUseCase(backupFile.absolutePath)) {
                    is RestoreResult.Success -> {
                        val stats = result.stats
                        _backupMessage.value = formatRestoreMessage(prefix = Strings.successfullyRestored, stats = stats)
                    }
                    is RestoreResult.Failed -> {
                        _backupError.value = Strings.genericError(result.error)
                    }
                }
                _isLoading.value = false
                loadAutoBackupInfo()
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _backupError.value = Strings.errorDuringRestore(e.message)
                _isLoading.value = false
                LogConfig.logError("❌ Restore error", e)
            }
        }
    }

    /**
     * Restore from latest auto-backup
     * ✅ FIX: StateFlow updates are thread-safe
     */
    fun restoreLatestBackup() {
        makeIOCall {
            _isLoading.value = true
            _backupError.value = null
            _backupMessage.value = null

            try {
                when (val result = restoreLatestBackupUseCase()) {
                    is RestoreResult.Success -> {
                        val stats = result.stats
                        _backupMessage.value = formatRestoreMessage(prefix = Strings.successfullyRestored, stats = stats)
                    }
                    is RestoreResult.Failed -> {
                        _backupError.value = result.error
                    }
                }
                _isLoading.value = false
                loadAutoBackupInfo()
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _backupError.value = Strings.genericError(e.message)
                _isLoading.value = false
                LogConfig.logError("❌ Restore latest error", e)
            }
        }
    }

    /**
     * Export latest backup for sharing
     */
    suspend fun getLatestBackupForExport(): String? {
        return try {
            getLatestBackupForExportUseCase()
        } catch (e: Exception) {
            com.youtube.rating.android.sentry.SentryLogger.captureException(e)
            LogConfig.logError("❌ Export error", e)
            null
        }
    }

    /**
     * Import backup from external source
     * ✅ FIX: StateFlow updates are thread-safe
     */
    fun importBackupFromExternal(backupContent: String) {
        makeIOCall {
            _isLoading.value = true
            _backupError.value = null
            _backupMessage.value = null

            try {
                when (val result = importBackupFromExternalUseCase(backupContent)) {
                    is RestoreResult.Success -> {
                        val stats = result.stats
                        _backupMessage.value = formatRestoreMessage(prefix = "Uspješno uvezeno", stats = stats)
                    }
                    is RestoreResult.Failed -> {
                        _backupError.value = result.error
                    }
                }
                _isLoading.value = false
                loadAutoBackupInfo()
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _backupError.value = "Greška pri uvozu: ${e.message}"
                _isLoading.value = false
                LogConfig.logError("❌ Import error", e)
            }
        }
    }

    /**
     * Delete specific auto-backup
     */
    fun deleteAutoBackup(backupInfo: AutoBackupInfo) {
        makeIOCall {
            try {
                val success = deleteAutoBackupUseCase(backupInfo.filePath)
                
                if (success) {
                    LogConfig.log("🗑️ Auto-backup deleted: ${backupInfo.fileName}")
                    loadAutoBackupInfo()
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                LogConfig.logError("❌ Delete backup error", e)
            }
        }
    }

    /**
     * Export backup to Google Drive (via Storage Access Framework)
     * ✅ Uses file picker for user to choose save location
     * ✅ Uses injected googleDriveBackupManager for consistent state
     */
    fun exportToGoogleDrive(context: Context, uri: android.net.Uri) {
        makeIOCall {
            _isLoading.value = true
            _backupError.value = null
            _backupMessage.value = null

            try {
                val result = exportToGoogleDriveUseCase(uri.toString())
                if (result.isSuccess) {
                    _backupMessage.value = "✅ Backup uspješno spremljen na Google Drive!"
                    LogConfig.log("📤 Google Drive backup exported successfully")
                } else {
                    val e = result.exceptionOrNull()
                    _backupError.value = "Greška pri spremanju: ${e?.message}"
                    LogConfig.logError("❌ Google Drive export error", e)
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _backupError.value = "Greška pri spremanju: ${e.message}"
                LogConfig.logError("❌ Google Drive export error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get suggested filename for backup
     */
    fun getSuggestedBackupFilename(): String {
        return getSuggestedBackupFilenameUseCase()
    }

    /**
     * Import backup from Google Drive (via Storage Access Framework)
     * ✅ Uses file picker for user to choose file to restore
     * ✅ Uses injected googleDriveBackupManager which uses same FavoritesManager as HomeScreen
     */
    fun importFromGoogleDrive(context: Context, uri: android.net.Uri) {
        makeIOCall {
            _isLoading.value = true
            _backupError.value = null
            _backupMessage.value = null

            try {
                when (val result = importFromGoogleDriveUseCase(uri.toString())) {
                    is RestoreResult.Success -> {
                        val stats = result.stats
                        _backupMessage.value = "✅ " + formatRestoreMessage(prefix = "Uspješno uvezeno sa Google Drive", stats = stats)
                        LogConfig.log("📥 Google Drive backup imported: ${stats.favoritesRestored} favorites, ${stats.rosarySessionsRestored} rosary sessions")
                    }
                    is RestoreResult.Failed -> {
                        _backupError.value = result.error
                        LogConfig.logError("❌ Google Drive restore failed: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _backupError.value = "Greška pri učitavanju: ${e.message}"
                LogConfig.logError("❌ Google Drive import error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
