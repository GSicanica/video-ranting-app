package com.youtube.rating.android.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext
import com.youtube.rating.android.ui.screens.AnalyticsScreen
import com.youtube.rating.android.ui.screens.CallsScreen
import com.youtube.rating.android.ui.screens.FavoritesScreen
import com.youtube.rating.android.ui.screens.FastingScreen
import com.youtube.rating.android.ui.screens.KuiverGraphScreen
import com.youtube.rating.android.ui.screens.NotesScreen
import com.youtube.rating.android.ui.screens.RosaryScreen
import com.youtube.rating.android.ui.screens.SettingsScreen
import com.youtube.rating.android.ui.screens.prayer.PrayerScreen
import com.youtube.rating.android.storage.FastingEntry
import com.youtube.rating.android.storage.FavoritesGateway
import com.youtube.rating.android.utils.AdminManager
import com.youtube.rating.android.utils.DeepLinkUtil
import com.youtube.rating.android.utils.rememberHapticFeedback
import com.youtube.rating.android.viewmodel.PrayerViewModel
import com.youtube.rating.android.viewmodel.RatingViewModel
import com.youtube.rating.android.ui.screens.home.dialogs.VideoDetailsDialog
import com.youtube.rating.habittracker.presentation.navigation.habitTrackerScreen
import com.youtube.rating.running.presentation.navigation.runningScreen
import org.koin.compose.koinInject

fun NavGraphBuilder.mainNavigationGraph(
    navCoordinator: NavigationCoordinator,
    fastingEntries: Map<String, FastingEntry>,
    fastingWeeklyGoal: Int,
    onSaveFastingEntry: (FastingEntry) -> Unit,
    onUpdateFastingGoal: (Int) -> Unit,
    onOpenLeftDrawer: () -> Unit = {},
    onOpenRightDrawer: () -> Unit = {},
    isInPipMode: Boolean,
    onFullScreenVideoActiveChange: (Boolean) -> Unit
) {
    // Prayer Screen
    composable(Screen.Prayer.route) {
        val haptic = rememberHapticFeedback()
        val prayerVm: PrayerViewModel = koinInject()
        val adminManager: AdminManager = koinInject()

        PrayerScreen(
            viewModel = prayerVm,
            adminManager = adminManager,
            fastingEntries = fastingEntries,
            fastingWeeklyGoal = fastingWeeklyGoal,
            onSaveFastingEntry = onSaveFastingEntry,
            onUpdateFastingGoal = onUpdateFastingGoal,
            onOpenLeftDrawer = onOpenLeftDrawer,
            onOpenRightDrawer = onOpenRightDrawer,
            onHapticLight = { haptic.lightTap() },
            onHapticSuccess = { haptic.success() },
            fastingContent = {
                FastingScreen(
                    fastingEntries = fastingEntries,
                    weeklyGoal = fastingWeeklyGoal,
                    onSaveEntry = onSaveFastingEntry,
                    onUpdateGoal = onUpdateFastingGoal,
                    onBack = {},
                    showTopBar = false
                )
            },
            rosaryContent = {
                RosaryScreen()
            }
        )
    }

    // Favorites Screen
    composable(Screen.Favorites.route) {
        val context = LocalContext.current
        val favoritesGateway: FavoritesGateway = koinInject()
        FavoritesScreen(
            videoDetailsContent = { video, onDismiss ->
                val videoSearchResult = video.toVideoSearchResult()
                VideoDetailsDialog(
                    video = videoSearchResult,
                    onQuickRate = {},
                    submitState = RatingViewModel.SubmitState.Idle,
                    isFavorite = true,
                    onToggleFavorite = {
                        favoritesGateway.removeFavorite(video.videoId)
                        onDismiss()
                    },
                    onShare = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, DeepLinkUtil.getAppShareUrl(video.videoId))
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Podijeli"))
                    },
                    onCloseNoMini = onDismiss,
                    onDismiss = onDismiss,
                    isInPipMode = isInPipMode,
                    onFullScreenVideoActiveChange = onFullScreenVideoActiveChange
                )
            }
        )
    }

    // Notes Screen
    composable(Screen.Notes.route) {
        NotesScreen()
    }

    // Calls Screen
    composable(Screen.Calls.route) {
        CallsScreen()
    }

    // Analytics Screen
    composable(Screen.Analytics.route) {
        AnalyticsScreen(onBack = { navCoordinator.navigateUp() })
    }

    // Settings Screen
    composable(Screen.Settings.route) {
        SettingsScreen(
            onOpenHabitTracker = { navCoordinator.navigateToHabitTracker() },
            onOpenRunningFeature = { navCoordinator.navigateToRunningFeature() },
            onOpenKuiverFeature = { navCoordinator.navigateToKuiverGraph() }
        )
    }

    composable(Screen.KuiverGraph.route) {
        KuiverGraphScreen(onBack = { navCoordinator.navigateUp() })
    }

    runningScreen(onBack = { navCoordinator.navigateUp() })

    // Habit Tracker Screen
    habitTrackerScreen()

}
