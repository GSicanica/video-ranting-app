package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.core.presentation.state.ResultState
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.UsageAnalyticsResponse
import com.youtube.rating.shared.models.UsageSummary
import com.youtube.rating.shared.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsData(
    val usageSummary: UsageSummary?,
    val usageAnalytics: UsageAnalyticsResponse?,
)

data class AnalyticsUiState(
    val result: ResultState<AnalyticsData?> = ResultState.Success(null),
    val selectedPeriod: String = "month",
    val userToken: String? = null,
)

class AnalyticsViewModel(
    private val apiClient: RatingApiClient,
    private val userTokenManager: UserTokenManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val token = runCatching { userTokenManager.getUserTokenAsync() }.getOrNull()
            _uiState.update { it.copy(userToken = token) }
            if (!token.isNullOrBlank()) loadAnalytics()
            else _uiState.update { it.copy(result = ResultState.Error(Strings.tokenUnavailable)) }
        }
    }

    fun setSelectedPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadAnalytics()
    }

    fun refresh() {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        val token = _uiState.value.userToken
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(result = ResultState.Error(Strings.tokenUnavailable)) }
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(result = ResultState.Loading) }
            try {
                val period = _uiState.value.selectedPeriod
                val summaryResult = apiClient.getUsageSummary(userToken = token, period = period)
                val analyticsResult = apiClient.getUsageAnalytics(userToken = token, period = period)
                _uiState.update {
                    it.copy(result = ResultState.Success(AnalyticsData(usageSummary = summaryResult, usageAnalytics = analyticsResult)))
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _uiState.update { it.copy(result = ResultState.Error(Strings.errorLoading(e.message ?: ""))) }
                Logger.error("AnalyticsViewModel", "Failed to load analytics", e)
            }
        }
    }
}

