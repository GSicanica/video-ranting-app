package com.youtube.rating.android

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youtube.rating.android.data.prefs.AdminPrefs
import com.youtube.rating.android.data.settings.SettingsRepository
import com.youtube.rating.android.data.prefs.TrainingPrefs
import com.youtube.rating.android.localization.ContentLanguageManager
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.navigation.Screen
import com.youtube.rating.android.network.BaseUrlProvider
import com.youtube.rating.android.sentry.SentryLogger
import com.youtube.rating.android.utils.BibleApiService
import com.youtube.rating.android.utils.BibleQuoteGenerator
import com.youtube.rating.android.viewmodel.AppViewModel
import com.youtube.rating.android.viewmodel.BibleHomeViewModel
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

internal fun normalizeRouteForMetrics(route: String?): String {
    if (route.isNullOrBlank()) return "unknown"
    return if (route.startsWith("rate/")) "rate" else route
}

internal fun trackScreenDurationMetric(screen: String, durationMs: Long) {
    if (durationMs <= 0L) return
    SentryLogger.metricDistribution(
        "screen_time_ms",
        durationMs.toDouble(),
        tags = mapOf("screen" to screen)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingApp(
    initialIntent: Intent? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTrainingTab = isTrainingRoute(currentRoute)
    val drawerGesturesEnabled = true
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ App-level state moved to AppViewModel
    val appViewModel: AppViewModel = koinViewModel()
    val contentLanguageManager = remember(context) { ContentLanguageManager(context = context) }
    val currentLanguage by appViewModel.currentLanguage.collectAsStateWithLifecycle()
    val selectedContentLanguages by appViewModel.selectedContentLanguages.collectAsStateWithLifecycle()
    val showContentLanguageDialog by appViewModel.showContentLanguageDialog.collectAsStateWithLifecycle()
    val showBugReportDialog by appViewModel.showBugReportDialog.collectAsStateWithLifecycle()
    val homeTabClickTick by appViewModel.homeTabClickTick.collectAsStateWithLifecycle()
    val showOfflineVideoPlayer by appViewModel.showOfflineVideoPlayer.collectAsStateWithLifecycle()
    val selectedOfflineVideo by appViewModel.selectedOfflineVideo.collectAsStateWithLifecycle()
    val floatingVideoState by appViewModel.floatingVideoState.collectAsStateWithLifecycle()
    val isInPipMode by appViewModel.isInPipMode.collectAsStateWithLifecycle()
    val isAdminMode by AdminPrefs.adminModeFlow(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val brightnessManager: AppBrightnessManager = koinInject()
    val brightness by brightnessManager.brightness.collectAsStateWithLifecycle()
    var debugUnlocked by remember { mutableStateOf(false) }
    val currentIntent by appViewModel.currentIntent.collectAsStateWithLifecycle()

    val settingsRepository: SettingsRepository = koinInject()
    val settingsState by settingsRepository.state.collectAsStateWithLifecycle()
    val homeScreenStyle = settingsState.homeScreenStyle
    val callsTabEnabled by BaseUrlProvider.callsTabEnabledFlow()
        .collectAsStateWithLifecycle(initialValue = BaseUrlProvider.getCallsTabEnabled())

    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val trainingPsalmsToday = rememberTrainingPsalmsToday(
        context = context,
        isTrainingTab = isTrainingTab,
        leftDrawerState = leftDrawerState,
        rightDrawerState = rightDrawerState,
        scope = scope
    )
    val trainingViewModel: BibleHomeViewModel = koinViewModel()
    val trainingUi by trainingViewModel.uiState.collectAsStateWithLifecycle()
    val trainingPinned by TrainingPrefs.trainingDrawerPinnedFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val trainingPsalmsDone by TrainingPrefs.trainingPsalmsDoneTodayFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val trainingPsalmsDoneCollection: Collection<String> = trainingPsalmsDone
    val trainingPsalmsDoneCount = remember(trainingPsalmsToday, trainingPsalmsDone) {
        trainingPsalmsToday.count { trainingPsalmsDoneCollection.contains(it.toString()) }
    }

    LaunchedEffect(currentRoute) {
        debugUnlocked = AdminPrefs.getDebugUnlocked(context)
    }

    LaunchedEffect(callsTabEnabled, currentRoute) {
        if (!callsTabEnabled && currentRoute == Screen.Calls.route) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    RatingAppBackHandler(
        activity = context as? Activity,
        navController = navController,
        leftDrawerState = leftDrawerState,
        rightDrawerState = rightDrawerState,
        scope = scope
    )

    val taskItems by appViewModel.taskItems.collectAsStateWithLifecycle()

    val fastingEntries by appViewModel.fastingEntries.collectAsStateWithLifecycle()
    val fastingWeeklyGoal by appViewModel.fastingWeeklyGoal.collectAsStateWithLifecycle()

    AppStringsCoordinator(
        currentLanguage = currentLanguage,
        selectedContentLanguages = selectedContentLanguages
    )
    ScreenMetricsTracker(currentRoute = currentRoute, lifecycleOwner = lifecycleOwner)
    IntentRouter(
        initialIntent = initialIntent,
        currentIntent = currentIntent,
        onConsumeIntent = { appViewModel.setIntent(null) },
        navController = navController,
        onSetInitialIntent = { appViewModel.setIntent(it) }
    )

    val trainingSequentialSubtitle =
        if (trainingUi.currentBookName != null && trainingUi.currentBookChapters != null) {
            Strings.sequentialSubtitle(
                trainingUi.currentBookName ?: "",
                trainingUi.bibleChapter,
                trainingUi.currentBookChapters ?: 0,
                (trainingUi.seqProgress * 100).toInt()
            )
        } else {
            Strings.noSelection
        }

    RatingAppDrawers(
        leftDrawerState = leftDrawerState,
        rightDrawerState = rightDrawerState,
        gesturesEnabled = drawerGesturesEnabled,
        scope = scope,
        mainDrawerState = MainDrawerState(
            currentRoute = currentRoute,
            isAdminMode = isAdminMode,
            debugUnlocked = debugUnlocked,
            brightness = brightness
        ),
        mainDrawerActions = MainDrawerActions(
            onBrightnessChange = brightnessManager::setBrightness,
            onOpenContentLanguages = { appViewModel.setShowContentLanguageDialog(true) },
            onOpenBugReport = { appViewModel.setShowBugReportDialog(true) },
            onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } }
        ),
        isTrainingTab = isTrainingTab,
        trainingDrawerState = TrainingDrawerState(
            pinned = trainingPinned,
            readCount = trainingUi.bibleReadToday,
            thinkingMin = trainingUi.thinkingMin,
            psalmsDone = trainingPsalmsDoneCount,
            psalmsTotal = trainingPsalmsToday.size.coerceAtLeast(3),
            sequentialSubtitle = trainingSequentialSubtitle
        ),
        taskItems = taskItems,
        taskDrawerActions = TaskDrawerActions(
            onAddTask = appViewModel::addTask,
            onToggleTask = appViewModel::toggleTaskDoneById,
            onDeleteTask = appViewModel::deleteTaskById,
            onUpdateTaskTitle = appViewModel::updateTaskTitleById,
            onTogglePinned = appViewModel::toggleTaskPinnedById,
            onMoveTask = { id, up ->
                appViewModel.moveTaskById(
                    id,
                    if (up) AppViewModel.MoveDirection.UP else AppViewModel.MoveDirection.DOWN
                )
            },
            onClearCompleted = appViewModel::clearCompletedTasksForUndo,
            onSetAllDone = appViewModel::setAllTasksDone,
            onRestoreTasks = { index, items -> appViewModel.restoreTasksAt(items, index) }
        )
    ) {
        RatingAppScaffold(
            navController = navController,
            currentRoute = currentRoute,
            currentLanguage = currentLanguage,
            selectedContentLanguages = selectedContentLanguages,
            homeScreenStyle = homeScreenStyle,
            homeTabClickTick = homeTabClickTick,
            fastingEntries = fastingEntries,
            fastingWeeklyGoal = fastingWeeklyGoal,
            isTrainingTab = isTrainingTab,
            isInPipMode = isInPipMode,
            callsTabEnabled = callsTabEnabled,
            floatingVideoState = floatingVideoState,
            leftDrawerState = leftDrawerState,
            rightDrawerState = rightDrawerState,
            scope = scope,
            onHomeReClick = appViewModel::bumpHomeTabClick,
            onPlayVideo = appViewModel::openOfflineVideo,
            onStartFloatingVideo = appViewModel::startFloatingVideo,
            onStopFloatingVideo = appViewModel::stopFloatingVideo,
            onSaveFastingEntry = appViewModel::saveFastingEntry,
            onUpdateFastingGoal = appViewModel::updateFastingWeeklyGoal,
            onFullScreenVideoActiveChange = appViewModel::setFullScreenVideoActive
        )

        RatingRuntimeDialogs(
            showContentLanguageDialog = showContentLanguageDialog,
            selectedContentLanguages = selectedContentLanguages,
            contentLanguageManager = contentLanguageManager,
            onContentLanguagesSelected = { newLanguages ->
                appViewModel.setSelectedContentLanguages(newLanguages)
                contentLanguageManager.saveContentLanguages(newLanguages)
                appViewModel.setShowContentLanguageDialog(false)
            },
            onDismissContentLanguageDialog = {
                appViewModel.setShowContentLanguageDialog(false)
            },
            showOfflineVideoPlayer = showOfflineVideoPlayer,
            selectedOfflineVideo = selectedOfflineVideo,
            onDismissOfflineVideo = { appViewModel.closeOfflineVideo() }
        )
    }

    BugReportCoordinator(
        show = showBugReportDialog,
        context = context,
        scope = scope,
        onDismiss = { appViewModel.setShowBugReportDialog(false) }
    )

}
