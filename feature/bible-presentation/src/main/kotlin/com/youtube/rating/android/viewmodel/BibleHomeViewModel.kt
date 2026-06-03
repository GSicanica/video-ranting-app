package com.youtube.rating.android.viewmodel

import com.youtube.rating.core.coroutines.ioDispatcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.data.BibleBooks
import com.youtube.rating.android.localization.UiText
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.youtube.rating.android.data.prefs.BiblePlannerPrefs
import com.youtube.rating.android.data.prefs.BibleReaderPrefs
import com.youtube.rating.android.data.prefs.BibleSequentialPrefs
import com.youtube.rating.android.data.prefs.BibleStatsPrefs
import com.youtube.rating.android.data.prefs.TrainingPrefs
import com.youtube.rating.core.coroutines.makeIOCall

data class BibleHomeUiState(
    val today: String = "",
    val bibleReadToday: Int = 0,
    val thinkingMin: Int = 0,
    val bibleStreak: Int = 0,

    val bibleBookIndex: Int = 0,
    val bibleChapter: Int = 1,

    val goalUnits: Int = 3,

    // derived
    val currentBookName: String? = null,
    val currentBookChapters: Int? = null,
    val seqProgress: Float = 0f,

    val bibleUnits: Int = 0,
    val points: Int = 0,
    val goal: Int = 3,
    val progress: Float = 0f
)

sealed interface BibleHomeEvent {
    data class Message(val text: UiText) : BibleHomeEvent
}

class BibleHomeViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext

    private val _events = MutableSharedFlow<BibleHomeEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val events = _events.asSharedFlow()

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    init {
        ensureDailyCountersOnce()
    }

    private data class Temp(
        val readToday: Int,
        val medMin: Int,
        val streak: Int,
        val bookIndex: Int,
        val chapter: Int
    )

    // ✅ combine 5 flowova -> Temp, pa onda + goal flow
    val uiState: StateFlow<BibleHomeUiState> =
        combine(
            flow = BiblePlannerPrefs.readTodayFlow(context).distinctUntilChanged(),
            flow2 = TrainingPrefs.trainingBibleMeditationMinTodayFlow(context).distinctUntilChanged(),
            flow3 = BiblePlannerPrefs.streakFlow(context).distinctUntilChanged(),
            flow4 = BibleSequentialPrefs.bibleSequentialBookIndexFlow(context).distinctUntilChanged(),
            flow5 = BibleSequentialPrefs.bibleSequentialChapterFlow(context).distinctUntilChanged(),
        ) { readToday, medMin, streak, bookIndex, chapter ->
            Temp(
                readToday = readToday,
                medMin = medMin,
                streak = streak,
                bookIndex = bookIndex,
                chapter = chapter
            )
        }.combine(
            BibleStatsPrefs.bibleDailyGoalUnitsFlow(context).distinctUntilChanged()
        ) { t, goalUnits ->

            val safeRead = t.readToday.coerceAtLeast(0)
            val safeMed = t.medMin.coerceAtLeast(0)
            val safeStreak = t.streak.coerceAtLeast(0)

            val safeBookIndex = t.bookIndex.coerceAtLeast(0)
            val safeChapter = t.chapter.coerceAtLeast(1)

            val safeGoal = goalUnits.coerceIn(1, 20)

            val book = BibleBooks.getBook(safeBookIndex)
            val seq = BibleBooks.calculateProgress(safeBookIndex, safeChapter).coerceIn(0f, 1f)

            val medUnits = safeMed / 10
            val units = maxOf(safeRead, medUnits)

            val points = biblePointsFromReadingAndMeditation(readingArticles = safeRead, meditationMin = safeMed)
            val progress = (units.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)

            BibleHomeUiState(
                today = todayString(),
                bibleReadToday = safeRead,
                thinkingMin = safeMed,
                bibleStreak = safeStreak,
                bibleBookIndex = safeBookIndex,
                bibleChapter = safeChapter,
                goalUnits = safeGoal,

                currentBookName = book?.name,
                currentBookChapters = book?.chapters,
                seqProgress = seq,

                bibleUnits = units,
                points = points,
                goal = safeGoal,
                progress = progress
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BibleHomeUiState(today = todayString())
        )

    fun ensureDailyCountersOnce() {
        makeIOCall {
            TrainingPrefs.ensureDailyCounters(context)
            BibleStatsPrefs.addActiveDay(context)
        }
    }

    fun addRead() {
        makeIOCall {
            BiblePlannerPrefs.incrementReadToday(context, 1)
            BibleStatsPrefs.addBibleReadTotal(context, 1)
        }
    }

    fun subReadIfPossible(currentRead: Int) {
        if (currentRead <= 0) return
        makeIOCall {
            BiblePlannerPrefs.incrementReadToday(context, -1)
            BibleStatsPrefs.addBibleReadTotal(context, -1)
        }
    }

    fun addMeditation(minutes: Int) {
        if (minutes <= 0) return
        makeIOCall {
            TrainingPrefs.incrementTrainingBibleMeditationMinToday(context, minutes)
            BibleStatsPrefs.addBibleMeditationTotal(context, minutes)
        }
    }

    fun subMeditationIfPossible(currentMin: Int, minutes: Int) {
        if (minutes <= 0) return
        if (currentMin < minutes) return
        makeIOCall {
            TrainingPrefs.incrementTrainingBibleMeditationMinToday(context, -minutes)
            BibleStatsPrefs.addBibleMeditationTotal(context, -minutes)
        }
    }

    fun saveGoal(raw: Int?) {
        val goal = (raw ?: uiState.value.goalUnits).coerceIn(1, 20)
        makeIOCall {
            BibleStatsPrefs.setBibleDailyGoalUnits(context, goal)
            _events.tryEmit(BibleHomeEvent.Message(UiText.Dynamic("Spremljeno: cilj = $goal")))
        }
    }

    fun resetSequential() {
        viewModelScope.launch {
            // Postavi trenutnu knjigu kao offline za čitanje
            val currentBook = BibleBooks.getBook(uiState.value.bibleBookIndex)
            if (currentBook != null) {
                withContext(ioDispatcher) {
                    BibleReaderPrefs.setOfflineBibleBook(context, currentBook.id)
                }
            }

            withContext(ioDispatcher) {
                BibleSequentialPrefs.resetBibleSequentialProgress(context)
            }

        }
    }    fun setSequentialProgress(bookIndex: Int, chapter: Int) {
        makeIOCall {
            BibleSequentialPrefs.setBibleSequentialProgress(
                context,
                bookIndex.coerceAtLeast(0),
                chapter.coerceAtLeast(1)
            )
        }
    }

    fun onChapterRead() {
        makeIOCall {
            BiblePlannerPrefs.incrementReadToday(context, 1)
            BibleStatsPrefs.addBibleReadTotal(context, 1)
        }
    }

    fun resetToday(readToday: Int, medMin: Int) {
        makeIOCall {
            val todayKey = todayString()

            if (readToday > 0) BibleStatsPrefs.addBibleReadTotal(context, -readToday)
            if (medMin > 0) BibleStatsPrefs.addBibleMeditationTotal(context, -medMin)

            BiblePlannerPrefs.resetReadToday(context)
            TrainingPrefs.resetTrainingBibleMeditationMinToday(context)
            BibleStatsPrefs.removeActiveDay(context, todayKey)

            _events.tryEmit(BibleHomeEvent.Message(UiText.Dynamic("Danas resetirano.")))
        }
    }
}

private fun biblePointsFromReadingAndMeditation(readingArticles: Int, meditationMin: Int): Int {
    val read = readingArticles.coerceAtLeast(0)
    val med = meditationMin.coerceAtLeast(0)

    val units = maxOf(read, med / 10)

    val base = units * 10
    val balanceBonus = if (read > 0 && med >= 10) 12 else 0
    val depthBonus = ((units - 2).coerceAtLeast(0) / 2) * 6
    val startBonus = if (read > 0 || med >= 10) 8 else 0

    return base + balanceBonus + depthBonus + startBonus
}