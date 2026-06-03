package com.youtube.rating.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.storage.TaskItem
import com.youtube.rating.android.ui.components.AppMenuDrawerContent
import com.youtube.rating.android.ui.screens.TrainingRightDrawerContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class MainDrawerState(
    val currentRoute: String?,
    val isAdminMode: Boolean,
    val debugUnlocked: Boolean,
    val brightness: Float
)

internal data class MainDrawerActions(
    val onBrightnessChange: (Float) -> Unit,
    val onOpenContentLanguages: () -> Unit,
    val onOpenBugReport: () -> Unit,
    val onNavigate: (String) -> Unit
)

internal data class TrainingDrawerState(
    val pinned: Set<String>,
    val readCount: Int,
    val thinkingMin: Int,
    val psalmsDone: Int,
    val psalmsTotal: Int,
    val sequentialSubtitle: String
)

internal data class TaskDrawerActions(
    val onAddTask: (String) -> Unit,
    val onToggleTask: (Long, Boolean) -> Unit,
    val onDeleteTask: (Long) -> Unit,
    val onUpdateTaskTitle: (Long, String) -> Unit,
    val onTogglePinned: (Long) -> Unit,
    val onMoveTask: (Long, Boolean) -> Unit,
    val onClearCompleted: () -> Pair<Int, List<TaskItem>>?,
    val onSetAllDone: (Boolean) -> Unit,
    val onRestoreTasks: (Int, List<TaskItem>) -> Unit
)

@Composable
internal fun RatingAppDrawers(
    leftDrawerState: DrawerState,
    rightDrawerState: DrawerState,
    gesturesEnabled: Boolean,
    scope: CoroutineScope,
    mainDrawerState: MainDrawerState,
    mainDrawerActions: MainDrawerActions,
    isTrainingTab: Boolean,
    trainingDrawerState: TrainingDrawerState,
    taskItems: List<TaskItem>,
    taskDrawerActions: TaskDrawerActions,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = leftDrawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    AppMenuDrawerContent(
                        currentRoute = mainDrawerState.currentRoute,
                        isAdminMode = mainDrawerState.isAdminMode,
                        debugUnlocked = mainDrawerState.debugUnlocked,
                        brightness = mainDrawerState.brightness,
                        onBrightnessChange = mainDrawerActions.onBrightnessChange,
                        onOpenContentLanguages = mainDrawerActions.onOpenContentLanguages,
                        onOpenBugReport = mainDrawerActions.onOpenBugReport,
                        onNavigate = mainDrawerActions.onNavigate,
                        onClose = { scope.launch { leftDrawerState.close() } }
                    )
                }
            }
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = rightDrawerState,
                gesturesEnabled = gesturesEnabled,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight(),
                            drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            drawerTonalElevation = 8.dp
                        ) {
                            if (isTrainingTab) {
                                TrainingDrawerContent(state = trainingDrawerState)
                            } else {
                                TaskManagementDrawerContent(
                                    taskItems,
                                    onAddTask = taskDrawerActions.onAddTask,
                                    onToggleTask = taskDrawerActions.onToggleTask,
                                    onDeleteTask = taskDrawerActions.onDeleteTask,
                                    onUpdateTaskTitle = taskDrawerActions.onUpdateTaskTitle,
                                    onTogglePinned = taskDrawerActions.onTogglePinned,
                                    onMoveTask = taskDrawerActions.onMoveTask,
                                    onClearCompleted = taskDrawerActions.onClearCompleted,
                                    onSetAllDone = taskDrawerActions.onSetAllDone,
                                    onRestoreTasks = taskDrawerActions.onRestoreTasks,
                                    onClose = { scope.launch { rightDrawerState.close() } }
                                )
                            }
                        }
                    }
                }
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun TrainingDrawerContent(state: TrainingDrawerState) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        TrainingRightDrawerContent(
            pinned = state.pinned,
            readCount = state.readCount,
            thinkingMin = state.thinkingMin,
            psalmsDone = state.psalmsDone,
            psalmsTotal = state.psalmsTotal,
            sequentialSubtitle = state.sequentialSubtitle
        )
    }
}
