package com.youtube.rating.android.feature.running.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.feature.running.domain.RunningActiveSession
import com.youtube.rating.android.feature.running.domain.RoutePoint
import com.youtube.rating.android.feature.running.domain.RunningEntry
import com.youtube.rating.android.feature.running.domain.RunningRepository
import com.youtube.rating.android.feature.running.domain.RunningStats
import com.youtube.rating.android.feature.running.domain.RouteTracker
import com.youtube.rating.android.feature.running.domain.RunningTrackingServiceController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.sqrt

class RunningViewModel(
    private val repository: RunningRepository,
    private val routeTracker: RouteTracker,
    private val trackingServiceController: RunningTrackingServiceController,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isGpsTracking = MutableStateFlow(false)
    val isGpsTracking: StateFlow<Boolean> = _isGpsTracking.asStateFlow()

    private val _activeRoutePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val activeRoutePoints: StateFlow<List<RoutePoint>> = _activeRoutePoints.asStateFlow()

    private val _activeDistanceKm = MutableStateFlow(0.0)
    val activeDistanceKm: StateFlow<Double> = _activeDistanceKm.asStateFlow()

    private val _activeDurationSeconds = MutableStateFlow(0L)
    val activeDurationSeconds: StateFlow<Long> = _activeDurationSeconds.asStateFlow()

    private val _activePaceMinPerKm = MutableStateFlow(0.0)
    val activePaceMinPerKm: StateFlow<Double> = _activePaceMinPerKm.asStateFlow()

    private var trackingJob: Job? = null
    private var timerJob: Job? = null
    private var startedAtEpochMillis: Long = 0L
    private val activeSessionPersistMutex = Mutex()
    private var activeSessionPersistVersion: Long = 0L

    val entries: StateFlow<List<RunningEntry>> = repository.entries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val stats: StateFlow<RunningStats> = entries
        .map { current ->
            val totalDistance = current.sumOf { it.distanceKm }
            val totalDuration = current.sumOf { it.durationMinutes }
            val avgPace = if (totalDistance > 0.0) totalDuration / totalDistance else 0.0

            RunningStats(
                totalRuns = current.size,
                totalDistanceKm = round(totalDistance * 100) / 100,
                totalDurationMinutes = totalDuration,
                averagePaceMinPerKm = round(avgPace * 100) / 100,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RunningStats(0, 0.0, 0, 0.0),
        )

    init {
        restoreActiveSessionIfNeeded()
    }

    fun addEntry(distanceKm: Double, durationMinutes: Int) {
        if (distanceKm <= 0.0 || durationMinutes <= 0) {
            _message.value = "Unesi valjanu distancu i trajanje."
            return
        }

        if (distanceKm > 500.0 || durationMinutes > 24 * 60) {
            _message.value = "Unos izgleda nerealno. Provjeri podatke."
            return
        }

        val pace = durationMinutes / distanceKm
        val normalizedPace = round(pace * 100) / 100

        val entry = RunningEntry(
            id = System.currentTimeMillis(),
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            paceMinPerKm = normalizedPace,
            createdAtMillis = System.currentTimeMillis(),
        )

        viewModelScope.launch {
            runCatching { repository.addEntry(entry) }
                .onSuccess { _message.value = "Trening spremljen." }
                .onFailure { _message.value = "Spremanje nije uspjelo. Pokusaj ponovno." }
        }
    }

    fun startGpsTracking() {
        if (_isGpsTracking.value) return

        _activeRoutePoints.value = emptyList()
        _activeDistanceKm.value = 0.0
        _activeDurationSeconds.value = 0L
        _activePaceMinPerKm.value = 0.0
        startedAtEpochMillis = System.currentTimeMillis()
        _isGpsTracking.value = true
        if (!trackingServiceController.start()) {
            _isGpsTracking.value = false
            resetActiveTrackingState()
            _message.value = "Ne mogu pokrenuti pozadinsko GPS praćenje. Provjeri dozvole i pokušaj ponovno."
            return
        }

        persistActiveSessionSnapshot()
        startTimerLoop()
        startTrackingCollection()
    }

    fun stopGpsTracking(saveSession: Boolean) {
        if (!_isGpsTracking.value) return

        _isGpsTracking.value = false
        trackingJob?.cancel()
        timerJob?.cancel()
        trackingServiceController.stop()

        if (!saveSession) {
            clearActiveSessionAndResetState(message = "GPS praćenje zaustavljeno bez spremanja.")
            return
        }

        val distanceKm = _activeDistanceKm.value
        val durationSeconds = _activeDurationSeconds.value
        val routePoints = _activeRoutePoints.value

        if (distanceKm <= 0.0 || durationSeconds <= 0L || routePoints.size < 2) {
            clearActiveSessionAndResetState(message = "Nema dovoljno GPS podataka za spremanje treninga.")
            return
        }

        val durationMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
        val pace = round(((durationMinutes / distanceKm) * 100)) / 100
        val entry = RunningEntry(
            id = System.currentTimeMillis(),
            distanceKm = distanceKm,
            durationMinutes = durationMinutes,
            paceMinPerKm = pace,
            createdAtMillis = System.currentTimeMillis(),
            routePoints = routePoints,
        )

        viewModelScope.launch {
            runCatching {
                repository.addEntry(entry)
                clearPersistedActiveSessionSync()
            }
                .onSuccess {
                    resetActiveTrackingState()
                    _message.value = "GPS trening spremljen."
                }
                .onFailure { _message.value = "Spremanje GPS treninga nije uspjelo." }
        }
    }

    private fun startTrackingCollection() {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            routeTracker.routeUpdates()
                .catch {
                    _isGpsTracking.value = false
                    timerJob?.cancel()
                    trackingServiceController.stop()
                    clearPersistedActiveSession()
                    _message.value = "GPS praćenje nije dostupno: ${it.message ?: "nepoznata greška"}"
                }
                .collect { point ->
                    _activeRoutePoints.update { current ->
                        val previous = current.lastOrNull()
                        if (previous != null) {
                            val meters = distanceMeters(previous = previous, next = point)
                            if (meters in 0.5..250.0) {
                                _activeDistanceKm.value += meters / 1000.0
                                updateActivePace()
                            }
                        }
                        val updated = current + point
                        persistActiveSessionSnapshot(routePoints = updated)
                        updated
                    }
                }
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isGpsTracking.value) {
                val elapsedMs = (System.currentTimeMillis() - startedAtEpochMillis).coerceAtLeast(0L)
                _activeDurationSeconds.value = (elapsedMs / 1000L).coerceAtLeast(0L)
                updateActivePace()
                delay(1000L)
            }
        }
    }

    private fun persistActiveSessionSnapshot(routePoints: List<RoutePoint> = _activeRoutePoints.value) {
        val startedAt = startedAtEpochMillis
        if (!_isGpsTracking.value || startedAt <= 0L) return
        val persistVersion = ++activeSessionPersistVersion
        val snapshot = RunningActiveSession(
            startedAtEpochMillis = startedAt,
            distanceKm = _activeDistanceKm.value,
            routePoints = routePoints,
        )
        viewModelScope.launch {
            activeSessionPersistMutex.withLock {
                if (persistVersion != activeSessionPersistVersion) return@withLock
                repository.setActiveSession(session = snapshot)
            }
        }
    }

    private fun restoreActiveSessionIfNeeded() {
        viewModelScope.launch {
            val session = repository.activeSession.first() ?: return@launch
            if (_isGpsTracking.value) return@launch

            startedAtEpochMillis = session.startedAtEpochMillis
            _activeRoutePoints.value = session.routePoints
            _activeDistanceKm.value = session.distanceKm
            _isGpsTracking.value = true
            val elapsedMs = (System.currentTimeMillis() - startedAtEpochMillis).coerceAtLeast(0L)
            _activeDurationSeconds.value = elapsedMs / 1000L
            updateActivePace()

            if (!trackingServiceController.start()) {
                _isGpsTracking.value = false
                clearPersistedActiveSession()
                resetActiveTrackingState()
                _message.value = "Pronađena je aktivna GPS sesija, ali servis se nije mogao pokrenuti."
                return@launch
            }

            startTimerLoop()
            startTrackingCollection()
            _message.value = "Nastavljeno GPS praćenje iz prethodne sesije."
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteEntry(entryId) }
                .onSuccess { _message.value = "Trening obrisan." }
                .onFailure { _message.value = "Brisanje nije uspjelo." }
        }
    }

    fun clearAllEntries() {
        viewModelScope.launch {
            runCatching { repository.clearEntries() }
                .onSuccess { _message.value = "Svi treninzi su obrisani." }
                .onFailure { _message.value = "Brisanje svih treninga nije uspjelo." }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun onLocationPermissionDenied() {
        _message.value = "Za GPS praćenje potrebno je odobriti lokaciju."
    }

    override fun onCleared() {
        trackingJob?.cancel()
        timerJob?.cancel()
        super.onCleared()
    }

    private fun clearActiveSessionAndResetState(message: String) {
        resetActiveTrackingState()
        clearPersistedActiveSession()
        _message.value = message
    }

    private fun resetActiveTrackingState() {
        _activeRoutePoints.value = emptyList()
        _activeDistanceKm.value = 0.0
        _activeDurationSeconds.value = 0L
        _activePaceMinPerKm.value = 0.0
        startedAtEpochMillis = 0L
    }

    private fun clearPersistedActiveSession() {
        val persistVersion = ++activeSessionPersistVersion
        viewModelScope.launch {
            activeSessionPersistMutex.withLock {
                if (persistVersion != activeSessionPersistVersion) return@withLock
                repository.setActiveSession(session = null)
            }
        }
    }

    private suspend fun clearPersistedActiveSessionSync() {
        val persistVersion = ++activeSessionPersistVersion
        activeSessionPersistMutex.withLock {
            if (persistVersion != activeSessionPersistVersion) return
            repository.setActiveSession(session = null)
        }
    }

    private fun updateActivePace() {
        val distance = _activeDistanceKm.value
        val minutes = _activeDurationSeconds.value / 60.0
        _activePaceMinPerKm.value = if (distance > 0.0 && minutes > 0.0) {
            round((minutes / distance) * 100) / 100
        } else {
            0.0
        }
    }

    private fun distanceMeters(previous: RoutePoint, next: RoutePoint): Double {
        val earthRadiusM = 6371000.0
        val dLat = Math.toRadians(next.latitude - previous.latitude)
        val dLon = Math.toRadians(next.longitude - previous.longitude)
        val lat1 = Math.toRadians(previous.latitude)
        val lat2 = Math.toRadians(next.latitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }
}
