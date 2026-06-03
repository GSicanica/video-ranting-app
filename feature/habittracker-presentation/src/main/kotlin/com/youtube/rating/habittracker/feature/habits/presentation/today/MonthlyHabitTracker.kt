package com.youtube.rating.habittracker.feature.habits.presentation.today

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Serializable
private data class MonthlyGoalItem(
    val id: Long,
    val title: String,
    val checkedDays: Set<Int> = emptySet()
)

@Serializable
private enum class MonthlyMood(
    val label: String,
    val emoji: String
) {
    Happy("Happy", "😊"),
    Relaxed("Relaxed", "😌"),
    Neutral("Neutral", "😐"),
    Sad("Sad", "😞"),
    Stressed("Stressed", "😖");

    fun nextOrNull(): MonthlyMood? {
        return when (this) {
            Happy -> Relaxed
            Relaxed -> Neutral
            Neutral -> Sad
            Sad -> Stressed
            Stressed -> null
        }
    }
}

@Serializable
private enum class TrackerSection(
    val displayTitle: String
) {
    Header("Header"),
    AddGoal("Add Goal"),
    Stats("Stats"),
    Graph("Daily Graph"),
    DailyTracker("Daily Tracker"),
    GoalProgress("Goal Progress"),
    Mood("Mood Tracker")
}

@Serializable
private data class MonthlyTrackerState(
    val goals: List<MonthlyGoalItem> = defaultGoals(),
    val moodByDay: Map<Int, MonthlyMood> = emptyMap(),
    val sectionOrder: List<TrackerSection> = defaultSectionOrder()
)

@Serializable
private data class MonthlyTrackerRootState(
    val selectedMonth: String = YearMonth.now().storageKey(),
    val statesByMonth: Map<String, MonthlyTrackerState> = emptyMap()
)

private data class MonthlyStats(
    val totalCompleted: Int,
    val totalPossible: Int,
    val monthlyPercent: Float,
    val completedPerDay: List<Int>,
    val notCompletedPerDay: List<Int>,
    val percentPerDay: List<Float>,
    val percentPerGoal: Map<Long, Float>
)

private val GoalGridTitleWidth = 188.dp
private val GoalGridDayWidth = 30.dp
private val GoalGridHeaderHeight = 26.dp
private val GoalGridRowHeight = 34.dp
private val GoalGridCheckSize = 20.dp

private fun defaultGoals(): List<MonthlyGoalItem> {
    return listOf(
        MonthlyGoalItem(
            id = 1L,
            title = "Čitati Bibliju 5 minuta",
            checkedDays = setOf(1)
        ),
        MonthlyGoalItem(
            id = 2L,
            title = "Ne tipkati u vožnji"
        )
    )
}

private fun defaultSectionOrder(): List<TrackerSection> {
    return listOf(
        TrackerSection.Header,
        TrackerSection.AddGoal,
        TrackerSection.Stats,
        TrackerSection.Graph,
        TrackerSection.DailyTracker,
        TrackerSection.GoalProgress,
        TrackerSection.Mood
    )
}

private fun normalizeSectionOrder(
    savedOrder: List<TrackerSection>
): List<TrackerSection> {
    val defaultOrder = defaultSectionOrder()
    val cleaned = savedOrder.filter { section -> section in defaultOrder }

    return cleaned + defaultOrder.filter { section -> section !in cleaned }
}

private fun YearMonth.storageKey(): String = toString()

private fun String.toYearMonthOrNull(): YearMonth? {
    return runCatching {
        YearMonth.parse(this)
    }.getOrNull()
}

private fun daysInMonth(month: YearMonth): List<Int> {
    return (1..month.lengthOfMonth()).toList()
}

private fun calculateStats(
    goals: List<MonthlyGoalItem>,
    days: List<Int>
): MonthlyStats {
    val validDays = days.toSet()
    val totalGoals = goals.size

    val completedPerDay = days.map { day ->
        goals.count { goal -> day in goal.checkedDays }
    }

    val notCompletedPerDay = completedPerDay.map { completed ->
        totalGoals - completed
    }

    val percentPerDay = completedPerDay.map { completed ->
        if (totalGoals == 0) 0f else completed.toFloat() / totalGoals
    }

    val totalCompleted = goals.sumOf { goal ->
        goal.checkedDays.count { day -> day in validDays }
    }

    val totalPossible = goals.size * days.size

    val monthlyPercent = if (totalPossible == 0) {
        0f
    } else {
        totalCompleted.toFloat() / totalPossible
    }

    val percentPerGoal = goals.associate { goal ->
        val checked = goal.checkedDays.count { day -> day in validDays }

        val percent = if (days.isEmpty()) {
            0f
        } else {
            checked.toFloat() / days.size
        }

        goal.id to percent
    }

    return MonthlyStats(
        totalCompleted = totalCompleted,
        totalPossible = totalPossible,
        monthlyPercent = monthlyPercent,
        completedPerDay = completedPerDay,
        notCompletedPerDay = notCompletedPerDay,
        percentPerDay = percentPerDay,
        percentPerGoal = percentPerGoal
    )
}

private object MonthlyTrackerStorage {
    private const val PREFS = "monthly_habit_tracker_prefs"
    private const val KEY_STATE = "monthly_habit_tracker_state_v4"
    private const val KEY_ROOT_STATE = "monthly_habit_tracker_root_state_v1"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadRoot(
        context: Context,
        initialMonth: YearMonth
    ): MonthlyTrackerRootState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val rootRaw = prefs.getString(KEY_ROOT_STATE, null)
        if (rootRaw != null) {
            return runCatching {
                json.decodeFromString<MonthlyTrackerRootState>(rootRaw)
            }.getOrDefault(
                MonthlyTrackerRootState(selectedMonth = initialMonth.storageKey())
            )
        }

        val legacyState = loadLegacyState(context)
        return MonthlyTrackerRootState(
            selectedMonth = initialMonth.storageKey(),
            statesByMonth = mapOf(initialMonth.storageKey() to legacyState)
        )
    }

    private fun loadLegacyState(context: Context): MonthlyTrackerState {
        val raw = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STATE, null)
            ?: return MonthlyTrackerState()

        return runCatching {
            json.decodeFromString<MonthlyTrackerState>(raw)
        }.getOrDefault(MonthlyTrackerState())
    }

    fun saveRoot(
        context: Context,
        state: MonthlyTrackerRootState
    ) {
        val raw = json.encodeToString(state)

        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROOT_STATE, raw)
            .apply()
    }
}

@Composable
fun MonthlyHabitTracker(
    modifier: Modifier = Modifier,
    month: YearMonth = YearMonth.now()
) {
    val context = LocalContext.current

    var selectedMonth by remember {
        mutableStateOf(month)
    }

    var statesByMonth by remember {
        mutableStateOf<Map<String, MonthlyTrackerState>>(emptyMap())
    }

    val days = remember(selectedMonth) {
        daysInMonth(selectedMonth)
    }

    val goals = remember {
        mutableStateListOf<MonthlyGoalItem>()
    }

    val moodByDay = remember {
        mutableStateMapOf<Int, MonthlyMood>()
    }

    val sectionOrder = remember {
        mutableStateListOf<TrackerSection>()
    }

    var newGoalText by remember {
        mutableStateOf("")
    }

    var goalToDelete by remember {
        mutableStateOf<MonthlyGoalItem?>(null)
    }

    var showMonthControls by remember {
        mutableStateOf(false)
    }

    var hasLoaded by remember {
        mutableStateOf(false)
    }

    fun stateFromUi(monthForState: YearMonth): MonthlyTrackerState {
        val validDays = daysInMonth(monthForState).toSet()
        return MonthlyTrackerState(
                goals = goals.map { goal ->
                    goal.copy(
                        checkedDays = goal.checkedDays
                            .filter { day -> day in validDays }
                            .toSet()
                    )
                },
                moodByDay = moodByDay.filterKeys { day -> day in validDays },
                sectionOrder = sectionOrder.toList()
            )
    }

    fun applyStateForMonth(
        targetMonth: YearMonth,
        savedStates: Map<String, MonthlyTrackerState>
    ) {
        val targetDays = daysInMonth(targetMonth).toSet()
        val state = savedStates[targetMonth.storageKey()] ?: MonthlyTrackerState()

        goals.clear()
        goals.addAll(
            state.goals.map { goal ->
                goal.copy(
                    checkedDays = goal.checkedDays
                        .filter { day -> day in targetDays }
                        .toSet()
                )
            }
        )

        moodByDay.clear()
        moodByDay.putAll(
            state.moodByDay.filterKeys { day -> day in targetDays }
        )

        sectionOrder.clear()
        sectionOrder.addAll(
            normalizeSectionOrder(state.sectionOrder)
        )
    }

    fun persist(selectedMonthToSave: YearMonth = selectedMonth): Map<String, MonthlyTrackerState> {
        if (!hasLoaded) return statesByMonth

        val updatedStates = statesByMonth + (selectedMonth.storageKey() to stateFromUi(selectedMonth))
        statesByMonth = updatedStates

        MonthlyTrackerStorage.saveRoot(
            context = context,
            state = MonthlyTrackerRootState(
                selectedMonth = selectedMonthToSave.storageKey(),
                statesByMonth = updatedStates
            )
        )

        return updatedStates
    }

    fun changeMonth(targetMonth: YearMonth) {
        val updatedStates = persist(selectedMonthToSave = targetMonth)
        selectedMonth = targetMonth
        applyStateForMonth(
            targetMonth = targetMonth,
            savedStates = updatedStates
        )
    }

    LaunchedEffect(Unit) {
        val saved = MonthlyTrackerStorage.loadRoot(
            context = context,
            initialMonth = month
        )
        val loadedMonth = saved.selectedMonth.toYearMonthOrNull() ?: month

        statesByMonth = saved.statesByMonth
        selectedMonth = loadedMonth
        applyStateForMonth(
            targetMonth = loadedMonth,
            savedStates = saved.statesByMonth
        )
        hasLoaded = true
    }

    val stats = calculateStats(
        goals = goals,
        days = days
    )

    if (goalToDelete != null) {
        DeleteGoalDialog(
            goalTitle = goalToDelete?.title.orEmpty(),
            onDismiss = {
                goalToDelete = null
            },
            onConfirm = {
                goals.remove(goalToDelete)
                goalToDelete = null
                persist()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TrackerColors.Background,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        ReorderableSections(
            sections = sectionOrder,
            onOrderChanged = {
                persist()
            }
        ) { section ->
            when (section) {
                TrackerSection.Header -> {
                    HeaderCard(
                        month = selectedMonth,
                        stats = stats,
                        showMonthControls = showMonthControls,
                        onHeaderClick = {
                            showMonthControls = !showMonthControls
                        },
                        onPreviousMonthClick = {
                            changeMonth(selectedMonth.minusMonths(1))
                        },
                        onCurrentMonthClick = {
                            changeMonth(YearMonth.now())
                        },
                        onNextMonthClick = {
                            changeMonth(selectedMonth.plusMonths(1))
                        }
                    )
                }

                TrackerSection.AddGoal -> {
                    AddGoalCard(
                        value = newGoalText,
                        onValueChange = {
                            newGoalText = it
                        },
                        onAddClick = {
                            val normalized = newGoalText.trim()

                            if (normalized.isNotEmpty()) {
                                goals.add(
                                    MonthlyGoalItem(
                                        id = System.currentTimeMillis(),
                                        title = normalized
                                    )
                                )

                                newGoalText = ""
                                persist()
                            }
                        }
                    )
                }

                TrackerSection.Stats -> {
                    StatsRow(
                        goals = goals,
                        days = days,
                        stats = stats,
                        moodByDay = moodByDay
                    )
                }

                TrackerSection.Graph -> {
                    DailyCompletionGraphCard(
                        days = days,
                        stats = stats
                    )
                }

                TrackerSection.DailyTracker -> {
                    GoalGridCard(
                        days = days,
                        goals = goals,
                        onToggleDay = { goalIndex, day ->
                            val goal = goals[goalIndex]

                            goals[goalIndex] = goal.copy(
                                checkedDays = if (day in goal.checkedDays) {
                                    goal.checkedDays - day
                                } else {
                                    goal.checkedDays + day
                                }
                            )

                            persist()
                        },
                        onDeleteGoal = { goal ->
                            goalToDelete = goal
                        }
                    )
                }

                TrackerSection.GoalProgress -> {
                    GoalProgressCard(
                        goals = goals,
                        days = days,
                        stats = stats
                    )
                }

                TrackerSection.Mood -> {
                    MoodCard(
                        days = days,
                        moodByDay = moodByDay,
                        onMoodClick = { day ->
                            val currentMood = moodByDay[day]
                            val nextMood = currentMood?.nextOrNull() ?: MonthlyMood.Happy

                            if (currentMood == MonthlyMood.Stressed) {
                                moodByDay.remove(day)
                            } else {
                                moodByDay[day] = nextMood
                            }

                            persist()
                        }
                    )
                }
            }
        }
    }
}



@Composable
private fun ReorderableSections(
    sections: SnapshotStateList<TrackerSection>,
    onOrderChanged: () -> Unit,
    content: @Composable (TrackerSection) -> Unit
) {
    val density = LocalDensity.current
    val moveThresholdPx = with(density) { 120.dp.toPx() }

    var draggingSection by remember {
        mutableStateOf<TrackerSection?>(null)
    }

    var dragOffsetY by remember {
        mutableStateOf(0f)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        sections.forEach { section ->
            val isDragging = draggingSection == section

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        if (isDragging) {
                            IntOffset(
                                x = 0,
                                y = dragOffsetY.roundToInt()
                            )
                        } else {
                            IntOffset.Zero
                        }
                    }
                    .pointerInput(section, sections.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingSection = section
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggingSection = null
                                dragOffsetY = 0f
                            },
                            onDragEnd = {
                                draggingSection = null
                                dragOffsetY = 0f
                                onOrderChanged()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                if (draggingSection != section) return@detectDragGesturesAfterLongPress

                                dragOffsetY += dragAmount.y

                                val currentIndex = sections.indexOf(section)

                                if (
                                    dragOffsetY > moveThresholdPx &&
                                    currentIndex < sections.lastIndex
                                ) {
                                    sections.move(
                                        from = currentIndex,
                                        to = currentIndex + 1
                                    )
                                    dragOffsetY -= moveThresholdPx
                                    onOrderChanged()
                                }

                                if (
                                    dragOffsetY < -moveThresholdPx &&
                                    currentIndex > 0
                                ) {
                                    sections.move(
                                        from = currentIndex,
                                        to = currentIndex - 1
                                    )
                                    dragOffsetY += moveThresholdPx
                                    onOrderChanged()
                                }
                            }
                        )
                    }
            ) {
                DragContainer(
                    title = section.displayTitle,
                    isDragging = isDragging
                ) {
                    content(section)
                }
            }
        }
    }
}

private fun <T> SnapshotStateList<T>.move(
    from: Int,
    to: Int
) {
    if (from == to) return
    if (from !in indices) return
    if (to !in indices) return

    val item = removeAt(from)
    add(to, item)
}

@Composable
private fun DragContainer(
    title: String,
    isDragging: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (isDragging) 2.dp else 0.dp,
                color = if (isDragging) TrackerColors.Primary else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                color = if (isDragging) {
                    TrackerColors.DragBackground
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(24.dp)
            )
            .padding(if (isDragging) 6.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "☰ $title",
                color = TrackerColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )

            if (isDragging) {
                Text(
                    text = "Moving...",
                    color = TrackerColors.Primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        content()
    }
}

@Composable
private fun HeaderCard(
    month: YearMonth,
    stats: MonthlyStats,
    showMonthControls: Boolean,
    onHeaderClick: () -> Unit,
    onPreviousMonthClick: () -> Unit,
    onCurrentMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Monthly Habit Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TrackerColors.TextPrimary
                    )

                    Text(
                        text = month.displayName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TrackerColors.TextSecondary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${(stats.monthlyPercent * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TrackerColors.Primary
                    )

                    Text(
                        text = "${stats.totalCompleted}/${stats.totalPossible} completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrackerColors.TextSecondary
                    )
                }
            }

            if (showMonthControls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPreviousMonthClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Prethodni")
                    }

                    TextButton(
                        onClick = onCurrentMonthClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Danas")
                    }

                    OutlinedButton(
                        onClick = onNextMonthClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sljedeci")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddGoalCard(
    value: String,
    onValueChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text("Dodaj novi goal, npr. Trening 20 minuta")
                }
            )

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun StatsRow(
    goals: List<MonthlyGoalItem>,
    days: List<Int>,
    stats: MonthlyStats,
    moodByDay: Map<Int, MonthlyMood>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Goals",
            value = goals.size.toString(),
            subtitle = "active habits"
        )

        StatCard(
            modifier = Modifier.weight(1f),
            title = "Days",
            value = days.size.toString(),
            subtitle = "this month"
        )

        StatCard(
            modifier = Modifier.weight(1f),
            title = "Completed",
            value = stats.totalCompleted.toString(),
            subtitle = "total checks"
        )

        StatCard(
            modifier = Modifier.weight(1f),
            title = "Mood",
            value = moodByDay.size.toString(),
            subtitle = "days tracked"
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TrackerColors.TextSecondary
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TrackerColors.TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TrackerColors.TextSecondary
            )
        }
    }
}

@Composable
private fun DailyCompletionGraphCard(
    days: List<Int>,
    stats: MonthlyStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            DailyCompletionChart(
                values = stats.percentPerDay.map { percent -> percent * 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    GraphSummaryRow(
                        title = "Day",
                        values = days.map { it.toString() }
                    )

                    GraphSummaryRow(
                        title = "Completed",
                        values = stats.completedPerDay.map { it.toString() }
                    )

                    GraphSummaryRow(
                        title = "Not Completed",
                        values = stats.notCompletedPerDay.map { it.toString() }
                    )

                    GraphSummaryRow(
                        title = "% Completed",
                        values = stats.percentPerDay.map { percent ->
                            "${(percent * 100).roundToInt()}%"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyCompletionChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TrackerColors.ChartBackground)
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = 8.dp,
                bottom = 6.dp
            )
    ) {
        if (values.isEmpty()) return@Canvas

        val leftPadding = 42f
        val rightPadding = 14f
        val topPadding = 12f
        val bottomPadding = 20f

        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding
        val bottomY = topPadding + chartHeight

        for (i in 0..5) {
            val percent = i * 20
            val y = bottomY - chartHeight * (percent / 100f)

            drawLine(
                color = TrackerColors.Divider,
                start = Offset(leftPadding, y),
                end = Offset(size.width - rightPadding, y),
                strokeWidth = 1f
            )
        }

        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) {
                leftPadding
            } else {
                leftPadding + index * chartWidth / (values.size - 1)
            }

            val y = bottomY - chartHeight * value.coerceIn(0f, 100f) / 100f

            Offset(x, y)
        }

        points.forEach { point ->
            drawLine(
                color = TrackerColors.Primary.copy(alpha = 0.24f),
                start = Offset(point.x, bottomY),
                end = point,
                strokeWidth = 10f
            )

            drawLine(
                color = TrackerColors.Primary,
                start = Offset(point.x, bottomY),
                end = point,
                strokeWidth = 4f
            )

            drawCircle(
                color = TrackerColors.Primary,
                radius = 4.5f,
                center = point
            )

            drawCircle(
                color = Color.White,
                radius = 2f,
                center = point
            )
        }
    }
}

@Composable
private fun GraphSummaryRow(
    title: String,
    values: List<String>
) {
    Row(
        modifier = Modifier.height(30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TrackerColors.TextSecondary
            )
        }

        values.forEach { value ->
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TrackerColors.CellBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall,
                    color = TrackerColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun GoalGridCard(
    days: List<Int>,
    goals: SnapshotStateList<MonthlyGoalItem>,
    onToggleDay: (goalIndex: Int, day: Int) -> Unit,
    onDeleteGoal: (MonthlyGoalItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.width(GoalGridTitleWidth)
                        )

                        days.forEach { day ->
                            DayHeaderCell(day = day)
                        }
                    }

                    Divider(
                        color = TrackerColors.Divider
                    )

                    goals.forEachIndexed { goalIndex, goal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoalTitleCell(
                                title = goal.title,
                                onDeleteClick = {
                                    onDeleteGoal(goal)
                                }
                            )

                            days.forEach { day ->
                                val checked = day in goal.checkedDays

                                DayCheckCell(
                                    checked = checked,
                                    onClick = {
                                        onToggleDay(goalIndex, day)
                                    }
                                )
                            }
                        }
                    }

                    if (goals.isEmpty()) {
                        EmptyGoalsMessage()
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeaderCell(
    day: Int
) {
    Box(
        modifier = Modifier
            .width(GoalGridDayWidth)
            .height(GoalGridHeaderHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = TrackerColors.TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GoalTitleCell(
    title: String,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(GoalGridTitleWidth)
            .height(GoalGridRowHeight)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = TrackerColors.TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        TextButton(
            onClick = onDeleteClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Delete",
                color = TrackerColors.Danger,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DayCheckCell(
    checked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(GoalGridDayWidth)
            .height(GoalGridRowHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(GoalGridCheckSize)
                .clip(CircleShape)
                .background(
                    if (checked) {
                        TrackerColors.Primary
                    } else {
                        TrackerColors.CellBackground
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (checked) {
                        TrackerColors.Primary
                    } else {
                        TrackerColors.Border
                    },
                    shape = CircleShape
                )
                .clickable {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyGoalsMessage() {
    Box(
        modifier = Modifier
            .widthIn(min = 600.dp)
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nema još goalova. Dodaj prvi goal iznad.",
            color = TrackerColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun GoalProgressCard(
    goals: List<MonthlyGoalItem>,
    days: List<Int>,
    stats: MonthlyStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (goals.isEmpty()) {
                Text(
                    text = "Dodaj goal da vidiš progress.",
                    color = TrackerColors.TextSecondary
                )
            } else {
                goals.forEach { goal ->
                    val checked = goal.checkedDays.count { day -> day in days }
                    val percent = stats.percentPerGoal[goal.id] ?: 0f

                    GoalProgressItem(
                        title = goal.title,
                        completed = checked,
                        total = days.size,
                        percent = percent
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressItem(
    title: String,
    completed: Int,
    total: Int,
    percent: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = TrackerColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "$completed/$total • ${(percent * 100).roundToInt()}%",
                color = TrackerColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        LinearProgressIndicator(
            progress = percent.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp)),
            color = TrackerColors.Primary,
            trackColor = TrackerColors.ProgressTrack
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodCard(
    days: List<Int>,
    moodByDay: SnapshotStateMap<Int, MonthlyMood>,
    onMoodClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                days.forEach { day ->
                    MoodDayCell(
                        day = day,
                        mood = moodByDay[day],
                        onClick = {
                            onMoodClick(day)
                        }
                    )
                }
            }

            Divider(
                color = TrackerColors.Divider
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonthlyMood.values().forEach { mood ->
                    MoodLegendItem(mood = mood)
                }
            }
        }
    }
}

@Composable
private fun MoodDayCell(
    day: Int,
    mood: MonthlyMood?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .height(76.dp)
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TrackerColors.CellBackground)
            .border(
                width = 1.dp,
                color = TrackerColors.Border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = TrackerColors.TextSecondary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = mood?.emoji ?: "＋",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun MoodLegendItem(
    mood: MonthlyMood
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(TrackerColors.CellBackground)
            .border(
                width = 1.dp,
                color = TrackerColors.Border,
                shape = RoundedCornerShape(100.dp)
            )
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = mood.emoji
        )

        Text(
            text = mood.label,
            style = MaterialTheme.typography.bodySmall,
            color = TrackerColors.TextPrimary
        )
    }
}

@Composable
private fun DeleteGoalDialog(
    goalTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete goal?")
        },
        text = {
            Text("Želiš obrisati \"$goalTitle\"?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrackerColors.Danger
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun YearMonth.displayName(): String {
    val monthName = month.getDisplayName(
        TextStyle.FULL,
        Locale.getDefault()
    )

    return "$monthName $year"
}

private object TrackerColors {
    val Background = Color(0xFFF4F6FA)
    val ChartBackground = Color(0xFFF8FAFC)
    val CellBackground = Color(0xFFF8FAFC)
    val InfoBackground = Color(0xFFEFF6FF)
    val DragBackground = Color(0xFFEFF6FF)

    val Primary = Color(0xFF2563EB)
    val Danger = Color(0xFFDC2626)

    val TextPrimary = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)

    val Border = Color(0xFFE5E7EB)
    val Divider = Color(0xFFE5E7EB)

    val ProgressTrack = Color(0xFFE5E7EB)
}
