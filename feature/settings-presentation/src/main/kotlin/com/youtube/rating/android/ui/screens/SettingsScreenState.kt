package com.youtube.rating.android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDate

internal class SettingsScreenState {
    var showLanguageDialog by mutableStateOf(false)
    var showAboutDialog by mutableStateOf(false)
    var showClearCacheDialog by mutableStateOf(false)
    var showBackupDialog by mutableStateOf(false)
    var showCrashConfirmDialog by mutableStateOf(false)
    var showOfflineBibleDialog by mutableStateOf(false)
    var showUserTokenDialog by mutableStateOf(false)
    var showDebugUnlockDialog by mutableStateOf(false)
    var debugUnlocked by mutableStateOf(false)
    var debugPassword by mutableStateOf("")
    var debugUnlockError by mutableStateOf<String?>(null)
    var showReportVideoDialog by mutableStateOf(false)
    var reportVideoId by mutableStateOf("")
    var reportReason by mutableStateOf("Test report (debug)")
    var showSaintLookupDialog by mutableStateOf(false)
    var saintLookupDate by mutableStateOf(LocalDate.now().toString())
    var showQuickSearchDialog by mutableStateOf(false)
    var showNetworkDebugDialog by mutableStateOf(false)
    var quickSearchInput by mutableStateOf("")
    var localQuickSearchTerms by mutableStateOf<List<String>>(emptyList())
}

@Composable
internal fun rememberSettingsScreenState(): SettingsScreenState = remember {
    SettingsScreenState()
}
