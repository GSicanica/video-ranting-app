package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.cache.GospelCache
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.GospelDayResponse
import com.youtube.rating.shared.models.RandomPassageResponse
import com.youtube.rating.shared.utils.RequestDeduplicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BiblePlannerNetworkState(
    val randomLoading: Boolean = false,
    val randomError: String? = null,
    val random: RandomPassageResponse? = null,
    val gospelLoading: Boolean = false,
    val gospelError: String? = null,
    val gospel: GospelDayResponse? = null
)

class BiblePlannerViewModel(
    private val apiClient: RatingApiClient,
    private val gospelCache: GospelCache
) : ViewModel() {

    private val requestDeduplicator = RequestDeduplicator()

    private val _state = MutableStateFlow(BiblePlannerNetworkState())
    val state: StateFlow<BiblePlannerNetworkState> = _state

    private var lastGospelDate: String? = null
    private var gospelJob: Job? = null

    fun loadRandomPassage() {
        if (_state.value.randomLoading) return

        viewModelScope.launch {
            val requestKey = "randomPassage"
            
            // Spriječi simultane duplikate
            if (!requestDeduplicator.shouldExecute(requestKey)) return@launch
            
            _state.value = _state.value.copy(randomLoading = true, randomError = null)
            try {
                val resp = apiClient.getRandomPassage()
                if (resp.success) {
                    _state.value = _state.value.copy(random = resp)
                } else {
                    _state.value = _state.value.copy(randomError = resp.message ?: "Greska pri ucitavanju.")
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _state.value = _state.value.copy(randomError = e.message ?: "Greska pri ucitavanju.")
            } finally {
                _state.value = _state.value.copy(randomLoading = false)
                requestDeduplicator.markComplete(requestKey)
            }
        }
    }

    fun loadGospelOfDay(date: String? = null, force: Boolean = false) {
        if (_state.value.gospelLoading) return
        if (!force && _state.value.gospel != null && lastGospelDate == date) return
        gospelJob?.cancel()
        gospelJob = viewModelScope.launch {
            _state.value = _state.value.copy(gospelLoading = true, gospelError = null)
            try {
                val resp = gospelCache.getGospel(date, force)
                if (resp.success) {
                    lastGospelDate = date
                    _state.value = _state.value.copy(gospel = resp)
                } else {
                    val msg = resp.message ?: "Greska pri dohvacanju."
                    if (msg.contains("not found", ignoreCase = true)) {
                        _state.value = _state.value.copy(gospel = null, gospelError = null)
                    } else {
                        _state.value = _state.value.copy(gospelError = msg)
                    }
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _state.value = _state.value.copy(gospelError = e.message ?: "Greska pri dohvacanju.")
            } finally {
                _state.value = _state.value.copy(gospelLoading = false)
            }
        }
    }
}

class BiblePlannerViewModelFactory(
    private val apiClient: RatingApiClient,
    private val gospelCache: GospelCache
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BiblePlannerViewModel::class.java)) {
            return BiblePlannerViewModel(apiClient = apiClient, gospelCache = gospelCache) as T
        }
        val e = IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e,
            tags = mapOf("where" to "BiblePlannerViewModelFactory.create")
        )
        error(e.message ?: "Unknown ViewModel class")
    }
}
