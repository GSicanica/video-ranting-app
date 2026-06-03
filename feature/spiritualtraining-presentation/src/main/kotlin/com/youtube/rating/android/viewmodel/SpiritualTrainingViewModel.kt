package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.NovenaTodayResponse
import com.youtube.rating.shared.utils.RequestDeduplicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NovenaUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val payload: NovenaTodayResponse? = null
)

class SpiritualTrainingViewModel(
    private val apiClient: RatingApiClient
) : ViewModel() {

    private val requestDeduplicator = RequestDeduplicator()

    private val _novenaState = MutableStateFlow(NovenaUiState())
    val novenaState: StateFlow<NovenaUiState> = _novenaState

    fun loadNovenas(force: Boolean = false) {
        if (_novenaState.value.loading) return
        if (!force && _novenaState.value.payload != null) return

        viewModelScope.launch {
            val requestKey = "novenas:$force"

            // Spriječi simultane duplikate
            if (!requestDeduplicator.shouldExecute(requestKey)) return@launch

            _novenaState.value = _novenaState.value.copy(loading = true, error = null)
            try {
                val resp = apiClient.getNovenasToday()
                if (resp.success) {
                    _novenaState.value = _novenaState.value.copy(payload = resp)
                } else {
                    _novenaState.value = _novenaState.value.copy(error = resp.message ?: "Greska pri dohvacanju.")
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _novenaState.value = _novenaState.value.copy(error = e.message ?: "Greska pri dohvacanju.")
            } finally {
                _novenaState.value = _novenaState.value.copy(loading = false)
                requestDeduplicator.markComplete(requestKey)
            }
        }
    }
}

class SpiritualTrainingViewModelFactory(
    private val apiClient: RatingApiClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpiritualTrainingViewModel::class.java)) {
            return SpiritualTrainingViewModel(apiClient = apiClient) as T
        }
        val e = IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e,
            tags = mapOf("where" to "SpiritualTrainingViewModelFactory.create")
        )
        error(e.message ?: "Unknown ViewModel class")
    }
}
