package com.youtube.rating.android.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.android.data.prefs.BiblePlannerPrefs
import com.youtube.rating.android.data.prefs.SaintsPrefs
import com.youtube.rating.android.data.settings.SettingsState
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.notifications.NotificationChannels
import com.youtube.rating.android.viewmodel.SettingsViewModel

@Composable
internal fun SettingsNotificationsSectionContent(
    context: Context,
    viewModel: SettingsViewModel,
    settingsState: SettingsState,
    notificationPermissionRequester: NotificationPermissionRequester,
) {
    val gospelReminderEnabled by BiblePlannerPrefs.reminderEnabledFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val gospelReminderHour by BiblePlannerPrefs.reminderHourFlow(context)
        .collectAsStateWithLifecycle(initialValue = 8)
    val gospelReminderMinute by BiblePlannerPrefs.reminderMinuteFlow(context)
        .collectAsStateWithLifecycle(initialValue = 0)
    val saintOfDayNotificationsEnabled by SaintsPrefs.saintOfDayNotificationsEnabledFlow(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val newVideoNotificationsEnabled = settingsState.newVideoNotificationsEnabled

    SettingsToggleRow(
        icon = Icons.Default.NotificationsActive,
        title = "Evanđelje dana obavijest",
        subtitle = if (gospelReminderEnabled) {
            "Uključeno svaki dan u ${settingsFormatHourMinute(hour = gospelReminderHour, minute = gospelReminderMinute)}"
        } else {
            "Isključeno"
        },
        checked = gospelReminderEnabled,
        onRowClick = {
            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    viewModel.saveBibleReminder(gospelReminderEnabled, selectedHour, selectedMinute)
                },
                gospelReminderHour,
                gospelReminderMinute,
                true
            ).show()
        },
        onCheckedChange = { enabled ->
            if (enabled) {
                val canProceed = notificationPermissionRequester.request {
                    NotificationChannels.ensureChannels(context)
                    viewModel.saveBibleReminder(true, gospelReminderHour, gospelReminderMinute)
                }
                if (!canProceed) return@SettingsToggleRow
                NotificationChannels.ensureChannels(context)
            }
            viewModel.saveBibleReminder(enabled, gospelReminderHour, gospelReminderMinute)
        }
    )

    SettingsToggleRow(
        icon = Icons.Default.NotificationsActive,
        title = "Svetac dana obavijest",
        subtitle = if (saintOfDayNotificationsEnabled) {
            "Uključeno"
        } else {
            "Isključeno"
        },
        checked = saintOfDayNotificationsEnabled,
        onCheckedChange = { enabled ->
            if (enabled) {
                val canProceed = notificationPermissionRequester.request {
                    viewModel.setSaintOfDayNotificationsEnabled(context, true)
                }
                if (!canProceed) return@SettingsToggleRow
            }
            viewModel.setSaintOfDayNotificationsEnabled(context, enabled)
        }
    )

    SettingsToggleRow(
        icon = Icons.Default.NotificationsActive,
        title = "Novi video obavijesti",
        subtitle = if (newVideoNotificationsEnabled) {
            "Uključeno - stiže naslov videa"
        } else {
            "Isključeno"
        },
        checked = newVideoNotificationsEnabled,
        onCheckedChange = { enabled ->
            if (enabled) {
                val canProceed = notificationPermissionRequester.request {
                    NotificationChannels.ensureChannels(context)
                    viewModel.setNewVideoNotificationsEnabled(context, true)
                }
                if (!canProceed) return@SettingsToggleRow
                NotificationChannels.ensureChannels(context)
            }
            viewModel.setNewVideoNotificationsEnabled(context, enabled)
        }
    )
}

@Composable
internal fun SettingsAboutSectionContent(
    context: Context,
    appUsageTimeMs: Long,
    onShowAboutDialog: () -> Unit,
    onOpenHabitTracker: () -> Unit = {},
    onOpenRunningFeature: () -> Unit = {},
    onOpenKuiverFeature: () -> Unit = {},
) {
    val stripeDonationUrl = "https://donate.stripe.com/3cI7sK8vcdBC6871e17ss00"

    SectionHeader(title = Strings.sectionAbout)

    SettingsItem(
        icon = Icons.Default.PrivacyTip,
        title = Strings.privacyPolicy,
        subtitle = Strings.privacyPolicyHelp,
        onClick = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://tmbv-hms.com/privacy-policy.php")
            )
            context.startActivity(intent)
        }
    )

    SettingsItem(
        icon = Icons.Default.Send,
        title = "Doniraj",
        subtitle = "Podrži razvoj aplikacije putem Stripe",
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stripeDonationUrl))
            context.startActivity(intent)
        }
    )

    SettingsItem(
        icon = Icons.Default.TrackChanges,
        title = "Habit Tracker",
        subtitle = "Prati dnevne navike",
        onClick = onOpenHabitTracker
    )

/*
    SettingsItem(
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        title = "Trcanje",
        subtitle = "Samostalni feature za evidenciju treninga",
        onClick = onOpenRunningFeature
    )

    SettingsItem(
        icon = Icons.Default.Hub,
        title = "Kuiver + Paginator",
        subtitle = "Interaktivni graf i paginirana lista scenarija",
        onClick = onOpenKuiverFeature
    )
*/

    SettingsItem(
        icon = Icons.Default.Info,
        title = Strings.aboutApp,
        subtitle = Strings.version + BuildConfig.VERSION_NAME,
        onClick = onShowAboutDialog
    )

    SettingsItem(
        icon = Icons.Default.History,
        title = Strings.appUsageTime,
        subtitle = settingsFormatUsageTime(milliseconds = appUsageTimeMs),
        onClick = { }
    )
}
