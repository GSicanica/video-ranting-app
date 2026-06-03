package com.youtube.rating.android

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youtube.rating.android.data.prefs.TasksPrefs
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.storage.TaskItem
import com.youtube.rating.core.coroutines.makeIOCall
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun TaskManagementDrawerContent(
    tasks: List<TaskItem>,
    onAddTask: (String) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onUpdateTaskTitle: (Long, String) -> Unit,
    onTogglePinned: (Long) -> Unit,
    onMoveTask: (Long, Boolean) -> Unit,
    onClearCompleted: () -> Pair<Int, List<TaskItem>>?,
    onSetAllDone: (Boolean) -> Unit,
    onRestoreTasks: (Int, List<TaskItem>) -> Unit,
    onClose: () -> Unit
) {
    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(TaskFilter.All) }
    var hideDone by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(TaskSort.NEWEST) }
    var pinnedExpanded by rememberSaveable { mutableStateOf(true) }
    var doneExpanded by rememberSaveable { mutableStateOf(false) }
    var editTaskId by remember { mutableStateOf<Long?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var showClearCompletedConfirm by remember { mutableStateOf(false) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val doneCount = remember(tasks) { tasks.count { it.isDone } }
    val activeCount = remember(tasks) { tasks.count { !it.isDone } }
    val totalCount = tasks.size
    val progress =
        if (totalCount == 0) 0f else (doneCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(Unit) {
        TasksPrefs.taskDrawerHideDoneFlow(context).collect { hideDone = it }
    }
    LaunchedEffect(Unit) {
        TasksPrefs.taskDrawerSortFlow(context).collect { raw ->
            sort = runCatching { TaskSort.valueOf(raw) }.getOrDefault(TaskSort.NEWEST)
        }
    }
    // Note: Do not auto-focus to avoid keyboard popping on app start.

    val lists = remember(tasks, filter, hideDone, sort) {
        val base = tasks
        val baseSorted = when (sort) {
            TaskSort.NEWEST -> base.sortedByDescending { it.updatedAt }
            TaskSort.OLDEST -> base.sortedBy { it.createdAt }
            TaskSort.AZ -> base.sortedBy { it.title.lowercase() }
            TaskSort.MANUAL -> base.sortedByDescending { it.order }
        }

        val pinned = baseSorted.filter { it.pinned }
        val unpinned = baseSorted.filter { !it.pinned }

        val match: (TaskItem) -> Boolean = { t ->
            when (filter) {
                TaskFilter.All -> true
                TaskFilter.Active -> !t.isDone
                TaskFilter.Done -> t.isDone
                TaskFilter.Pinned -> t.pinned
            }
        }

        val applyHideDone = hideDone && filter != TaskFilter.Done

        val visiblePinned = pinned.asSequence()
            .filter(match)
            .filter { !applyHideDone || !it.isDone }
            .toList()

        val visibleUnpinned = unpinned.asSequence()
            .filter(match)
            .filter { !applyHideDone || !it.isDone }
            .toList()

        val done = when (filter) {
            TaskFilter.Done -> baseSorted.filter { it.isDone }
            TaskFilter.All -> if (hideDone) emptyList() else baseSorted.filter { it.isDone }
            else -> emptyList()
        }

        Triple(visiblePinned, visibleUnpinned, done)
    }

    val pinnedList = lists.first
    val unpinnedList = lists.second
    val doneList = lists.third

    val isEmpty = pinnedList.isEmpty() && unpinnedList.isEmpty() && doneList.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Task manager",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                    )
                    Text(
                        text = "Aktivno: $activeCount  Gotovo: $doneCount  Ukupno: $totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showBulkMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Task actions")
                    }
                    DropdownMenu(
                        expanded = showBulkMenu,
                        onDismissRequest = { showBulkMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Oznaci sve kao gotovo") },
                            enabled = tasks.isNotEmpty() && doneCount != tasks.size,
                            onClick = {
                                showBulkMenu = false
                                onSetAllDone(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Resetuj sve (aktivno)") },
                            enabled = tasks.isNotEmpty() && doneCount > 0,
                            onClick = {
                                showBulkMenu = false
                                onSetAllDone(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Obrisi gotove…") },
                            enabled = doneCount > 0,
                            onClick = {
                                showBulkMenu = false
                                showClearCompletedConfirm = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Zatvori") },
                            onClick = {
                                showBulkMenu = false
                                onClose()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                label = { Text("Novi task") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val trimmed = newTaskTitle.trim()
                            if (trimmed.isNotEmpty()) {
                                onAddTask(trimmed)
                                newTaskTitle = ""
                                runCatching { focusRequester.requestFocus() }
                            }
                        },
                        enabled = newTaskTitle.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Dodaj task")
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val trimmed = newTaskTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddTask(trimmed)
                            newTaskTitle = ""
                            runCatching { focusRequester.requestFocus() }
                        } else {
                            focusManager.clearFocus()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item(key = "f_all") {
                    FilterChip(
                        selected = filter == TaskFilter.All,
                        onClick = { filter = TaskFilter.All },
                        label = { Text("Sve") }
                    )
                }
                item(key = "f_active") {
                    FilterChip(
                        selected = filter == TaskFilter.Active,
                        onClick = { filter = TaskFilter.Active },
                        label = { Text("Aktivno") }
                    )
                }
                item(key = "f_done") {
                    FilterChip(
                        selected = filter == TaskFilter.Done,
                        onClick = { filter = TaskFilter.Done },
                        label = { Text("Gotovo") }
                    )
                }
                item(key = "f_pinned") {
                    FilterChip(
                        selected = filter == TaskFilter.Pinned,
                        onClick = { filter = TaskFilter.Pinned },
                        label = { Text("Pin") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = hideDone,
                    onClick = {
                        hideDone = !hideDone
                        scope.makeIOCall {
                            TasksPrefs.setTaskDrawerHideDone(context, hideDone)
                        }
                    },
                    label = { Text("Sakrij gotovo") }
                )

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    FilterChip(
                        selected = false,
                        onClick = { showSortMenu = true },
                        label = {
                            val label = when (sort) {
                                TaskSort.NEWEST -> "Sort: Novo"
                                TaskSort.OLDEST -> "Sort: Staro"
                                TaskSort.AZ -> "Sort: A-Z"
                                TaskSort.MANUAL -> "Sort: Rucno"
                            }
                            Text(label)
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = Strings.sortMenu
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        fun setSort(s: TaskSort) {
                            sort = s
                            scope.makeIOCall {
                                TasksPrefs.setTaskDrawerSort(context, sort.name)
                            }
                        }
                        DropdownMenuItem(
                            text = { Text("Novo") },
                            onClick = { showSortMenu = false; setSort(s = TaskSort.NEWEST) }
                        )
                        DropdownMenuItem(
                            text = { Text("Staro") },
                            onClick = { showSortMenu = false; setSort(s = TaskSort.OLDEST) }
                        )
                        DropdownMenuItem(
                            text = { Text("A-Z") },
                            onClick = { showSortMenu = false; setSort(s = TaskSort.AZ) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rucno") },
                            onClick = { showSortMenu = false; setSort(s = TaskSort.MANUAL) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Spacer(modifier = Modifier.height(6.dp))

            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val msg = when {
                        tasks.isEmpty() -> "Nema taskova. Dodaj prvi zadatak."
                        filter == TaskFilter.Active -> "Nema aktivnih taskova."
                        filter == TaskFilter.Done -> "Nema gotovih taskova."
                        filter == TaskFilter.Pinned -> "Nema pinned taskova."
                        else -> "Nema taskova."
                    }
                    Text(msg, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pinnedList.isNotEmpty() && filter != TaskFilter.Done) {
                        item(key = "hdr_pinned") {
                            DrawerSectionHeader(
                                title = "Pinned",
                                count = pinnedList.size,
                                expanded = pinnedExpanded,
                                onToggle = { pinnedExpanded = !pinnedExpanded }
                            )
                        }
                        if (pinnedExpanded) {
                            itemsIndexed(pinnedList, key = { _, t -> t.id }) { idx, task ->
                                SwipeToDismissTaskRow(
                                    task = task,
                                    showManualMove = sort == TaskSort.MANUAL,
                                    canMoveUp = idx > 0,
                                    canMoveDown = idx < pinnedList.lastIndex,
                                    onMoveUp = { onMoveTask(task.id, true) },
                                    onMoveDown = { onMoveTask(task.id, false) },
                                    onTogglePin = { onTogglePinned(task.id) },
                                    onEdit = {
                                        editTaskId = task.id
                                        editTitle = task.title
                                    },
                                    onToggleDone = { done -> onToggleTask(task.id, done) },
                                    onDelete = {
                                        val removedIndex = tasks.indexOfFirst { it.id == task.id }
                                            .coerceAtLeast(0)
                                        onDeleteTask(task.id)
                                        scope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = "Obrisano: ${task.title}",
                                                actionLabel = "UNDO"
                                            )
                                            if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                onRestoreTasks(removedIndex, listOf(task))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (unpinnedList.isNotEmpty() && filter != TaskFilter.Done) {
                        item(key = "hdr_other") {
                            DrawerSectionHeader(
                                title = "Ostalo",
                                count = unpinnedList.size,
                                expanded = true,
                                onToggle = null
                            )
                        }
                        itemsIndexed(unpinnedList, key = { _, t -> t.id }) { idx, task ->
                            SwipeToDismissTaskRow(
                                task = task,
                                showManualMove = sort == TaskSort.MANUAL,
                                canMoveUp = idx > 0,
                                canMoveDown = idx < unpinnedList.lastIndex,
                                onMoveUp = { onMoveTask(task.id, true) },
                                onMoveDown = { onMoveTask(task.id, false) },
                                onTogglePin = { onTogglePinned(task.id) },
                                onEdit = {
                                    editTaskId = task.id
                                    editTitle = task.title
                                },
                                onToggleDone = { done -> onToggleTask(task.id, done) },
                                onDelete = {
                                    val removedIndex = tasks.indexOfFirst { it.id == task.id }
                                        .coerceAtLeast(0)
                                    onDeleteTask(task.id)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Obrisano: ${task.title}",
                                            actionLabel = "UNDO"
                                        )
                                        if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            onRestoreTasks(removedIndex, listOf(task))
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (doneList.isNotEmpty()) {
                        item(key = "hdr_done") {
                            DrawerSectionHeader(
                                title = "Gotovo",
                                count = doneList.size,
                                expanded = doneExpanded || filter == TaskFilter.Done,
                                onToggle = if (filter == TaskFilter.Done) null else {
                                    { doneExpanded = !doneExpanded }
                                }
                            )
                        }
                        if (filter == TaskFilter.Done || doneExpanded) {
                            itemsIndexed(doneList, key = { _, t -> t.id }) { _, task ->
                                SwipeToDismissTaskRow(
                                    task = task,
                                    showManualMove = false,
                                    canMoveUp = false,
                                    canMoveDown = false,
                                    onMoveUp = {},
                                    onMoveDown = {},
                                    onTogglePin = { onTogglePinned(task.id) },
                                    onEdit = {
                                        editTaskId = task.id
                                        editTitle = task.title
                                    },
                                    onToggleDone = { done -> onToggleTask(task.id, done) },
                                    onDelete = {
                                        val removedIndex = tasks.indexOfFirst { it.id == task.id }
                                            .coerceAtLeast(0)
                                        onDeleteTask(task.id)
                                        scope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = "Obrisano: ${task.title}",
                                                actionLabel = "UNDO"
                                            )
                                            if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                onRestoreTasks(removedIndex, listOf(task))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearCompletedConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCompletedConfirm = false },
            title = { Text("Obrisati gotove taskove?") },
            text = { Text("Ova akcija brise sve gotove taskove. Mozes ih vratiti odmah preko UNDO poruke.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCompletedConfirm = false
                        val removed = onClearCompleted()
                        if (removed != null) {
                            val (index, items) = removed
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "Obrisano: ${items.size}",
                                    actionLabel = "UNDO"
                                )
                                if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                    onRestoreTasks(index, items)
                                }
                            }
                        }
                    }
                ) { Text("Obrisi") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedConfirm = false }) { Text("Odustani") }
            }
        )
    }

    val currentEditId = editTaskId
    if (currentEditId != null) {
        AlertDialog(
            onDismissRequest = { editTaskId = null },
            title = { Text("Uredi task") },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Naslov") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = editTitle.trim()
                        if (t.isNotEmpty()) onUpdateTaskTitle(currentEditId, t)
                        editTaskId = null
                    }
                ) { Text("Spremi") }
            },
            dismissButton = {
                TextButton(onClick = { editTaskId = null }) { Text("Odustani") }
            }
        )
    }
}

@Composable
private fun DrawerSectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: (() -> Unit)?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title ($count)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (onToggle != null) {
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
        }
    }
}

internal enum class TaskFilter { All, Active, Done, Pinned }
internal enum class TaskSort { NEWEST, OLDEST, AZ, MANUAL }

@Composable
private fun SwipeToDismissTaskRow(
    task: TaskItem,
    showManualMove: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.35f }
    )

    var firedValue by remember(task.id) { mutableStateOf<SwipeToDismissBoxValue?>(null) }
    var rowMenuOpen by remember(task.id) { mutableStateOf(false) }

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                if (firedValue != SwipeToDismissBoxValue.StartToEnd) {
                    firedValue = SwipeToDismissBoxValue.StartToEnd
                    onToggleDone(!task.isDone)
                }
                dismissState.reset()
            }
            SwipeToDismissBoxValue.EndToStart -> {
                if (firedValue != SwipeToDismissBoxValue.EndToStart) {
                    firedValue = SwipeToDismissBoxValue.EndToStart
                    onDelete()
                }
                dismissState.reset()
            }
            SwipeToDismissBoxValue.Settled -> firedValue = null
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val value = dismissState.targetValue
                if (value == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (task.isDone) "Vrati" else "Gotovo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (value == SwipeToDismissBoxValue.EndToStart) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        content = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onToggleDone(!task.isDone) },
                            onLongClick = onEdit
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isDone,
                        onCheckedChange = { onToggleDone(!task.isDone) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pin task",
                            tint = if (task.pinned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showManualMove) {
                        IconButton(onClick = onMoveUp) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                        }
                        IconButton(onClick = onMoveDown) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                        }
                    }
                    Box {
                        IconButton(onClick = { rowMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Task menu")
                        }
                        DropdownMenu(
                            expanded = rowMenuOpen,
                            onDismissRequest = { rowMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Uredi") },
                                onClick = { rowMenuOpen = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (task.isDone) "Vrati" else "Oznaci kao gotovo") },
                                onClick = { rowMenuOpen = false; onToggleDone(!task.isDone) },
                                leadingIcon = { Icon(Icons.Default.Done, contentDescription = null) }
                            )
                            if (showManualMove) {
                                DropdownMenuItem(
                                    text = { Text("Pomjeri gore") },
                                    enabled = canMoveUp,
                                    onClick = { rowMenuOpen = false; onMoveUp() },
                                    leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pomjeri dole") },
                                    enabled = canMoveDown,
                                    onClick = { rowMenuOpen = false; onMoveDown() },
                                    leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Obrisi") },
                                onClick = { rowMenuOpen = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    )
}
