package com.youtube.rating.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.youtube.rating.android.storage.FastingEntry
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.android.ui.models.VideoCloseAction
import com.youtube.rating.android.ui.screens.CompactHomeScreen
import com.youtube.rating.android.ui.screens.FastingScreen
import com.youtube.rating.android.ui.screens.HomeScreen
import com.youtube.rating.android.ui.screens.RosaryScreen
import com.youtube.rating.android.ui.screens.SaintsScreen
import com.youtube.rating.shared.models.VideoSearchResult

data class AppNavGraphState(
    val homeScreenStyle: String,
    val homeTabClickTick: Long,
    val fastingEntries: Map<String, FastingEntry>,
    val fastingWeeklyGoal: Int,
    val isHomeRoute: Boolean,
    val isInPipMode: Boolean
)

data class AppNavGraphActions(
    val onSaveFastingEntry: (FastingEntry) -> Unit,
    val onUpdateFastingGoal: (Int) -> Unit,
    val onOpenLeftDrawer: () -> Unit = {},
    val onOpenRightDrawer: () -> Unit = {},
    val onPlayVideo: (OfflineVideo) -> Unit = {},
    val onStartFloatingVideo: (VideoSearchResult, Int, VideoCloseAction) -> Unit,
    val onStopFloatingVideo: () -> Unit,
    val onFullScreenVideoActiveChange: (Boolean) -> Unit
)

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    state: AppNavGraphState,
    actions: AppNavGraphActions
) {
    val navCoordinator = NavigationCoordinator(navController = navController)

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            when (state.homeScreenStyle) {
                "compact" -> CompactHomeScreen(
                    isInPipMode = state.isInPipMode,
                    onFullScreenVideoActiveChange = actions.onFullScreenVideoActiveChange
                )
                "gallery" -> HomeScreen(
                    isHomeActive = state.isHomeRoute,
                    homeTabClickTick = state.homeTabClickTick,
                    onPlayVideo = actions.onPlayVideo,
                    onStartFloatingVideo = actions.onStartFloatingVideo,
                    onStopFloatingVideo = actions.onStopFloatingVideo,
                    isInPipMode = state.isInPipMode,
                    onFullScreenVideoActiveChange = actions.onFullScreenVideoActiveChange
                   )
                else -> HomeScreen(
                    isHomeActive = state.isHomeRoute,
                    homeTabClickTick = state.homeTabClickTick,
                    onPlayVideo = actions.onPlayVideo,
                    onStartFloatingVideo = actions.onStartFloatingVideo,
                    onStopFloatingVideo = actions.onStopFloatingVideo,
                    isInPipMode = state.isInPipMode,
                    onFullScreenVideoActiveChange = actions.onFullScreenVideoActiveChange
                )
            }
        }

        // Rate Video Screen (route = deep link)
        composable(
            route = Screen.RateVideo.route,
            arguments = listOf(
                navArgument("videoId") {
                    type = NavType.StringType
                },
                navArgument("start") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val videoId = ArgumentParser.parseVideoIdArgument(backStackEntry)
            val startSeconds = ArgumentParser.parseVideoStartArgument(backStackEntry)
            HomeScreen(
                initialVideoId = videoId,
                initialStartSeconds = startSeconds,
                isHomeActive = state.isHomeRoute,
                homeTabClickTick = state.homeTabClickTick,
                onPlayVideo = actions.onPlayVideo,
                onStartFloatingVideo = actions.onStartFloatingVideo,
                onStopFloatingVideo = actions.onStopFloatingVideo,
                isInPipMode = state.isInPipMode,
                onFullScreenVideoActiveChange = actions.onFullScreenVideoActiveChange
            )
        }

        // Fasting Screen
        composable(Screen.Fasting.route) {
            FastingScreen(
                fastingEntries = state.fastingEntries,
                weeklyGoal = state.fastingWeeklyGoal,
                onSaveEntry = actions.onSaveFastingEntry,
                onUpdateGoal = actions.onUpdateFastingGoal,
                onBack = { navCoordinator.navigateUp() }
            )
        }

        // Rosary Screen
        composable(Screen.Rosary.route) {
            RosaryScreen()
        }

        // Saints Screen
        composable(Screen.Saints.route) {
            SaintsScreen()
        }

        // Include grouped navigation graphs
        bibleNavigationGraph(
            navCoordinator = navCoordinator,
            onOpenLeftDrawer = actions.onOpenLeftDrawer,
            onOpenRightDrawer = actions.onOpenRightDrawer
        )
        mainNavigationGraph(
            navCoordinator = navCoordinator,
            fastingEntries = state.fastingEntries,
            fastingWeeklyGoal = state.fastingWeeklyGoal,
            onSaveFastingEntry = actions.onSaveFastingEntry,
            onUpdateFastingGoal = actions.onUpdateFastingGoal,
            onOpenLeftDrawer = actions.onOpenLeftDrawer,
            onOpenRightDrawer = actions.onOpenRightDrawer,
            isInPipMode = state.isInPipMode,
            onFullScreenVideoActiveChange = actions.onFullScreenVideoActiveChange
        )
        mediaNavigationGraph(navCoordinator, actions.onPlayVideo)
    }
}
