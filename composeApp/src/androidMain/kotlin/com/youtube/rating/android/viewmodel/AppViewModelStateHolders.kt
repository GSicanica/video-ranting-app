package com.youtube.rating.android.viewmodel

import android.content.Context
import android.content.Intent
import com.youtube.rating.android.data.prefs.FastingPrefs
import com.youtube.rating.android.data.prefs.TasksPrefs
import com.youtube.rating.android.localization.ContentLanguageManager
import com.youtube.rating.android.localization.LanguageManager
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.FastingEntry
import com.youtube.rating.android.storage.FastingManager
import com.youtube.rating.android.storage.FastingReminderSettings
import com.youtube.rating.android.storage.OfflineVideo
import com.youtube.rating.android.storage.TaskItem
import com.youtube.rating.android.storage.TaskManager
import com.youtube.rating.android.ui.models.VideoCloseAction
import com.youtube.rating.android.utils.AutoBackupManager
import com.youtube.rating.android.utils.ChangeType
import com.youtube.rating.core.coroutines.ioDispatcher
import com.youtube.rating.core.coroutines.makeIOCall
import com.youtube.rating.shared.models.VideoSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AppLanguageStateHolder(
    private val appContext: Context,
    private val scope: CoroutineScope
) {
    private val languageManager = LanguageManager(context = appContext)
    private val contentLanguageManager = ContentLanguageManager(context = appContext)

    private val _currentLanguage = MutableStateFlow(Strings.currentLanguage)
    val currentLanguage: StateFlow<Strings.Language> = _currentLanguage.asStateFlow()

    private val _selectedContentLanguages = MutableStateFlow<Set<Strings.Language>>(emptySet())
    val selectedContentLanguages: StateFlow<Set<Strings.Language>> =
        _selectedContentLanguages.asStateFlow()

    init {
        scope.launch {
            _currentLanguage.value = withContext(ioDispatcher) { languageManager.loadLanguage() }
        }
        scope.launch {
            _selectedContentLanguages.value = withContext(ioDispatcher) {
                contentLanguageManager.loadContentLanguages()
            }
        }
    }

    fun setCurrentLanguage(language: Strings.Language) {
        _currentLanguage.value = language
    }

    fun setSelectedContentLanguages(languages: Set<Strings.Language>) {
        _selectedContentLanguages.value = languages
    }
}

internal class AppDialogStateHolder {
    private val _showContentLanguageDialog = MutableStateFlow(false)
    val showContentLanguageDialog: StateFlow<Boolean> =
        _showContentLanguageDialog.asStateFlow()

    private val _showBugReportDialog = MutableStateFlow(false)
    val showBugReportDialog: StateFlow<Boolean> = _showBugReportDialog.asStateFlow()

    fun setShowContentLanguageDialog(show: Boolean) {
        _showContentLanguageDialog.value = show
    }

    fun setShowBugReportDialog(show: Boolean) {
        _showBugReportDialog.value = show
    }
}

internal class AppNavigationStateHolder {
    private val _currentIntent = MutableStateFlow<Intent?>(null)
    val currentIntent: StateFlow<Intent?> = _currentIntent.asStateFlow()

    private val _homeTabClickTick = MutableStateFlow(0L)
    val homeTabClickTick: StateFlow<Long> = _homeTabClickTick.asStateFlow()

    fun setIntent(intent: Intent?) {
        _currentIntent.value = intent
    }

    fun bumpHomeTabClick() {
        _homeTabClickTick.value = System.currentTimeMillis()
    }
}

data class AppFloatingVideoState(
    val video: VideoSearchResult,
    val startSeconds: Int,
    val mode: VideoCloseAction
)

internal class AppPlaybackStateHolder {

    private val _showOfflineVideoPlayer = MutableStateFlow(false)
    val showOfflineVideoPlayer: StateFlow<Boolean> = _showOfflineVideoPlayer.asStateFlow()

    private val _selectedOfflineVideo = MutableStateFlow<OfflineVideo?>(null)
    val selectedOfflineVideo: StateFlow<OfflineVideo?> = _selectedOfflineVideo.asStateFlow()

    private val _fullScreenVideoActive = MutableStateFlow(false)
    val fullScreenVideoActive: StateFlow<Boolean> = _fullScreenVideoActive.asStateFlow()

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private val _floatingVideoState = MutableStateFlow<AppFloatingVideoState?>(null)
    val floatingVideoState: StateFlow<AppFloatingVideoState?> = _floatingVideoState.asStateFlow()

    fun openOfflineVideo(video: OfflineVideo) {
        _selectedOfflineVideo.value = video
        _showOfflineVideoPlayer.value = true
    }

    fun closeOfflineVideo() {
        _showOfflineVideoPlayer.value = false
        _selectedOfflineVideo.value = null
    }

    fun setFullScreenVideoActive(active: Boolean) {
        _fullScreenVideoActive.value = active
    }

    fun setInPipMode(active: Boolean) {
        _isInPipMode.value = active
    }

    fun startFloatingVideo(
        video: VideoSearchResult,
        startSeconds: Int,
        mode: VideoCloseAction
    ) {
        _floatingVideoState.value = AppFloatingVideoState(
            video = video,
            startSeconds = startSeconds.coerceAtLeast(0),
            mode = mode
        )
    }

    fun stopFloatingVideo() {
        _floatingVideoState.value = null
    }
}

internal class AppTaskStateHolder(
    appContext: Context,
    private val scope: CoroutineScope,
    private val taskManager: TaskManager,
    private val autoBackupManager: AutoBackupManager
) {
    private val _taskItems = MutableStateFlow<List<TaskItem>>(emptyList())
    val taskItems: StateFlow<List<TaskItem>> = _taskItems.asStateFlow()

    init {
        scope.launch {
            TasksPrefs.tasksJsonFlow(appContext).collect {
                _taskItems.value = withContext(ioDispatcher) { taskManager.getTasks() }
            }
        }
    }

    private inline fun updateTasks(crossinline mutate: (MutableList<TaskItem>) -> Unit) {
        val list = _taskItems.value.toMutableList()
        mutate(list)
        _taskItems.value = list
        scope.makeIOCall {
            taskManager.saveTasks(list)
            autoBackupManager.triggerAutoBackup(ChangeType.TASK_UPDATED)
        }
    }

    fun addTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val newTask = TaskItem(
            id = System.currentTimeMillis(),
            title = trimmed,
            isDone = false,
            order = System.currentTimeMillis()
        )
        updateTasks { it.add(0, newTask) }
    }

    fun toggleTaskDone(index: Int, done: Boolean) {
        updateTasks { list ->
            if (index !in list.indices) return@updateTasks
            list[index] = list[index].copy(isDone = done, updatedAt = System.currentTimeMillis())
        }
    }

    fun toggleTaskDoneById(id: Long, done: Boolean) {
        updateTasks { list ->
            val i = list.indexOfFirst { it.id == id }
            if (i < 0) return@updateTasks
            list[i] = list[i].copy(isDone = done, updatedAt = System.currentTimeMillis())
        }
    }

    fun deleteTask(index: Int) {
        updateTasks { list ->
            if (index !in list.indices) return@updateTasks
            list.removeAt(index)
        }
    }

    fun deleteTaskById(id: Long) {
        updateTasks { list ->
            val i = list.indexOfFirst { it.id == id }
            if (i < 0) return@updateTasks
            list.removeAt(i)
        }
    }

    fun updateTaskTitleById(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        updateTasks { list ->
            val i = list.indexOfFirst { it.id == id }
            if (i < 0) return@updateTasks
            val task = list[i]
            if (task.title == trimmed) return@updateTasks
            list[i] = task.copy(title = trimmed, updatedAt = System.currentTimeMillis())
        }
    }

    fun toggleTaskPinnedById(id: Long) {
        updateTasks { list ->
            val i = list.indexOfFirst { it.id == id }
            if (i < 0) return@updateTasks
            val task = list[i]
            list[i] = task.copy(pinned = !task.pinned, updatedAt = System.currentTimeMillis())
        }
    }

    fun moveTaskById(id: Long, direction: AppViewModel.MoveDirection) {
        updateTasks { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return@updateTasks

            val task = list[index]
            val sameGroup = list
                .filter { it.pinned == task.pinned }
                .sortedByDescending { it.order }

            val pos = sameGroup.indexOfFirst { it.id == id }
            if (pos < 0) return@updateTasks

            val swapWith = when (direction) {
                AppViewModel.MoveDirection.UP -> sameGroup.getOrNull(pos - 1)
                AppViewModel.MoveDirection.DOWN -> sameGroup.getOrNull(pos + 1)
            } ?: return@updateTasks

            val otherIndex = list.indexOfFirst { it.id == swapWith.id }
            if (otherIndex < 0) return@updateTasks

            val now = System.currentTimeMillis()
            val a = list[index]
            val b = list[otherIndex]
            list[index] = a.copy(order = b.order, updatedAt = now)
            list[otherIndex] = b.copy(order = a.order, updatedAt = now)
        }
    }

    fun restoreTasksAt(tasks: List<TaskItem>, startIndex: Int) {
        if (tasks.isEmpty()) return
        updateTasks { list ->
            val idx = startIndex.coerceIn(0, list.size)
            list.addAll(idx, tasks)
        }
    }

    fun clearCompletedTasks() {
        if (_taskItems.value.none { it.isDone }) return
        updateTasks { list -> list.removeAll { it.isDone } }
    }

    fun clearCompletedTasksForUndo(): Pair<Int, List<TaskItem>>? {
        val current = _taskItems.value
        val removed = current.filter { it.isDone }
        if (removed.isEmpty()) return null
        val firstIndex = current.indexOfFirst { it.isDone }.coerceAtLeast(0)
        val updated = current.filterNot { it.isDone }
        _taskItems.value = updated
        scope.makeIOCall {
            taskManager.saveTasks(updated)
            autoBackupManager.triggerAutoBackup(ChangeType.TASK_UPDATED)
        }
        return firstIndex to removed
    }

    fun setAllTasksDone(done: Boolean) {
        if (_taskItems.value.isEmpty()) return
        val updatedAt = System.currentTimeMillis()
        updateTasks { list ->
            for (i in list.indices) {
                list[i] = list[i].copy(isDone = done, updatedAt = updatedAt)
            }
        }
    }
}

internal class AppFastingStateHolder(
    appContext: Context,
    private val scope: CoroutineScope
) {
    private val fastingManager = FastingManager.getInstance(appContext)

    private val _fastingEntries = MutableStateFlow<Map<String, FastingEntry>>(emptyMap())
    val fastingEntries: StateFlow<Map<String, FastingEntry>> = _fastingEntries.asStateFlow()

    private val _fastingWeeklyGoal = MutableStateFlow(3)
    val fastingWeeklyGoal: StateFlow<Int> = _fastingWeeklyGoal.asStateFlow()

    private val _fastingReminderSettings = MutableStateFlow(FastingReminderSettings())
    val fastingReminderSettings: StateFlow<FastingReminderSettings> =
        _fastingReminderSettings.asStateFlow()

    init {
        scope.launch {
            FastingPrefs.fastingEntriesFlow(appContext).collect {
                _fastingEntries.value = withContext(ioDispatcher) { fastingManager.getEntries() }
                _fastingWeeklyGoal.value = withContext(ioDispatcher) { fastingManager.getWeeklyGoal() }
                _fastingReminderSettings.value = withContext(ioDispatcher) {
                    fastingManager.getReminderSettings()
                }
            }
        }
    }

    fun saveFastingEntry(entry: FastingEntry) {
        scope.launch {
            _fastingEntries.value = withContext(ioDispatcher) { fastingManager.upsertEntry(entry) }
        }
    }

    fun updateFastingWeeklyGoal(goal: Int) {
        scope.launch {
            withContext(ioDispatcher) { fastingManager.setWeeklyGoal(goal) }
            _fastingWeeklyGoal.value = goal
        }
    }

    fun updateFastingReminder(settings: FastingReminderSettings) {
        scope.launch {
            withContext(ioDispatcher) { fastingManager.saveReminderSettings(settings) }
            _fastingReminderSettings.value = settings
        }
    }
}
