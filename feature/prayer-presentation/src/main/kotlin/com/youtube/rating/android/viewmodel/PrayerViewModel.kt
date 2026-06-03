package com.youtube.rating.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.localization.UiText
import com.youtube.rating.android.repository.Encouragement
import com.youtube.rating.android.repository.PrayerRepository
import com.youtube.rating.android.localization.Strings
import com.youtube.rating.android.repository.PrayerRequest
import com.youtube.rating.android.utils.AnalyticsManager
import com.youtube.rating.shared.utils.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.youtube.rating.android.data.prefs.TrainingPrefs

class PrayerViewModel(
    private val appContext: Context,
    private val repo: PrayerRepository,
    private val analyticsManager: AnalyticsManager,
    private val pageSize: Int = 20
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: UiText? = null,
        val createError: UiText? = null,
        val addEncouragementError: UiText? = null,
        val items: List<PrayerRequest> = emptyList(),
        val hasMore: Boolean = true,
        val isOnline: Boolean = true,
        val selectedRequest: PrayerRequest? = null,
        val encouragements: List<Encouragement> = emptyList(),
        val isLoadingEncouragements: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState(isLoading = true))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val page = AtomicInteger(0)

    // Minimalni interval između refresh poziva (3 minute)
    @Volatile
    private var lastRefreshTimestamp = 0L
    private val refreshCooldownMs = 3 * 60 * 1000L

    // Cache za ohrabrenja po requestId (TTL 5 min)
    private val encouragementsCache = ConcurrentHashMap<String, Pair<Long, List<Encouragement>>>()
    private val encouragementsCacheTtl = 5 * 60 * 1000L

    fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        // Ako imamo podatke i nije prošlo dovoljno vremena, preskoči
        if (!force && _ui.value.items.isNotEmpty() && (now - lastRefreshTimestamp) < refreshCooldownMs) {
            _ui.value = _ui.value.copy(isLoading = false)
            return
        }

        page.set(0)
        _ui.value = _ui.value.copy(isLoading = true, error = null, hasMore = true)
        viewModelScope.launch {
            runCatching { repo.loadRequests(page = 0, pageSize = pageSize, forceRefresh = force) }
                .onSuccess { p ->
                    lastRefreshTimestamp = System.currentTimeMillis()
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        items = p.items,
                        hasMore = p.hasMore,
                        error = null
                    )
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        isLoading = false,
                        error = UiText.Dynamic(e.message ?: Strings.errorLoadingGeneric)
                    )
                }
        }
    }

    fun loadMore() {
        val st = _ui.value
        if (st.isLoading || st.isLoadingMore || !st.hasMore) return

        _ui.value = st.copy(isLoadingMore = true)
        viewModelScope.launch {
            val next = page.get() + 1
            runCatching { repo.loadRequests(page = next, pageSize = pageSize, forceRefresh = false) }
                .onSuccess { p ->
                    page.set(next)
                    _ui.value = _ui.value.copy(
                        isLoadingMore = false,
                        items = _ui.value.items + p.items,
                        hasMore = p.hasMore
                    )
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        isLoadingMore = false,
                        error = UiText.Dynamic(e.message ?: Strings.errorLoadingGeneric)
                    )
                }
        }
    }

    fun createRequest(text: String, authorName: String?) {
        if (text.isBlank()) return
        _ui.value = _ui.value.copy(createError = null)
        viewModelScope.launch {
            runCatching { repo.createRequest(text.trim(), authorName?.trim().takeIf { !it.isNullOrBlank() }) }
                .onSuccess { created ->
                    // ubaci na vrh
                    _ui.value = _ui.value.copy(items = listOf(created) + _ui.value.items)
                    
                    // Track prayer request analytics
                    analyticsManager.trackPrayerRequest()
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(createError = UiText.Dynamic(e.message ?: Strings.errorAdding))
                }
        }
    }

    fun togglePrayed(item: PrayerRequest) {
        val newValue = !item.iPrayed
        Logger.info("PrayerViewModel", "togglePrayed requestId=${item.id} newValue=$newValue")
        // optimistički update
        val updatedLocal = item.copy(
            iPrayed = newValue,
            prayedCount = (item.prayedCount + if (newValue) 1 else -1).coerceAtLeast(0)
        )
        _ui.value = _ui.value.copy(items = _ui.value.items.map { if (it.id == item.id) updatedLocal else it })

        viewModelScope.launch {
            // Update local "training" counter (daily goal) immediately; rollback if API call fails.
            runCatching {
                TrainingPrefs.incrementTrainingPrayerDoneToday(
                    context = appContext.applicationContext,
                    delta = if (newValue) 1 else -1
                )
            }
            runCatching { repo.togglePrayed(item.id, prayed = newValue) }
                .onSuccess { serverUpdated ->
                    Logger.info(
                        "PrayerViewModel",
                        "togglePrayed success requestId=${item.id} serverCount=${serverUpdated.prayedCount} serverIPrayed=${serverUpdated.iPrayed}"
                    )
                    val merged = if (serverUpdated.prayedCount == item.prayedCount) {
                        serverUpdated.copy(prayedCount = updatedLocal.prayedCount, iPrayed = newValue)
                    } else {
                        serverUpdated
                    }
                    _ui.value = _ui.value.copy(items = _ui.value.items.map { if (it.id == item.id) merged else it })

                    if (serverUpdated.prayedCount == item.prayedCount) {
                        refresh(force = true)
                    }
                }
                .onFailure {
                    Logger.error("PrayerViewModel", "togglePrayed failed requestId=${item.id}", it)
                    _ui.value = _ui.value.copy(items = _ui.value.items.map { if (it.id == item.id) updatedLocal else it })
                    refresh(force = true)
                }
        }
    }

    fun openDetails(item: PrayerRequest) {
        // Provjeri cache za ohrabrenja
        val cached = encouragementsCache[item.id]
        if (cached != null && (System.currentTimeMillis() - cached.first) < encouragementsCacheTtl) {
            _ui.value = _ui.value.copy(selectedRequest = item, encouragements = cached.second, isLoadingEncouragements = false)
            return
        }

        _ui.value = _ui.value.copy(selectedRequest = item, encouragements = emptyList(), isLoadingEncouragements = true)
        viewModelScope.launch {
            runCatching { repo.loadEncouragements(item.id) }
                .onSuccess { list ->
                    encouragementsCache[item.id] = System.currentTimeMillis() to list
                    _ui.value = _ui.value.copy(encouragements = list, isLoadingEncouragements = false)
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        isLoadingEncouragements = false,
                        error = UiText.Dynamic(e.message ?: Strings.error)
                    )
                }
        }
    }

    fun clearCreateError() {
        _ui.value = _ui.value.copy(createError = null)
    }

    fun clearAddEncouragementError() {
        _ui.value = _ui.value.copy(addEncouragementError = null)
    }

    fun closeDetails() {
        _ui.value = _ui.value.copy(selectedRequest = null, encouragements = emptyList(), isLoadingEncouragements = false)
    }

    fun addEncouragement(requestId: String, message: String, authorName: String?) {
        if (message.isBlank()) return
        _ui.value = _ui.value.copy(addEncouragementError = null)
        viewModelScope.launch {
            runCatching { repo.addEncouragement(requestId, message.trim(), authorName?.trim().takeIf { !it.isNullOrBlank() }) }
                .onSuccess { created ->
                    // Invalidate encouragements cache za ovaj request
                    encouragementsCache.remove(requestId)
                    val st = _ui.value
                    _ui.value = st.copy(
                        encouragements = st.encouragements + created,
                        items = st.items.map {
                            if (it.id == requestId) it.copy(encouragementCount = it.encouragementCount + 1) else it
                        }
                    )
                    
                    // Track encouragement analytics
                    analyticsManager.trackEncouragement()

                    TrainingPrefs.incrementTrainingEncouragementDoneToday(
                        context = appContext.applicationContext,
                        delta = 1
                    )
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        addEncouragementError = UiText.Dynamic(e.message ?: Strings.errorSendingEncouragement)
                    )
                }
        }
    }

    fun deleteRequest(item: PrayerRequest, adminToken: String) {
        val current = _ui.value.items
        // Optimistic removal
        _ui.value = _ui.value.copy(
            items = current.filterNot { it.id == item.id },
            selectedRequest = _ui.value.selectedRequest?.takeIf { it.id != item.id }
        )

        viewModelScope.launch {
            runCatching { repo.deleteRequest(item.id, adminToken) }
                .onSuccess { ok ->
                    if (!ok) {
                        _ui.value = _ui.value.copy(items = current, error = UiText.Dynamic(Strings.deletionFailed))
                    }
                }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        items = current,
                        error = UiText.Dynamic(e.message ?: Strings.deletionFailed)
                    )
                }
        }
    }
}