package com.youtube.rating.android.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.text.format.Formatter.formatFileSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.localization.LanguageManager
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.components.RatingEmptyState
import com.youtube.rating.android.network.BaseUrlProvider
import com.youtube.rating.android.notifications.NotificationChannels
import com.youtube.rating.android.storage.FastingReminderSettings
import com.youtube.rating.core.designsystem.theme.spacing
import com.youtube.rating.android.utils.AdminManager
import com.youtube.rating.android.utils.ContentLanguageDialogController
import com.youtube.rating.android.utils.DebugConfigProvider
import com.youtube.rating.android.utils.DebugNotificationSender
import com.youtube.rating.shared.debug.NetworkDebugStore
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.LogLevel
import com.youtube.rating.shared.utils.Logger
import java.io.File
import java.time.LocalDate
import java.util.Locale
import com.youtube.rating.core.coroutines.makeIOCall
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.prefs.AppUsagePrefs
import com.youtube.rating.android.data.prefs.BackupPrefs
import com.youtube.rating.android.data.prefs.BiblePlannerPrefs
import com.youtube.rating.android.data.prefs.BibleReaderPrefs
import com.youtube.rating.android.data.prefs.HomePrefs
import com.youtube.rating.android.data.prefs.InstallPrefs
import com.youtube.rating.android.data.prefs.SaintsPrefs
import com.youtube.rating.android.data.prefs.ThemeColors
import com.youtube.rating.android.data.settings.SettingsState
import com.youtube.rating.core.designsystem.theme.ThemeMode
import com.youtube.rating.android.data.prefs.VideoPrefs
import com.youtube.rating.core.designsystem.components.RatingScaffold
import com.youtube.rating.core.designsystem.components.RatingTopAppBar

@Composable
private fun DebugHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        adminManager: AdminManager = koinInject(),
        viewModel: com.youtube.rating.android.viewmodel.SettingsViewModel = koinInject(),
        userTokenManager: com.youtube.rating.android.utils.UserTokenManager = koinInject(),
        apiClient: com.youtube.rating.shared.api.RatingApiClient = koinInject(),
        debugNotificationSender: DebugNotificationSender = koinInject(),
        debugConfigProvider: DebugConfigProvider = koinInject(),
        contentLanguageDialogController: ContentLanguageDialogController = koinInject(),
        onOpenHabitTracker: () -> Unit = {},
        onOpenRunningFeature: () -> Unit = {},
        onOpenKuiverFeature: () -> Unit = {}
) {
    val context = LocalContext.current
    val notificationPermissionRequester = rememberPostNotificationsPermissionRequester()
        val scope = rememberCoroutineScope()
        val scrollState = rememberScrollState()
        val spacing = MaterialTheme.spacing

        // ✅ singletons once
        val languageManager = remember(context) { LanguageManager(context = context) }
        val screenState = rememberSettingsScreenState()
        val networkDebugEntries by NetworkDebugStore.entries.collectAsStateWithLifecycle(initialValue = emptyList())
    val isUploadServerTesting by viewModel.isUploadServerTesting.collectAsStateWithLifecycle()
    val uploadServerTestResult by viewModel.uploadServerTestResult.collectAsStateWithLifecycle()
        val isGeneratingUserId by viewModel.isGeneratingUserId.collectAsStateWithLifecycle()
        val generatedUserId by viewModel.generatedUserId.collectAsStateWithLifecycle()
        val reportStatus by viewModel.reportStatus.collectAsStateWithLifecycle()
        val psalmSyncStatus by viewModel.psalmSyncStatus.collectAsStateWithLifecycle()
        val saintLookupStatus by viewModel.saintLookupStatus.collectAsStateWithLifecycle()
        val saintLookupResult by viewModel.saintLookupResult.collectAsStateWithLifecycle()

        // Admin mode activation counter
        var languageClickCount by remember { mutableIntStateOf(0) }
        val isAdminMode by AdminPrefs.adminModeFlow(context)
            .collectAsStateWithLifecycle(initialValue = adminManager.isAdminMode())

        LaunchedEffect(Unit) {
            screenState.debugUnlocked = AdminPrefs.getDebugUnlocked(context)
}



        val currentLanguage = Strings.currentLanguage
        val contentLanguages = Strings.selectedContentLanguages
        val quickSearchTerms by HomePrefs.quickSearchTermsFlow(context)
            .collectAsStateWithLifecycle(initialValue = emptyList())

        // cache size - compute asynchronously to avoid ANR from file I/O on main thread
        var cacheSize by remember { mutableStateOf(0L) }
        LaunchedEffect(screenState.showClearCacheDialog) {
            makeIOCall {
                cacheSize = settingsCalculateDirectorySize(directory = context.cacheDir)
            }
        }

        // Google Drive export/import
        val exportLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            uri?.let { viewModel.exportToGoogleDrive(context, it) }
        }

        val importLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { viewModel.importFromGoogleDrive(context, it) }
        }

        LaunchedEffect(screenState.showQuickSearchDialog) {
            if (screenState.showQuickSearchDialog) {
                screenState.localQuickSearchTerms = quickSearchTerms
                screenState.quickSearchInput = ""
            }
        }

        LaunchedEffect(generatedUserId) {
            generatedUserId?.let { msg ->
                android.widget.Toast.makeText(
                    context,
                    msg,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                viewModel.clearGeneratedUserId()
            }
        }

        val googleDriveAutoBackupCreateLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            uri?.let {
                // Persist permission so AutoBackupManager can write later (best-effort).
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                scope.launch {
                    viewModel.setGoogleDriveAutoBackup(context, enabled = true, uri = it.toString())
                    android.widget.Toast.makeText(
                        context,
                        Strings.autoBackupToastEnabled,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val gDriveAutoEnabled by BackupPrefs.googleDriveAutoBackupEnabledFlow(context)
            .collectAsStateWithLifecycle(initialValue = false)
        val gDriveAutoUri by BackupPrefs.googleDriveAutoBackupUriFlow(context)
            .collectAsStateWithLifecycle(initialValue = "")
        val settingsState by viewModel.settingsState.collectAsStateWithLifecycle(initialValue = SettingsState())
        val themePrefs = settingsState.themeColors
        val homeScreenStyle = settingsState.homeScreenStyle

        val offlineBibleBook by BibleReaderPrefs.offlineBibleBookFlow(context)
            .collectAsStateWithLifecycle(initialValue = null)

        // App usage time tracking (per-user)
        val currentUserToken = userTokenManager.getCachedUserToken()
        val appUsageTimeMs by AppUsagePrefs.getAppUsageTimeMsFlow(context, currentUserToken)
            .collectAsStateWithLifecycle(initialValue = 0L)

        RatingScaffold(
            topBar = {
                RatingTopAppBar(title = Strings.settingsTitle)
            }
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = spacing.lg, vertical = spacing.lg)
        ) {

            GeneralSettingsSection {
                // DISPLAY SECTION
                SectionHeader(title = Strings.sectionDisplay)

            val themeMode = settingsState.themeMode
            val isSystemDark = isSystemInDarkTheme()

            SettingRow(
                icon = when (themeMode) {
                    ThemeMode.DARK -> Icons.Default.DarkMode
                    ThemeMode.LIGHT -> Icons.Default.LightMode
                    ThemeMode.SYSTEM ->
                        if (isSystemDark) Icons.Default.DarkMode else Icons.Default.LightMode
                },
                title = Strings.darkMode,
                subtitle = when (themeMode) {
                    ThemeMode.DARK -> Strings.darkModeOn
                    ThemeMode.LIGHT -> Strings.darkModeOff
                    ThemeMode.SYSTEM -> Strings.darkModeSystem
                },
                onClick = {
                    val next = when (themeMode) {
                        ThemeMode.DARK -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.SYSTEM
                        ThemeMode.SYSTEM -> ThemeMode.DARK
                    }
                    viewModel.setThemeMode(context, next)
                }
            )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // PERSONALIZATION SECTION
                SectionHeader(title = Strings.sectionPersonalization)


            val colorOptions = remember {
                listOf(
                    Color(0xFF0F8696),
                    Color(0xFF1E88E5),
                    Color(0xFF43A047),
                    Color(0xFF8E24AA),
                    Color(0xFFF4511E),
                    Color(0xFFD81B60)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // New section: Set all colors at once
            Text(
                Strings.quickColorSelection,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ColorRow(
                colors = colorOptions,
                selectedArgb = null, // No individual selection for this row
                onSelect = { color ->
                    viewModel.applyThemeColor(context, color?.toArgb())
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.resetThemeColors(context) }) {
                    Text(Strings.resetToDefault)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Home screen style selector
            Text(
                text = Strings.homeScreenStyle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val homeStyles = listOf(
                "default" to Strings.styleDefault,
                "compact" to Strings.styleCompact
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                homeStyles.forEach { (key, label) ->
                    val isSelected = homeScreenStyle == key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setHomeScreenStyle(context, key)
                        },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // LANGUAGE SECTION
                SectionHeader(
                    title = Strings.sectionLanguageContent,
                    onClick = {
                        languageClickCount++
                        if (languageClickCount >= 10) {
                            val newState = adminManager.toggleAdminMode()
                            languageClickCount = 0
                            android.widget.Toast.makeText(
                                context,
                                if (newState) Strings.adminActivated else Strings.adminDeactivated,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

            SettingsItem(
                icon = Icons.Default.Language,
                title = Strings.appLanguage,
                subtitle = when (currentLanguage) {
                    Strings.Language.CROATIAN -> "Hrvatski"
                    Strings.Language.ENGLISH -> "English"
                    Strings.Language.GERMAN -> "Deutsch"
                },
                onClick = { screenState.showLanguageDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Translate,
                title = Strings.contentLanguagesLabel,
                subtitle = Strings.selectedCount(contentLanguages.size),
                onClick = { contentLanguageDialogController.openContentLanguageDialog() }
            )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // DATA MANAGEMENT SECTION
                SectionHeader(title = Strings.sectionDataManagement)

            SettingsItem(
                icon = Icons.Default.CloudUpload,
                title = Strings.backupToGoogleDrive,
                subtitle = Strings.backupToGoogleDriveHelp,
                onClick = {
                    try {
                        exportLauncher.launch(viewModel.getSuggestedBackupFilename())
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        android.widget.Toast.makeText(
                            context,
                            "Greška: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

            SettingsItem(
                icon = Icons.Default.CloudDownload,
                title = Strings.restoreFromGoogleDrive,
                subtitle = Strings.restoreFromGoogleDriveHelp,
                onClick = {
                    try {
                        importLauncher.launch(arrayOf("application/json"))
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        android.widget.Toast.makeText(
                            context,
                            "Greška: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )


            SettingsItem(
                icon = Icons.Default.Backup,
                title = Strings.backupCopy,
                subtitle = Strings.backupCopyHelp,
                onClick = { screenState.showBackupDialog = true }
            )

            SettingsToggleRow(
                icon = Icons.Default.CloudSync,
                title = Strings.autoGoogleDriveBackup,
                subtitle = if (gDriveAutoEnabled && gDriveAutoUri.isNotBlank()) {
                    Strings.autoBackupEnabled
                } else if (gDriveAutoEnabled) {
                    Strings.autoBackupNoFile
                } else {
                    Strings.autoBackupDisabled
                },
                checked = gDriveAutoEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (gDriveAutoUri.isBlank()) {
                            googleDriveAutoBackupCreateLauncher.launch("youtube_backup_auto.json")
                        } else {
                            viewModel.setGoogleDriveAutoBackup(context, enabled = true)
                        }
                    } else {
                        viewModel.setGoogleDriveAutoBackup(context, enabled = false)
                    }
                }
            )

            // Watch History Toggle
            val watchHistoryEnabled = settingsState.watchHistoryEnabled

            SettingsToggleRow(
                icon = Icons.Default.History,
                title = "Povijest Gledanja",
                subtitle = if (watchHistoryEnabled) {
                    "Automatski sprema gledane videe"
                } else {
                    "Onemogućeno"
                },
                checked = watchHistoryEnabled,
                onCheckedChange = { enabled ->
                    viewModel.setWatchHistoryEnabled(context, enabled)
                }
            )

            // Resume Playback Toggle
            val resumePlaybackEnabled = settingsState.resumePlaybackEnabled

            SettingsToggleRow(
                icon = Icons.Filled.PlayArrow,
                title = "Nastavi gdje si stao",
                subtitle = if (resumePlaybackEnabled) {
                    "Pamti zadnju sekundu videa"
                } else {
                    "Onemogućeno"
                },
                checked = resumePlaybackEnabled,
                onCheckedChange = { enabled ->
                    viewModel.setResumePlaybackEnabled(context, enabled)
                }
            )

            }

            NotificationsSettingsSection {
                SettingsNotificationsSectionContent(
                    context = context,
                    viewModel = viewModel,
                    settingsState = settingsState,
                    notificationPermissionRequester = notificationPermissionRequester
                )
            }

            GeneralSettingsSection {

            SettingsItem(
                icon = Icons.Default.TrackChanges,
                title = "Habit Tracker",
                subtitle = "Prati dnevne navike",
                onClick = onOpenHabitTracker
            )

            // Disable scroll effects toggle (context = default: disabled)
            val scrollEffectsDisabled = settingsState.scrollEffectsDisabled

            SettingsToggleRow(
                icon = Icons.Default.MotionPhotosOff,
                title = "Skrol efekti",
                checked = !scrollEffectsDisabled,
                onCheckedChange = { enabled ->
                    viewModel.setDisableScrollEffects(context, enabled)
                }
            )

            val lockFeaturedHome = settingsState.lockFeaturedHome

            SettingsToggleRow(
                icon = Icons.Default.Info,
                title = "Skrolanje popularnih videa",
                subtitle = if (!lockFeaturedHome) {
                    "Popularni videi se skrolaju s listom"
                } else {
                    "Popularni videi ostaju na vrhu"
                },
                checked = !lockFeaturedHome,
                onCheckedChange = { enabled ->
                    viewModel.setLockFeaturedHome(context, !enabled)
                }
            )





            if (isAdminMode) {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = Strings.clearCache,
                    subtitle = Strings.cacheOccupied + settingsFormatFileSize(cacheSize),
                    onClick = { screenState.showClearCacheDialog = true }
                )
            }

            // ABOUT SECTION
            SettingsAboutSectionContent(
                context = context,
                appUsageTimeMs = appUsageTimeMs,
                onShowAboutDialog = { screenState.showAboutDialog = true },
                onOpenHabitTracker = onOpenHabitTracker,
                onOpenRunningFeature = onOpenRunningFeature,
                onOpenKuiverFeature = onOpenKuiverFeature
            )
            }

            // ADMIN ONLY
            if (isAdminMode) {
                AdminSettingsSection {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🔧 Admin Opcije",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "📦 Version Code",
                    subtitle = BuildConfig.VERSION_CODE.toString(),
                    onClick = { /* display only */ }
                )
                }

                val canShowDebugItems = BuildConfig.DEBUG || screenState.debugUnlocked
                DebugSettingsSection {
                    if (!BuildConfig.DEBUG && !screenState.debugUnlocked) {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "🔒 Debug Alati",
                            subtitle = "Unesite lozinku za prikaz debug opcija",
                            onClick = {
                                screenState.debugPassword = ""
                                screenState.debugUnlockError = null
                                screenState.showDebugUnlockDialog = true
                            }
                        )
                    }

                    if (!BuildConfig.DEBUG && screenState.debugUnlocked) {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "🔓 Debug otključan",
                            subtitle = "Deaktiviraj debug opcije",
                            onClick = {
                                screenState.debugUnlocked = false
                                viewModel.setDebugUnlocked(context, false)
                                LogConfig.ENABLE_LOGS = false
                                Logger.isEnabled = false
                                Logger.minLevel = LogLevel.INFO
                            }
                        )
                    }

                    // Debug tools
                    if (canShowDebugItems) {
                        var debugInstallId by remember { mutableStateOf<String?>(null) }
                        var debugUserToken by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(Unit) {
                            debugInstallId = runCatching { InstallPrefs.getInstallId(context) }.getOrNull()
                            debugUserToken = runCatching { userTokenManager.getCachedUserToken() }.getOrNull()
                        }

                        var isBaseUrlRefreshing by remember { mutableStateOf(false) }
                        var baseUrlRefreshResult by remember { mutableStateOf<String?>(null) }

                        DebugHeader(title = "ℹ️ Info")
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "App verzija",
                            subtitle = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            onClick = { /* display only */ }
                        )
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Build",
                            subtitle = "Debug: ${BuildConfig.DEBUG} • Logs: ${BuildConfig.ENABLE_LOGGING}",
                            onClick = { /* display only */ }
                        )
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Device",
                            subtitle = "${Build.MANUFACTURER} ${Build.MODEL} • SDK ${Build.VERSION.SDK_INT}",
                            onClick = { /* display only */ }
                        )
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Locale/Timezone",
                            subtitle = "${Locale.getDefault()} • ${java.util.TimeZone.getDefault().id}",
                            onClick = { /* display only */ }
                        )
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Uptime",
                            subtitle = "${SystemClock.uptimeMillis() / 1000}s",
                            onClick = { /* display only */ }
                        )
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "User Token",
                            subtitle = debugUserToken?.takeIf { it.isNotBlank() }?.let { "${it.take(10)}..." }
                                ?: "nije postavljen",
                            onClick = { /* display only */ }
                        )
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Install ID",
                            subtitle = debugInstallId?.takeIf { it.isNotBlank() }?.let { "${it.take(10)}..." }
                                ?: "nije dostupno",
                            onClick = { /* display only */ }
                        )
                        SettingsItem(
                            icon = Icons.Default.Share,
                            title = "📋 Kopiraj debug info",
                            subtitle = "Kopira ključne informacije za support",
                            onClick = {
                                val text = buildString {
                                    appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                                    appendLine("Debug: ${BuildConfig.DEBUG} | Logs: ${BuildConfig.ENABLE_LOGGING}")
                                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
                                    appendLine("Locale: ${Locale.getDefault()} | TZ: ${java.util.TimeZone.getDefault().id}")
                                    appendLine("BaseURL Active: ${com.youtube.rating.shared.BASE_URL}")
                                    appendLine("BaseURL BuildConfig: ${BuildConfig.BASE_URL}")
                                    appendLine("UserToken: ${debugUserToken ?: "n/a"}")
                                    appendLine("InstallId: ${debugInstallId ?: "n/a"}")
                                }
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(ClipData.newPlainText("debug_info", text))
                                android.widget.Toast.makeText(
                                    context,
                                    "Debug info kopiran",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        DebugHeader(title = "🌐 Backend")
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Base URL",
                            subtitle = "Aktivni: ${com.youtube.rating.shared.BASE_URL}\nBuildConfig: ${BuildConfig.BASE_URL}",
                            onClick = {
                                val text = "Active: ${com.youtube.rating.shared.BASE_URL}\nBuildConfig: ${BuildConfig.BASE_URL}"
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(ClipData.newPlainText("base_url_debug", text))
                                android.widget.Toast.makeText(
                                    context,
                                    "Base URL kopiran",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.CloudSync,
                            title = "Refresh Base URL",
                            subtitle = baseUrlRefreshResult ?: "Povuci novi base URL sa servera",
                            onClick = {
                                if (isBaseUrlRefreshing) return@SettingsItem
                                isBaseUrlRefreshing = true
                                baseUrlRefreshResult = "Osvježavam..."
                                scope.launch {
                                    val ok = BaseUrlProvider.refreshFromServer(context)
                                    baseUrlRefreshResult = if (ok) {
                                        "Ažurirano: ${com.youtube.rating.shared.BASE_URL}"
                                    } else {
                                        "Nije bilo promjena ili greška"
                                    }
                                    isBaseUrlRefreshing = false
                                }
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Cloud,
                            title = "Otvori backend",
                            subtitle = com.youtube.rating.shared.BASE_URL,
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "${com.youtube.rating.shared.BASE_URL}".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Cloud,
                            title = "Health check",
                            subtitle = "${com.youtube.rating.shared.BASE_URL}/api/health.php",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "${com.youtube.rating.shared.BASE_URL}/api/health.php".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.CloudSync,
                            title = "🧪 Debug: Psalm sync",
                            subtitle = psalmSyncStatus ?: "Pošalji označene psalme na backend",
                            onClick = {
                                viewModel.clearPsalmSyncStatus()
                                viewModel.debugSyncPsalmHighlights(context)
                            }
                        )

                        DebugHeader(title = "🔗 Links")
                        SettingsItem(
                            icon = Icons.Default.Cloud,
                            title = "Admin panel",
                            subtitle = "${com.youtube.rating.shared.BASE_URL}/adminPanel/",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "${com.youtube.rating.shared.BASE_URL}/adminPanel/".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Cloud,
                            title = "📊 Pregledaj Crasheve",
                            subtitle = "Otvori backend admin panel za pregled crash izvještaja",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "${com.youtube.rating.shared.BASE_URL}/adminPanel/crash-logs.php".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.BugReport,
                            title = "🐛 Pregledaj Bug Izvještaje",
                            subtitle = "Otvori backend admin panel za pregled bug izvještaja",
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "${com.youtube.rating.shared.BASE_URL}/adminPanel/bug-reports.php".toUri()
                                )
                                context.startActivity(intent)
                            }
                        )

                        DebugHeader(title = "🧪 Testovi")
                        SettingsItem(
                            icon = Icons.Default.BugReport,
                            title = "💥 Testiraj Crash Report",
                            subtitle = "Izazovi kontrolirani crash za testiranje Kotzilla analytics",
                            onClick = { screenState.showCrashConfirmDialog = true }
                        )
                        if (BuildConfig.DEBUG) {
                            SettingsItem(
                                icon = Icons.Default.Flag,
                                title = "🚩 Debug: Prijavi video",
                                subtitle = "Pošalji report na backend",
                                onClick = {
                                    viewModel.clearReportStatus()
                                    if (screenState.reportVideoId.isBlank()) screenState.reportVideoId = ""
                                    screenState.showReportVideoDialog = true
                                }
                            )
                        }
                        SettingsItem(
                            icon = Icons.Default.CloudSync,
                            title = "🧪 Upload server ping",
                            subtitle = if (isUploadServerTesting) {
                                "Testiram..."
                            } else {
                                uploadServerTestResult ?: "Provjeri dostupnost upload endpoint-a"
                            },
                            onClick = {
                                if (isUploadServerTesting) return@SettingsItem
                                viewModel.testUploadServerPing()
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.CloudUpload,
                            title = "🧪 Upload POST test",
                            subtitle = "Pošalji multipart bez fajla (očekivano 4xx ako endpoint radi)",
                            onClick = {
                                if (isUploadServerTesting) return@SettingsItem
                                viewModel.testUploadServerPost()
                            }
                        )

                        DebugHeader(title = "🛠️ Alati")
                        if (!BuildConfig.DEBUG) {
                            val releaseLogsEnabled by AdminPrefs.releaseLogsEnabledFlow(context)
                                .collectAsStateWithLifecycle(initialValue = false)

	                        Row(
	                            modifier = Modifier
	                                .fillMaxWidth()
	                                .padding(horizontal = 16.dp, vertical = 12.dp),
	                            verticalAlignment = Alignment.CenterVertically
	                        ) {
	                            Icon(
	                                imageVector = Icons.Default.Info,
	                                contentDescription = null,
	                                tint = MaterialTheme.colorScheme.primary,
	                                modifier = Modifier.size(24.dp)
	                            )

	                            Spacer(modifier = Modifier.width(16.dp))

	                            Column(modifier = Modifier.weight(1f)) {
	                                Text(
	                                    text = "Logovi u Logcat-u",
	                                    style = MaterialTheme.typography.bodyLarge,
	                                    fontWeight = FontWeight.Medium
	                                )
	                                Text(
	                                    text = if (releaseLogsEnabled) "Uključeno (release)" else "Isključeno (release)",
	                                    style = MaterialTheme.typography.bodySmall,
	                                    color = MaterialTheme.colorScheme.onSurfaceVariant
	                                )
	                            }

	                            Switch(
	                                checked = releaseLogsEnabled,
	                                onCheckedChange = { enabled ->
	                                    viewModel.setReleaseLogsEnabled(context, enabled)
	                                    LogConfig.ENABLE_LOGS = enabled
	                                    Logger.isEnabled = enabled
	                                    Logger.minLevel = if (enabled) LogLevel.DEBUG else LogLevel.INFO
	                                }
	                            )
	                        }
	                    }

                    SettingsItem(
                        icon = Icons.Default.Cloud,
                        title = "📊 Pregledaj Crasheve",
                        subtitle = "Otvori backend admin panel za pregled crash izvještaja",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "${com.youtube.rating.shared.BASE_URL}/adminPanel/crash-logs.php".toUri()
                            )
                            context.startActivity(intent)
                        }
                    )

                    SettingsItem(
                        icon = Icons.Default.BugReport,
                        title = "🐛 Pregledaj Bug Izvještaje",
                        subtitle = "Otvori backend admin panel za pregled bug izvještaja",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "${com.youtube.rating.shared.BASE_URL}/adminPanel/bug-reports.php".toUri()
                            )
                            context.startActivity(intent)
                        }
                    )

                    // Debug tools (advanced)
                    if (canShowDebugItems) {

                        SettingsItem(
                            icon = Icons.Default.Speed,
                            title = "⚡ StrictMode",
                            subtitle = "Detektuj performance kršenja (${if (com.youtube.rating.android.utils.StrictModeManager.isEnabled()) "UKLJUČEN" else "ISKLJUČEN"})",
                            onClick = {
                                if (com.youtube.rating.android.utils.StrictModeManager.isEnabled()) {
                                    com.youtube.rating.android.utils.StrictModeManager.disable()
                                    android.widget.Toast.makeText(
                                        context,
                                        "StrictMode isključen",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    com.youtube.rating.android.utils.StrictModeManager.enable()
                                    android.widget.Toast.makeText(
                                        context,
                                        "StrictMode uključen - ekran će treptati pri kršenju",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.NotificationsActive,
                            title = "🔔 Test Notifications",
                            subtitle = "Pošalji test obavijesti odmah (post + evanđelje)",
                            onClick = {
                                when (debugNotificationSender.sendDebugTestNotifications(context)) {
                                    DebugNotificationSender.Result.Success -> {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Test obavijesti poslane odmah",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    DebugNotificationSender.Result.MissingPermission -> {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Omogući Notifications dozvolu za test obavijesti",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    is DebugNotificationSender.Result.Failed -> {
                                        android.widget.Toast.makeText(
                                            context,
                                            Strings.genericError("Debug test failed"),
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Memory,
                            title = "💾 Memory Profiler",
                            subtitle = "Prati memory usage i GC događaje (${if (com.youtube.rating.android.utils.MemoryProfiler.isMonitoring()) "RADI" else "STOPIRAN"})",
                            onClick = {
                                if (com.youtube.rating.android.utils.MemoryProfiler.isMonitoring()) {
                                    com.youtube.rating.android.utils.MemoryProfiler.stopMonitoring()
                                    android.widget.Toast.makeText(
                                        context,
                                        "Memory profiling stopiran",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    com.youtube.rating.android.utils.MemoryProfiler.startMonitoring()
                                    android.widget.Toast.makeText(
                                        context,
                                        "Memory profiling pokrenut",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Search,
                            title = "🗓️ Svetac dana (datum)",
                            subtitle = "Dohvati sveca za proizvoljan datum (YYYY-MM-DD)",
                            onClick = {
                                screenState.saintLookupDate = LocalDate.now().toString()
                                viewModel.resetSaintLookup(screenState.saintLookupDate)
                                screenState.showSaintLookupDialog = true
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Speed,
                            title = "📈 FPS Monitor",
                            subtitle = "Real-time FPS tracking (${if (com.youtube.rating.android.utils.FpsMonitor.isMonitoring()) "RADI" else "STOPIRAN"})",
                            onClick = {
                                if (com.youtube.rating.android.utils.FpsMonitor.isMonitoring()) {
                                    val stats =
                                        com.youtube.rating.android.utils.FpsMonitor.getFormattedStats()
                                    com.youtube.rating.android.utils.FpsMonitor.stopMonitoring()
                                    android.widget.Toast.makeText(
                                        context,
                                        "FPS monitor stopiran\n$stats",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    com.youtube.rating.android.utils.FpsMonitor.startMonitoring()
                                    android.widget.Toast.makeText(
                                        context,
                                        "FPS monitoring pokrenut",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Upload,
                            title = "🌐 Server zahtjevi",
                            subtitle = "Pregled svega poslanog na server (${networkDebugEntries.size})",
                            onClick = { screenState.showNetworkDebugDialog = true }
                        )

                        // User token is the only "identity" the app has (no login).
                        // Keep it stable in release builds so ratings and personal data stay attached.
                        if (BuildConfig.DEBUG) {
                            SettingsItem(
                                icon = Icons.Default.Person,
                                title = "🆔 Generiši novi User ID",
                                subtitle = if (isGeneratingUserId) "Generišem..." else "Dobij novi anonimni user token sa servera",
                                onClick = {
                                    viewModel.generateNewUserId()
                                }
                            )
                        }

                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = "🚪 Deaktiviraj Admin Mod",
                            subtitle = "Isključi admin opcije i vrati se u normalan mod",
                            onClick = {
                                com.youtube.rating.android.utils.StrictModeManager.disable()
                                com.youtube.rating.android.utils.MemoryProfiler.stopMonitoring()
                                com.youtube.rating.android.utils.FpsMonitor.stopMonitoring()
                                adminManager.setAdminMode(false)
                                android.widget.Toast.makeText(
                                    context,
                                    "Admin mod deaktiviran - svi alati stopiraju",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        // 🔑 USER TOKEN ITEM (debug-only)
                        if (BuildConfig.DEBUG) {
                            SettingsItem(
                                icon = Icons.Default.Person,
                                title = "🔑 User Token",
                                subtitle = if (userTokenManager.hasUserToken()) {
                                    "Token postavljen (${userTokenManager.getCachedUserToken()?.take(8)}...)"
                                } else {
                                    "Nije postavljen - potreban za server funkcionalnosti"
                                },
                                onClick = { screenState.showUserTokenDialog = true }
                            )
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Dialogs
            if (screenState.showLanguageDialog) {
                LanguageSelectionDialog(
                    currentLanguage = currentLanguage,
                    onDismiss = { screenState.showLanguageDialog = false },
                    onLanguageSelected = { language ->
                        scope.launch {
                            languageManager.saveLanguage(language)
                            screenState.showLanguageDialog = false
                            (context as? android.app.Activity)?.recreate()
                        }
                    }
                )
            }

            if (screenState.showNetworkDebugDialog) {
                AlertDialog(
                    onDismissRequest = { screenState.showNetworkDebugDialog = false },
                    title = { Text("Server zahtjevi (debug)") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        ) {
                            if (networkDebugEntries.isEmpty()) {
                                RatingEmptyState(title = "Nema logova još.")
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    networkDebugEntries.takeLast(250).forEach { line ->
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    val text = networkDebugEntries.joinToString("\n")
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Server zahtjevi debug")
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Podijeli debug log"))
                                }
                            ) { Text(Strings.share) }
                            TextButton(onClick = { NetworkDebugStore.clear() }) { Text(Strings.clear) }
                            TextButton(onClick = { screenState.showNetworkDebugDialog = false }) { Text(Strings.close) }
                        }
                    },
                    dismissButton = {}
                )
            }

            if (screenState.showAboutDialog) {
                AboutDialog(onDismiss = { screenState.showAboutDialog = false })
            }

            if (screenState.showQuickSearchDialog) {
                AlertDialog(
                    onDismissRequest = { screenState.showQuickSearchDialog = false },
                    title = { Text("Brza pretraga") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = screenState.quickSearchInput,
                                onValueChange = { screenState.quickSearchInput = it },
                                placeholder = { Text("Unesite pojam (npr. ivancic)") },
                                singleLine = true
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val term = screenState.quickSearchInput.trim()
                                        if (term.isNotBlank() && screenState.localQuickSearchTerms.none { it.equals(term, true) }) {
                                            screenState.localQuickSearchTerms = screenState.localQuickSearchTerms + term
                                            screenState.quickSearchInput = ""
                                        }
                                    }
                                ) { Text("Dodaj") }
                                Text(
                                    text = "Pojmovi se prikazuju kao chipovi na Home."
                                )
                            }
                            if (screenState.localQuickSearchTerms.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    maxLines = 4
                                ) {
                                    screenState.localQuickSearchTerms.forEach { term ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                screenState.localQuickSearchTerms =
                                                    screenState.localQuickSearchTerms.filterNot { it == term }
                                            },
                                            label = { Text(text = "$term ×", maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.saveQuickSearchTerms(context, screenState.localQuickSearchTerms)
                                screenState.showQuickSearchDialog = false
                            }
                        ) { Text(Strings.save) }
                    },
                    dismissButton = {
                        TextButton(onClick = { screenState.showQuickSearchDialog = false }) { Text(Strings.cancel) }
                    }
                )
            }

            if (screenState.showUserTokenDialog) {
                UserTokenDialog(
                    userTokenManager = userTokenManager,
                    apiClient = apiClient,
                    onDismiss = { screenState.showUserTokenDialog = false }
                )
            }

            if (screenState.showCrashConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { screenState.showCrashConfirmDialog = false },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = { Text("⚠️ Test Crash Report", color = MaterialTheme.colorScheme.error) },
                    text = {
                        Column {
                            Text(Strings.crashTestWarning)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Crash će biti poslan na Kotzilla Analytics i backend server.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(Strings.continueQuestion, fontWeight = FontWeight.Normal)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                screenState.showCrashConfirmDialog = false
                                scope.launch {
                                    kotlinx.coroutines.delay(300)
                                    // Don't crash the app for testing. Send a synthetic exception to Sentry instead.
                                    com.youtube.rating.android.sentry.SentryLogger.captureException(
                                        RuntimeException("TEST_CRASH (synthetic) - Admin triggered at ${System.currentTimeMillis()}"),
                                        tags = mapOf("type" to "synthetic_test_crash")
                                    )
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("💥 Izazovi Crash") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            screenState.showCrashConfirmDialog = false
                        }) { Text(Strings.cancel) }
                    }
                )
            }

            if (screenState.showDebugUnlockDialog) {
                AlertDialog(
                    onDismissRequest = {
                        screenState.showDebugUnlockDialog = false
                        screenState.debugUnlockError = null
                    },
                    title = { Text("🔒 Debug lozinka") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = screenState.debugPassword,
                                onValueChange = { screenState.debugPassword = it },
                                label = { Text(Strings.password) },
                                singleLine = true
                            )
                            screenState.debugUnlockError?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val expected = debugConfigProvider.debugUnlockPassword
                            if (expected.isNotBlank() && screenState.debugPassword == expected) {
                                screenState.debugUnlocked = true
                                viewModel.setDebugUnlocked(context, true)
                                LogConfig.ENABLE_LOGS = true
                                Logger.isEnabled = true
                                Logger.minLevel = LogLevel.DEBUG
                                screenState.showDebugUnlockDialog = false
                                screenState.debugUnlockError = null
                            } else {
                                screenState.debugUnlockError = "Pogrešna lozinka"
                            }
                        }) {
                            Text(Strings.confirm)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            screenState.showDebugUnlockDialog = false
                            screenState.debugUnlockError = null
                        }) {
                            Text(Strings.dismiss)
                        }
                    }
                )
            }

            if (screenState.showClearCacheDialog) {
                AlertDialog(
                    onDismissRequest = { screenState.showClearCacheDialog = false },
                    icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                    title = { Text(Strings.clearCacheQuestion) },
                    text = {
                        Text(
                            "Ovo će obrisati sve privremene datoteke (${settingsFormatFileSize(bytes = cacheSize)}). " +
                                    "Aplikacija može biti sporija dok se ponovo ne popuni predmemorija."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearCache(context) {
                                    screenState.showClearCacheDialog = false
                                }
                            }
                        ) { Text(Strings.clearCache) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            screenState.showClearCacheDialog = false
                        }) { Text(Strings.cancel) }
                    }
                )
            }

            if (screenState.showBackupDialog) {
                BackupDialog(
                    viewModel = viewModel,
                    onDismiss = { screenState.showBackupDialog = false }
                )
            }

            if (screenState.showOfflineBibleDialog) {
                OfflineBibleDialog(
                    currentBook = offlineBibleBook,
                    onBookSelected = { bookId ->
                        viewModel.setOfflineBibleBook(context, bookId)
                        screenState.showOfflineBibleDialog = false
                        android.widget.Toast.makeText(
                            context,
                            if (bookId != null) "✅ Knjiga spremljena za offline čitanje" else "❌ Offline knjiga uklonjena",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismiss = { screenState.showOfflineBibleDialog = false }
                )
            }
        }

        if (BuildConfig.DEBUG && screenState.showReportVideoDialog) {
            AlertDialog(
                onDismissRequest = { screenState.showReportVideoDialog = false },
                title = { Text("Debug: Prijavi video") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = screenState.reportVideoId,
                            onValueChange = { screenState.reportVideoId = it.trim() },
                            label = { Text("Video ID") },
                            placeholder = { Text("npr. dQw4w9WgXcQ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = screenState.reportReason,
                            onValueChange = { screenState.reportReason = it },
                            label = { Text("Razlog") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        reportStatus?.let { msg ->
                            Text(
                                text = msg,
                                color = if (msg.startsWith("✅")) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    PrimaryActionButton(
                        text = "Pošalji",
                        onClick = {
                            viewModel.submitVideoReport(screenState.reportVideoId, screenState.reportReason)
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { screenState.showReportVideoDialog = false }) { Text(Strings.close) }
                }
            )
        }

        if (screenState.showSaintLookupDialog) {
            AlertDialog(
                onDismissRequest = { screenState.showSaintLookupDialog = false },
                title = { Text("Debug: Svetac dana po datumu") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = screenState.saintLookupDate,
                            onValueChange = { screenState.saintLookupDate = it.trim() },
                            label = { Text("Datum") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        saintLookupStatus?.let { msg ->
                            Text(
                                text = msg,
                                color = if (msg.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        saintLookupResult?.let { saint ->
                            Text(
                                text = saint.title ?: "(bez naslova)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Normal
                            )
                            saint.date?.let {
                                Text(text = "Datum: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    PrimaryActionButton(
                        text = "Dohvati",
                        onClick = {
                            viewModel.lookupSaintByDate(screenState.saintLookupDate)
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { screenState.showSaintLookupDialog = false }) { Text(Strings.close) }
                }
            )
        }
    }
}
