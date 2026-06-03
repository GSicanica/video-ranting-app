package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.cache.GospelCache
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.GospelDayResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GospelOfDayUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val payload: GospelDayResponse? = null
)

class GospelOfDayViewModel(
    private val apiClient: RatingApiClient,
    private val gospelCache: GospelCache
) : ViewModel() {
    private val _state = MutableStateFlow(GospelOfDayUiState())
    val state: StateFlow<GospelOfDayUiState> = _state

    private var lastDate: String? = null
    private var loadJob: Job? = null

    fun load(date: String? = null, force: Boolean = false) {
        if (_state.value.loading) return
        if (!force && _state.value.payload != null && lastDate == date) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val resp = gospelCache.getGospel(date, force)
                if (resp.success) {
                    lastDate = date
                    _state.value = _state.value.copy(payload = resp)
                } else {
                    val msg = resp.message ?: "Greska pri dohvacanju."
                    if (msg.contains("not found", ignoreCase = true)) {
                        _state.value = _state.value.copy(payload = null, error = null)
                    } else {
                        _state.value = _state.value.copy(error = msg)
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _state.value = _state.value.copy(error = e.message ?: "Greska pri dohvacanju.")
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}

class GospelOfDayViewModelFactory(
    private val apiClient: RatingApiClient,
    private val gospelCache: GospelCache
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GospelOfDayViewModel::class.java)) {
            return GospelOfDayViewModel(apiClient = apiClient, gospelCache = gospelCache) as T
        }
        val e = IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e,
            tags = mapOf("where" to "GospelOfDayViewModelFactory.create")
        )
        error(e.message ?: "Unknown ViewModel class")
    }
}
