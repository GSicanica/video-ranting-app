package com.youtube.rating.android.ui.screens

import com.youtube.rating.core.coroutines.ioDispatcher

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.designsystem.theme.spacing
import com.youtube.rating.shared.utils.LogConfig
import com.youtube.rating.shared.utils.Logger
import com.youtube.rating.shared.utils.LogLevel
import com.youtube.rating.android.network.OkHttpClients
import com.youtube.rating.shared.STREAM_UPLOAD_ENDPOINT
import com.youtube.rating.shared.debug.NetworkDebugStore
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.Request
import com.youtube.rating.shared.models.SaintOfDayResponse
import androidx.compose.foundation.interaction.MutableInteractionSource

@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = MaterialTheme.spacing
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xs),
            content = content
        )
    }
}

    @Composable
    fun ColorRow(
        colors: List<Color>,
        selectedArgb: Int?,
        onSelect: (Color?) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { color ->
                val selected = selectedArgb == color.toArgb()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .clickable { onSelect(color) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onSelect(null) }) {
                Text(Strings.noColor)
            }
        }
    }


    @Composable
    fun SectionHeader(title: String, onClick: (() -> Unit)? = null) {
        val spacing = MaterialTheme.spacing
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = spacing.lg, bottom = spacing.sm, start = spacing.md)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            onClick = onClick,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    } else Modifier
                )
        )
    }

    @Composable
    fun GeneralSettingsSection(content: @Composable ColumnScope.() -> Unit) {
        SettingsSectionCard(content = content)
    }

    @Composable
    fun NotificationsSettingsSection(content: @Composable ColumnScope.() -> Unit) {
        SettingsSectionCard(content = content)
    }

    @Composable
    fun AdminSettingsSection(content: @Composable ColumnScope.() -> Unit) {
        SettingsSectionCard(content = content)
    }

    @Composable
    fun DebugSettingsSection(content: @Composable ColumnScope.() -> Unit) {
        SettingsSectionCard(content = content)
    }

    @Composable
    fun SettingsToggleRow(
        icon: ImageVector,
        title: String,
        subtitle: String? = null,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        onRowClick: (() -> Unit)? = null
    ) {
        ListItem(
            headlineContent = {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = subtitle?.takeIf { it.isNotBlank() }?.let {
                { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onRowClick ?: { onCheckedChange(!checked) },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
        )
    }

    @Composable
    fun SettingsItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        ListItem(
            headlineContent = {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        )
    }

    @Composable
    fun SettingRow(
        icon: ImageVector,
        title: String,
        subtitle: String? = null,
        onClick: () -> Unit,
        trailing: (@Composable () -> Unit)? = null
    ) {
        ListItem(
            headlineContent = {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            },
            supportingContent = subtitle?.takeIf { it.isNotBlank() }?.let {
                { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = trailing ?: {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        )
    }

    @Composable
    fun PrimaryActionButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            Text(text)
        }
    }

    @Composable
    fun AboutDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(Strings.aboutApp) },
            text = {
                Column {
                    Text(Strings.appName, fontWeight = FontWeight.Normal)
                    Text(
                        "Verzija ${BuildConfig.VERSION_NAME}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(Strings.appDescription)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } }
        )
    }

    /* =======================
       BackupDialog + dialogs
       (copy 1:1 from your existing file)
       ======================= */

    @Composable
    fun BackupDialog(
        viewModel: com.youtube.rating.android.viewmodel.SettingsViewModel,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
        val backupError by viewModel.backupError.collectAsStateWithLifecycle()
        val autoBackups by viewModel.autoBackupInfo.collectAsStateWithLifecycle()
        var showConfirmation by remember { mutableStateOf(false) }
        var showImportConfirmation by remember { mutableStateOf(false) }

        val latestBackup = remember(autoBackups) { autoBackups.firstOrNull() }
        LaunchedEffect(Unit) {
            viewModel.loadAutoBackupInfo()
        }
        val backupStatus = backupMessage ?: backupError.orEmpty()

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                scope.launch {
                    viewModel.clearMessages()
                    try {
                        val backupContent = withContext(ioDispatcher) {
                            context.contentResolver.openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { reader -> reader.readText() }
                        }
                        if (backupContent != null) {
                            viewModel.importBackupFromExternal(backupContent)
                        } else {
                            viewModel.setBackupError("❌ Ne mogu pročitati datoteku")
                        }
                    } catch (e: Exception) {
                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                        viewModel.setBackupError("❌ Greška: ${e.message}")
                    }
                }
            }
        }

        if (showImportConfirmation) {
            AlertDialog(
                onDismissRequest = { showImportConfirmation = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text(Strings.importBackupTitle) },
                text = { Text(Strings.importBackupWarning) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportConfirmation = false
                            filePickerLauncher.launch("application/json")
                        }
                    ) { Text(Strings.yesImport) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showImportConfirmation = false
                    }) { Text(Strings.cancel) }
                }
            )
        }

        if (showConfirmation) {
            AlertDialog(
                onDismissRequest = { showConfirmation = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text(Strings.loadConfirmTitle) },
                text = {
                    Column {
                        Text(Strings.loadBackupWarning)
                        if (latestBackup != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Zadnja kopija: ${latestBackup.getFormattedDate()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(Strings.areYouSure, fontWeight = FontWeight.Normal)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmation = false
                            if (latestBackup != null) {
                                scope.launch {
                                    viewModel.clearMessages()
                                    try {
                                        viewModel.restoreLatestBackup()
                                    } catch (e: Exception) {
                                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                                        viewModel.setBackupError("❌ Greška: ${e.message}")
                                    }
                                }
                            }
                        }
                    ) { Text(Strings.yesLoad) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showConfirmation = false
                    }) { Text(Strings.cancel) }
                }
            )
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Backup, contentDescription = null) },
            title = { Text(Strings.backupCopyTitle) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (latestBackup != null) {
                        Text(Strings.lastAutoBackup, fontWeight = FontWeight.Normal)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        latestBackup.getFormattedDate(),
                                        fontWeight = FontWeight.Normal
                                    )
                                    Text(
                                        latestBackup.getFormattedSize(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.History, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (isLoading) "Učitavanje..." else "Učitaj Zadnje Podatke")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    viewModel.clearMessages()
                                    try {
                                        val backupContent =
                                            viewModel.getLatestBackupForExport()
                                        if (backupContent != null) {
                                            val tempFile = File(
                                                context.cacheDir,
                                                "backup_export_${System.currentTimeMillis()}.json"
                                            )
                                            withContext(ioDispatcher) {
                                                tempFile.writeText(backupContent)
                                            }

                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                val uri =
                                                    androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.provider",
                                                        tempFile
                                                    )
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(
                                                    shareIntent,
                                                    "Izvezi backup"
                                                )
                                            )
                                            viewModel.setBackupMessage("✅ Backup spreman za dijeljenje")
                                        } else {
                                            viewModel.setBackupError("❌ Nema backup-a za izvoz")
                                        }
                                    } catch (e: Exception) {
                                        com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                                        viewModel.setBackupError("❌ Greška: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.exportBackup)
                        }
                    } else {
                        Text(
                            "Nema dostupnih sigurnosnih kopija.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "💡 Automatske kopije se kreiraju nakon svake izmjene podataka.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showImportConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.importBackupFromFile)
                    }

                    if (backupStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (backupStatus.startsWith("✅"))
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(text = backupStatus, modifier = Modifier.padding(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ℹ️ Zadnjih 10 kopija se čuva automatski. Kopije se kreiraju pri svakoj izmjeni podataka.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } }
        )
    }

    @Composable
    fun ContentLanguageDialog(
        selectedLanguages: Set<Strings.Language>,
        onDismiss: () -> Unit,
        onConfirm: (Set<Strings.Language>) -> Unit
    ) {
        var selected by remember { mutableStateOf(selectedLanguages) }
        val languages = Strings.Language.entries

        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Translate, contentDescription = null) },
            title = { Text(Strings.contentLanguagesTitle) },
            text = {
                Column {
                    Text(
                        text = "Odaberi jezike videa koje želiš vidjeti:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    languages.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected =
                                        if (language in selected) selected - language else selected + language
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = language in selected,
                                onCheckedChange = { checked ->
                                    selected =
                                        if (checked) selected + language else selected - language
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(language.getDisplayName())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(selected) },
                    enabled = selected.isNotEmpty()
                ) { Text(Strings.confirm) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
        )
    }

    @Composable
    fun LanguageSelectionDialog(
        currentLanguage: Strings.Language,
        onDismiss: () -> Unit,
        onLanguageSelected: (Strings.Language) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Language, contentDescription = null) },
            title = { Text(Strings.appLanguageTitle) },
            text = {
                Column {
                    Strings.Language.entries.forEach { language ->
                        LanguageOption(
                            label = language.getDisplayName(),
                            language = language,
                            currentLanguage = currentLanguage,
                            onClick = { onLanguageSelected(language) }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(Strings.close) } }
        )
    }

    @Composable
    fun LanguageOption(
        label: String,
        language: Strings.Language,
        currentLanguage: Strings.Language,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = language == currentLanguage, onClick = onClick)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label)
        }
    }

    fun Strings.Language.getDisplayName(): String = when (this) {
        Strings.Language.CROATIAN -> "Hrvatski"
        Strings.Language.ENGLISH -> "English"
        Strings.Language.GERMAN -> "Deutsch"
    }

    // Helpers
    fun settingsCalculateDirectorySize(directory: File): Long {
        var size: Long = 0
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) settingsCalculateDirectorySize(directory = file) else file.length()
            }
        }
        return size
    }

    fun settingsFormatUsageTime(milliseconds: Long): String {
        if (milliseconds == 0L) return "Još niste koristili aplikaciju"

        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun settingsFormatBookTitle(slug: String): String {
        return slug
            .split('-')
            .joinToString(" ") { part ->
                if (part.isBlank()) part else part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                }
            }
            .trim()
    }

    fun settingsFormatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }

    fun settingsFormatHourMinute(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    /**
     * Bug Report Dialog
     */
    @Composable
    fun BugReportDialog(
        onDismiss: () -> Unit,
        onSubmit: (title: String, description: String, email: String, priority: String) -> Unit
    ) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("medium") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🐛 Prijavi Bug (u izradi)")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Pomozi nam poboljšati aplikaciju prijavljivanjem bugova!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(Strings.feedbackTitleLabel) },
                        placeholder = { Text(Strings.feedbackTitlePlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = title.isBlank()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(Strings.feedbackDetailLabel) },
                        placeholder = { Text(Strings.feedbackDetailPlaceholder) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 6,
                        isError = description.isBlank()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(Strings.feedbackEmailLabel) },
                        placeholder = { Text(Strings.feedbackEmailPlaceholder) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Prioritet:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "low" to "Nizak",
                            "medium" to "Srednji",
                            "high" to "Visok",
                            "critical" to "Kritičan"
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = priority == value,
                                onClick = { priority = value },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (priority == value) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "* Automatski će biti prikupljeni tehnički podaci o uređaju za lakše rješavanje problema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            onSubmit(title, description, email, priority)
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank() && description.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.send)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OfflineBibleDialog(
        currentBook: String?,
        onBookSelected: (String?) -> Unit,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        var books by remember { mutableStateOf<List<String>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                val bibleBooks =
                    com.youtube.rating.android.utils.LocalBibleRepository.getBooks(context)
                books = bibleBooks.sortedBy { settingsFormatBookTitle(slug = it) }
                loading = false
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                error = e.message ?: "Greška pri učitavanju knjiga"
                loading = false
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(Strings.selectBookForOffline) },
            text = {
                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Text(
                            text = error ?: "Greška",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Option to disable offline book
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBookSelected(null) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentBook == null,
                                    onClick = { onBookSelected(null) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nijedna knjiga (isključi offline)",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // List of books
                            books.forEach { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onBookSelected(book) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentBook == book,
                                        onClick = { onBookSelected(book) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = settingsFormatBookTitle(slug = book),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = book,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(Strings.close)
                }
            }
        )
    }

    // � USER TOKEN DIALOG
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UserTokenDialog(
        userTokenManager: com.youtube.rating.android.utils.UserTokenManager,
        apiClient: com.youtube.rating.shared.api.RatingApiClient,
        onDismiss: () -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var customToken by remember { mutableStateOf("") }

        val currentToken = userTokenManager.getCachedUserToken()

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "🔑 User Token",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Info text
                    Text(
                        text = "Unesite svoj user token za pristup server funkcionalnostima. Token možete dobiti od administratora aplikacije.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Current token display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Trenutni token:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentToken ?: "Nema tokena",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Token input
                    Text(
                        text = "🔑 Unesite svoj token:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal
                    )

                    OutlinedTextField(
                        value = customToken,
                        onValueChange = { customToken = it },
                        label = { Text(Strings.userTokenLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        )
                    )

                    // Test actions
                    Text(
                        text = "🧪 Test Akcije:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal
                    )

                    val runTestAction: (successMessage: String, block: suspend () -> String?) -> Unit =
                        { successMessage, block ->
                            scope.launch {
                                try {
                                    val overrideMessage = block()
                                    android.widget.Toast.makeText(
                                        context,
                                        overrideMessage ?: successMessage,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                                    android.widget.Toast.makeText(
                                        context,
                                        "❌ Greška: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }

                    val testActions = listOf(
                        "📊 Test Analytics API" to {
                            runTestAction("✅ Analytics API radi!") {
                                apiClient.getUsageAnalytics(customToken.ifEmpty { currentToken ?: "" })
                                null
                            }
                        },
                        "📱 Test Tab Usage" to {
                            runTestAction("✅ API poziv radi - tab usage bi trebao raditi!") {
                                // Simple test - just check if we can make a request
                                apiClient.getUsageAnalytics(customToken.ifEmpty { currentToken ?: "" })
                                null
                            }
                        }
                    )

                    testActions.forEach { (name, action) ->
                        OutlinedButton(
                            onClick = { action() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customToken.isNotEmpty()) {
                            userTokenManager.setUserProvidedToken(customToken)
                            android.widget.Toast.makeText(
                                context,
                                "✅ Token postavljen: ${customToken.take(20)}${if (customToken.length > 20) "..." else ""}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Token nije promijenjen",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        onDismiss()
                    }
                ) {
                    Text(Strings.apply)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Strings.cancel)
                }
            }
        )
    }
