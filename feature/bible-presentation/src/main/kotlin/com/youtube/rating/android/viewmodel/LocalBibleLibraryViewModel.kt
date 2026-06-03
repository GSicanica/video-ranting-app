package com.youtube.rating.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.presentation.state.ResultState
import com.youtube.rating.android.utils.LocalBibleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.youtube.rating.core.coroutines.makeIOCall

class LocalBibleLibraryViewModel : ViewModel() {
    private val _result = MutableStateFlow<ResultState<List<String>>>(ResultState.Loading)
    val result: StateFlow<ResultState<List<String>>> = _result.asStateFlow()

    fun load(context: Context) {
        _result.value = ResultState.Loading
        makeIOCall {
            try {
                LocalBibleRepository.preloadIndex(context)
                val entries = LocalBibleRepository.getBooks(context)
                _result.value = ResultState.Success(entries)
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _result.value = ResultState.Error(e.message ?: Strings.errorLoadingDot)
            }
        }
    }
}
