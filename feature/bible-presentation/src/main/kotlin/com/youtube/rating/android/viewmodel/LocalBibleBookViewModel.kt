package com.youtube.rating.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.presentation.state.ResultState
import com.youtube.rating.android.utils.LocalBibleRepository
import com.youtube.rating.core.coroutines.makeIOCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalBibleBookViewModel : ViewModel() {
    private val _result = MutableStateFlow<ResultState<List<String>>>(ResultState.Loading)
    val result: StateFlow<ResultState<List<String>>> = _result.asStateFlow()

    fun load(context: Context, bookId: String) {
        _result.value = ResultState.Loading
        makeIOCall {
            try {
                val chapters = LocalBibleRepository.getChapters(context, bookId)
                _result.value = ResultState.Success(chapters)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _result.value = ResultState.Error(e.message ?: Strings.errorLoadingDot)
            }
        }
    }
}

