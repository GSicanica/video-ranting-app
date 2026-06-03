package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.data.*
import com.youtube.rating.android.storage.RosaryManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RosaryViewModel(
    private val rosaryManager: RosaryManager
) : ViewModel() {
    
    val sessions = rosaryManager.sessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val settings = rosaryManager.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RosaryNotificationSettings()
    )
    
    val stats = rosaryManager.stats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RosaryStats()
    )
    
    private val _currentSession = MutableStateFlow<RosarySession?>(null)
    val currentSession: StateFlow<RosarySession?> = _currentSession.asStateFlow()
    
    private val _currentPosition = MutableStateFlow<RosaryPosition>(RosaryPosition.Start)
    val currentPosition: StateFlow<RosaryPosition> = _currentPosition.asStateFlow()
    
    private val _currentMystery = MutableStateFlow(RosaryMystery.forToday())
    val currentMystery: StateFlow<RosaryMystery> = _currentMystery.asStateFlow()
    
    /**
     * Započni novu molitvu krunice
     */
    fun startRosary() {
        viewModelScope.launch {
            runCatching {
                val session = rosaryManager.startSession()
                _currentSession.value = session
                _currentPosition.value = RosaryPosition.SignOfCross
                _currentMystery.value = RosaryMystery.valueOf(session.mysteryType)
            }
        }
    }
    
    /**
     * Idi na sljedeći korak
     */
    fun nextStep() {
        val nextPos = when (val current = _currentPosition.value) {
            RosaryPosition.Start -> RosaryPosition.SignOfCross
            RosaryPosition.SignOfCross -> RosaryPosition.ApostlesCreed
            RosaryPosition.ApostlesCreed -> RosaryPosition.FirstOurFather
            RosaryPosition.FirstOurFather -> RosaryPosition.FirstThreeHailMary(1)
            is RosaryPosition.FirstThreeHailMary -> {
                if (current.number < 3) {
                    RosaryPosition.FirstThreeHailMary(current.number + 1)
                } else {
                    RosaryPosition.FirstGloryBe
                }
            }
            RosaryPosition.FirstGloryBe -> RosaryPosition.Decade(1, RosaryPosition.DecadePosition.Mystery)
            is RosaryPosition.Decade -> {
                val position = current.position
                when (position) {
                    RosaryPosition.DecadePosition.Mystery ->
                        RosaryPosition.Decade(current.decadeNumber, RosaryPosition.DecadePosition.OurFather)
                    RosaryPosition.DecadePosition.OurFather ->
                        RosaryPosition.Decade(current.decadeNumber, RosaryPosition.DecadePosition.HailMary(1))
                    is RosaryPosition.DecadePosition.HailMary -> {
                        if (position.number < 10) {
                            RosaryPosition.Decade(
                                current.decadeNumber,
                                RosaryPosition.DecadePosition.HailMary(position.number + 1)
                            )
                        } else {
                            RosaryPosition.Decade(current.decadeNumber, RosaryPosition.DecadePosition.GloryBe)
                        }
                    }
                    RosaryPosition.DecadePosition.GloryBe ->
                        RosaryPosition.Decade(current.decadeNumber, RosaryPosition.DecadePosition.FatimaPrayer)
                    RosaryPosition.DecadePosition.FatimaPrayer -> {
                        // Ažuriraj sesiju - završena dekada
                        updateSessionDecade(decadeNumber = current.decadeNumber)

                        if (current.decadeNumber < 5) {
                            RosaryPosition.Decade(current.decadeNumber + 1, RosaryPosition.DecadePosition.Mystery)
                        } else {
                            RosaryPosition.HailHolyQueen
                        }
                    }
                }
            }
            RosaryPosition.HailHolyQueen -> {
                finishRosary()
                RosaryPosition.Finished
            }
            RosaryPosition.Finished -> RosaryPosition.Finished
        }
        
        _currentPosition.value = nextPos
    }
    
    /**
     * Ažuriraj broj završenih dekada
     */
    private fun updateSessionDecade(decadeNumber: Int) {
        viewModelScope.launch {
            runCatching {
                _currentSession.value?.let { session ->
                    rosaryManager.updateSession(
                        sessionId = session.id,
                        decadesCompleted = decadeNumber
                    )
                }
            }
        }
    }
    
    /**
     * Završi krunice
     */
    private fun finishRosary() {
        viewModelScope.launch {
            runCatching {
                _currentSession.value?.let { session ->
                    rosaryManager.updateSession(
                        sessionId = session.id,
                        completed = true,
                        decadesCompleted = 5
                    )
                }
            }
        }
    }
    
    /**
     * Resetuj krunice
     */
    fun resetRosary() {
        _currentSession.value = null
        _currentPosition.value = RosaryPosition.Start
        _currentMystery.value = RosaryMystery.forToday()
    }
    
    /**
     * Spremi postavke notifikacija
     */
    fun saveNotificationSettings(settings: RosaryNotificationSettings) {
        viewModelScope.launch {
            rosaryManager.saveNotificationSettings(settings)
        }
    }
    
    /**
     * Dohvati danas sesije
     */
    fun getTodaySessions(): List<RosarySession> {
        return rosaryManager.getTodaySessions()
    }
}
