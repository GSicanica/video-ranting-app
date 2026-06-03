package com.youtube.rating.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.presentation.state.ResultState
import com.youtube.rating.android.utils.LocalBibleRepository
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.core.coroutines.makeIOCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalBibleChapterViewModel(
    private val apiClient: RatingApiClient,
) : ViewModel() {
    private val _result = MutableStateFlow<ResultState<String>>(ResultState.Loading)
    val result: StateFlow<ResultState<String>> = _result.asStateFlow()

    fun load(context: Context, bookId: String, chapterFile: String) {
        _result.value = ResultState.Loading

        val safeBook = bookId.removePrefix("22output1/").removePrefix("/")
        val safeChapter = chapterFile.substringAfterLast('/')

        makeIOCall {
            try {
                val local = LocalBibleRepository.readChapterOrNull(context, safeBook, safeChapter)
                if (local != null) {
                    _result.value = ResultState.Success(local)
                } else {
                    val resp = apiClient.getBiblePassage(safeBook, safeChapter.removeSuffix(".md"))
                    if (resp.success) {
                        _result.value = ResultState.Success(resp.text.orEmpty())
                    } else {
                        _result.value = ResultState.Error(resp.message ?: Strings.errorLoadingDot)
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _result.value = ResultState.Error(e.message ?: Strings.errorLoadingDot)
            }
        }
    }
}

