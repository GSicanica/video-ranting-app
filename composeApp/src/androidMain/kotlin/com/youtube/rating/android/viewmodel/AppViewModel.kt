package com.youtube.rating.android.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.FastingEntry
import com.youtube.rating.android.storage.FastingReminderSettings
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.android.storage.TaskItem
import com.youtube.rating.android.storage.TaskManager
import com.youtube.rating.android.ui.models.VideoCloseAction
import com.youtube.rating.android.utils.AutoBackupManager
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(
    application: Application,
    taskManager: TaskManager,
    autoBackupManager: AutoBackupManager
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val languageState = AppLanguageStateHolder(appContext, viewModelScope)
    private val dialogState = AppDialogStateHolder()
    private val navigationState = AppNavigationStateHolder()
    private val playbackState = AppPlaybackStateHolder()
    private val taskState = AppTaskStateHolder(
        appContext = appContext,
        scope = viewModelScope,
        taskManager = taskManager,
        autoBackupManager = autoBackupManager
    )
    private val fastingState = AppFastingStateHolder(appContext, viewModelScope)

    val currentIntent: StateFlow<Intent?> = navigationState.currentIntent
    val currentLanguage: StateFlow<Strings.Language> = languageState.currentLanguage
    val selectedContentLanguages: StateFlow<Set<Strings.Language>> =
        languageState.selectedContentLanguages
    val showContentLanguageDialog: StateFlow<Boolean> = dialogState.showContentLanguageDialog
    val showBugReportDialog: StateFlow<Boolean> = dialogState.showBugReportDialog
    val homeTabClickTick: StateFlow<Long> = navigationState.homeTabClickTick
    val showOfflineVideoPlayer: StateFlow<Boolean> = playbackState.showOfflineVideoPlayer
    val selectedOfflineVideo: StateFlow<OfflineVideo?> = playbackState.selectedOfflineVideo
    val taskItems: StateFlow<List<TaskItem>> = taskState.taskItems
    val fullScreenVideoActive: StateFlow<Boolean> = playbackState.fullScreenVideoActive
    val isInPipMode: StateFlow<Boolean> = playbackState.isInPipMode
    val floatingVideoState: StateFlow<AppFloatingVideoState?> = playbackState.floatingVideoState
    val fastingEntries: StateFlow<Map<String, FastingEntry>> = fastingState.fastingEntries
    val fastingWeeklyGoal: StateFlow<Int> = fastingState.fastingWeeklyGoal
    val fastingReminderSettings: StateFlow<FastingReminderSettings> =
        fastingState.fastingReminderSettings

    fun setIntent(intent: Intent?) = navigationState.setIntent(intent)
    fun setCurrentLanguage(language: Strings.Language) = languageState.setCurrentLanguage(language)
    fun setSelectedContentLanguages(languages: Set<Strings.Language>) =
        languageState.setSelectedContentLanguages(languages)
    fun setShowContentLanguageDialog(show: Boolean) =
        dialogState.setShowContentLanguageDialog(show)
    fun setShowBugReportDialog(show: Boolean) = dialogState.setShowBugReportDialog(show)
    fun bumpHomeTabClick() = navigationState.bumpHomeTabClick()
    fun openOfflineVideo(video: OfflineVideo) = playbackState.openOfflineVideo(video)
    fun closeOfflineVideo() = playbackState.closeOfflineVideo()
    fun setFullScreenVideoActive(active: Boolean) = playbackState.setFullScreenVideoActive(active)
    fun setInPipMode(active: Boolean) = playbackState.setInPipMode(active)
    fun startFloatingVideo(video: VideoSearchResult, startSeconds: Int, mode: VideoCloseAction) =
        playbackState.startFloatingVideo(video, startSeconds, mode)
    fun stopFloatingVideo() = playbackState.stopFloatingVideo()

    fun addTask(title: String) = taskState.addTask(title)
    fun toggleTaskDone(index: Int, done: Boolean) = taskState.toggleTaskDone(index, done)
    fun toggleTaskDoneById(id: Long, done: Boolean) = taskState.toggleTaskDoneById(id, done)
    fun deleteTask(index: Int) = taskState.deleteTask(index)
    fun deleteTaskById(id: Long) = taskState.deleteTaskById(id)
    fun updateTaskTitleById(id: Long, title: String) = taskState.updateTaskTitleById(id, title)
    fun toggleTaskPinnedById(id: Long) = taskState.toggleTaskPinnedById(id)

    enum class MoveDirection { UP, DOWN }

    fun moveTaskById(id: Long, direction: MoveDirection) = taskState.moveTaskById(id, direction)
    fun restoreTasksAt(tasks: List<TaskItem>, startIndex: Int) =
        taskState.restoreTasksAt(tasks, startIndex)
    fun clearCompletedTasks() = taskState.clearCompletedTasks()
    fun clearCompletedTasksForUndo(): Pair<Int, List<TaskItem>>? =
        taskState.clearCompletedTasksForUndo()
    fun setAllTasksDone(done: Boolean) = taskState.setAllTasksDone(done)

    fun saveFastingEntry(entry: FastingEntry) = fastingState.saveFastingEntry(entry)
    fun updateFastingWeeklyGoal(goal: Int) = fastingState.updateFastingWeeklyGoal(goal)
    fun updateFastingReminder(settings: FastingReminderSettings) =
        fastingState.updateFastingReminder(settings)
}
