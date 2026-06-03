package com.youtube.rating.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.BuildConfig
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.navigation.Screen
import com.youtube.rating.core.designsystem.theme.spacing

@Composable
internal fun AppMenuDrawerContent(
    currentRoute: String?,
    isAdminMode: Boolean,
    debugUnlocked: Boolean,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onOpenContentLanguages: () -> Unit,
    onOpenBugReport: () -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    val versionName = BuildConfig.VERSION_NAME
    val spacing = MaterialTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = Strings.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${Strings.version} $versionName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = Strings.close)
            }
        }

        HorizontalDivider()

        Text(
            text = Strings.appBrightness,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Brightness6,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Slider(
                value = brightness.coerceIn(0f, 1f),
                onValueChange = onBrightnessChange,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(Strings.tabName("settings")) },
            selected = currentRoute == Screen.Settings.route,
            onClick = {
                onNavigate(Screen.Settings.route)
                onClose()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.TrackChanges, contentDescription = null) },
            label = { Text("Monthly Tracker") },
            selected = currentRoute == Screen.MonthlyHabitTracker.route ||
                currentRoute == Screen.HabitTracker.route,
            onClick = {
                onNavigate(Screen.MonthlyHabitTracker.route)
                onClose()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.NoteAlt, contentDescription = null) },
            label = { Text(Strings.tabName("notes")) },
            selected = currentRoute == Screen.Notes.route,
            onClick = {
                onNavigate(Screen.Notes.route)
                onClose()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text(Strings.watchHistoryTitle) },
            selected = currentRoute == Screen.WatchHistory.route,
            onClick = {
                onNavigate(Screen.WatchHistory.route)
                onClose()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.OfflinePin, contentDescription = null) },
            label = { Text(Strings.offlineVideos) },
            selected = currentRoute == Screen.OfflineVideos.route,
            onClick = {
                onNavigate(Screen.OfflineVideos.route)
                onClose()
            }
        )

        HorizontalDivider()

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            label = { Text(Strings.contentLanguagesLabel) },
            selected = false,
            onClick = {
                onOpenContentLanguages()
                onClose()
            }
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
            label = { Text(Strings.reportBug) },
            selected = false,
            onClick = {
                onOpenBugReport()
                onClose()
            }
        )

        if (debugUnlocked || isAdminMode) {
            Text(
                text = Strings.adminStatus(isOn = isAdminMode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
