package com.youtube.rating.android

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.navigation.AppNavGraph
import com.youtube.rating.android.navigation.AppNavGraphActions
import com.youtube.rating.android.navigation.AppNavGraphState
import com.youtube.rating.android.navigation.Screen
import com.youtube.rating.android.storage.FastingEntry
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.android.ui.components.FloatingVideoPlayer
import com.youtube.rating.android.viewmodel.AppFloatingVideoState
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun RatingAppScaffold(
    navController: NavHostController,
    currentRoute: String?,
    currentLanguage: Strings.Language,
    selectedContentLanguages: Set<Strings.Language>,
    homeScreenStyle: String,
    homeTabClickTick: Long,
    fastingEntries: Map<String, FastingEntry>,
    fastingWeeklyGoal: Int,
    isTrainingTab: Boolean,
    isInPipMode: Boolean,
    callsTabEnabled: Boolean,
    floatingVideoState: AppFloatingVideoState?,
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    scope: CoroutineScope,
    onHomeReClick: () -> Unit,
    onPlayVideo: (OfflineVideo) -> Unit,
    onStartFloatingVideo: (VideoSearchResult, Int, com.youtube.rating.android.ui.models.VideoCloseAction) -> Unit,
    onStopFloatingVideo: () -> Unit,
    onSaveFastingEntry: (FastingEntry) -> Unit,
    onUpdateFastingGoal: (Int) -> Unit,
    onFullScreenVideoActiveChange: (Boolean) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                RatingNavigationRail(
                    isInPipMode = isInPipMode,
                    currentRoute = currentRoute,
                    navController = navController,
                    callsTabEnabled = callsTabEnabled,
                    onHomeReClick = onHomeReClick
                )
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                bottomBar = {
                    if (!useNavRail) {
                        RatingBottomNavigationBar(
                            isInPipMode = isInPipMode,
                            currentRoute = currentRoute,
                            navController = navController,
                            callsTabEnabled = callsTabEnabled,
                            onHomeReClick = onHomeReClick
                        )
                    }
                }
            ) { paddingValues ->
                key(currentLanguage, selectedContentLanguages) {
                    var swipeTriggered by remember { mutableStateOf(false) }
                    var totalDragAmount by remember { mutableStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .then(
                                if (!isTrainingTab) {
                                    Modifier.pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                                swipeTriggered = false
                                                totalDragAmount = 0f
                                            },
                                            onDragEnd = {
                                                swipeTriggered = false
                                                totalDragAmount = 0f
                                            },
                                            onDragCancel = {
                                                swipeTriggered = false
                                                totalDragAmount = 0f
                                            }
                                        ) { _, dragAmount ->
                                            totalDragAmount += dragAmount
                                            if (!swipeTriggered && totalDragAmount > 25) {
                                                swipeTriggered = true
                                                scope.launch {
                                                    rightDrawerState.close()
                                                    leftDrawerState.open()
                                                }
                                            } else if (!swipeTriggered && totalDragAmount < -25) {
                                                swipeTriggered = true
                                                scope.launch {
                                                    leftDrawerState.close()
                                                    rightDrawerState.open()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        val isHomeRoute =
                            currentRoute == Screen.Home.route ||
                                currentRoute == Screen.RateVideo.route ||
                                (currentRoute?.startsWith("rate/") == true)

                        AppNavGraph(
                            modifier = Modifier.fillMaxSize(),
                            navController = navController,
                            state = AppNavGraphState(
                                homeScreenStyle = homeScreenStyle,
                                homeTabClickTick = homeTabClickTick,
                                fastingEntries = fastingEntries,
                                fastingWeeklyGoal = fastingWeeklyGoal,
                                isHomeRoute = isHomeRoute,
                                isInPipMode = isInPipMode
                            ),
                            actions = AppNavGraphActions(
                                onSaveFastingEntry = onSaveFastingEntry,
                                onUpdateFastingGoal = onUpdateFastingGoal,
                                onOpenLeftDrawer = {
                                    scope.launch {
                                        rightDrawerState.close()
                                        leftDrawerState.open()
                                    }
                                },
                                onOpenRightDrawer = {
                                    scope.launch {
                                        leftDrawerState.close()
                                        rightDrawerState.open()
                                    }
                                },
                                onPlayVideo = onPlayVideo,
                                onStartFloatingVideo = onStartFloatingVideo,
                                onStopFloatingVideo = onStopFloatingVideo,
                                onFullScreenVideoActiveChange = onFullScreenVideoActiveChange
                            )
                        )

                        floatingVideoState?.let { state ->
                            FloatingVideoPlayer(
                                video = state.video,
                                startSeconds = state.startSeconds,
                                modifier = Modifier.fillMaxSize(),
                                onExpand = { currentSecond ->
                                    navController.navigate(
                                        Screen.RateVideo.createRoute(
                                            state.video.videoId,
                                            currentSecond
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                },
                                onClose = onStopFloatingVideo
                            )
                        }
                    }
                }
            }
        }
    }
}
