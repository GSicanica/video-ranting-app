package com.youtube.rating.android.viewmodel

import com.youtube.rating.core.coroutines.ioDispatcher

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.data.repository.CallsRepository
import com.youtube.rating.android.data.prefs.CallsPrefs
import com.youtube.rating.android.data.prefs.InstallPrefs
import com.youtube.rating.android.data.prefs.PsalmPrefs
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.shared.models.PsalmAvailabilityItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.UUID
import java.time.OffsetDateTime
import java.time.Instant

data class CallsUiState(
    val callsUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val livekitUrl: String? = null,
    val livekitToken: String? = null,
    val roomName: String? = null,
    val displayName: String = "",
    val ageYears: String = "",
    val gender: String? = null,
    val notMarried: Boolean = false,
    val favoritePsalm: String? = null,
    val favoritePsalmLocked: Boolean = false,
    val savedPsalms: List<String> = emptyList(),
    val availabilityFrom: String = "",
    val availabilityItems: List<PsalmAvailabilityItem> = emptyList(),
    val selectedTokens: Set<String> = emptySet(),
    val availableTokens: List<String> = emptyList(),
    val tooManyHighlights: Boolean = false,
    val micEnabled: Boolean = true,
    val camEnabled: Boolean = true,
    val connectLiveKit: Boolean = false
)

class CallsViewModel(
    application: android.app.Application,
    private val repository: CallsRepository,
    private val userTokenManager: UserTokenManager
) : AndroidViewModel(application) {

    private val defaultLiveKitUrl: String = "wss://rtc.tmbv-hms.com"
    private val shouldLockFavoritePsalm: Boolean = !BuildConfig.DEBUG

    private val _uiState = MutableStateFlow(
        CallsUiState(
            callsUrl = repository.getCallsUrl()
        )
    )
    val uiState: StateFlow<CallsUiState> = _uiState

    private val highlightsFlow = PsalmPrefs.highlightedPsalmLinesFlow(getApplication())
        .map { it.toList().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            highlightsFlow.collect { tokens ->
                _uiState.update {
                    val nextFavorite = when {
                        !it.favoritePsalm.isNullOrBlank() -> it.favoritePsalm
                        tokens.isNotEmpty() -> tokens.first()
                        else -> null
                    }
                    it.copy(
                        availableTokens = tokens,
                        tooManyHighlights = tokens.size > 100,
                        favoritePsalm = nextFavorite
                    )
                }
            }
        }

        viewModelScope.launch {
            PsalmPrefs.savedPsalmsFlow(getApplication()).collect { set ->
                val list = set.toList().sorted()
                _uiState.update { current ->
                    val existing = current.favoritePsalm
                    val nextFavorite = when {
                        !existing.isNullOrBlank() && list.contains(existing) -> existing
                        list.isNotEmpty() -> list.first()
                        else -> null
                    }
                    current.copy(savedPsalms = list, favoritePsalm = nextFavorite)
                }
            }
        }

        viewModelScope.launch {
            CallsPrefs.genderFlow(getApplication()).collect { stored ->
                val normalized = stored?.trim()?.lowercase()
                val gender = when (normalized) {
                    "male", "m", "musko" -> "male"
                    "female", "f", "zensko" -> "female"
                    else -> null
                }
                _uiState.update { it.copy(gender = gender) }
            }
        }

        viewModelScope.launch {
            CallsPrefs.displayNameFlow(getApplication()).collect { stored ->
                val trimmed = stored?.trim().orEmpty()
                if (trimmed.isBlank()) return@collect
                _uiState.update { it.copy(displayName = trimmed) }
            }
        }

        viewModelScope.launch {
            CallsPrefs.ageYearsFlow(getApplication()).collect { stored ->
                val trimmed = stored?.trim().orEmpty()
                if (trimmed.isBlank()) return@collect
                _uiState.update { it.copy(ageYears = trimmed) }
            }
        }

        viewModelScope.launch {
            CallsPrefs.favoritePsalmFlow(getApplication()).collect { stored ->
                val trimmed = stored?.trim()?.takeIf { it.isNotBlank() }
                if (trimmed == null) return@collect
                _uiState.update { it.copy(favoritePsalm = trimmed, favoritePsalmLocked = shouldLockFavoritePsalm) }
            }
        }
    }

    private fun normalizeLiveKitUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val noTrailingSlash = trimmed.trimEnd('/')
        return when {
            noTrailingSlash.startsWith("wss://", ignoreCase = true) -> noTrailingSlash
            noTrailingSlash.startsWith("ws://", ignoreCase = true) -> noTrailingSlash
            noTrailingSlash.startsWith("https://", ignoreCase = true) ->
                "wss://" + noTrailingSlash.removePrefix("https://")
            noTrailingSlash.startsWith("http://", ignoreCase = true) ->
                "ws://" + noTrailingSlash.removePrefix("http://")
            else -> noTrailingSlash
        }
    }

    fun refreshCallsUrl() {
        _uiState.update { it.copy(callsUrl = repository.getCallsUrl()) }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value) }
        viewModelScope.launch {
            CallsPrefs.setDisplayName(getApplication(), value)
        }
    }

    fun onAgeYearsChange(value: String) {
        _uiState.update { it.copy(ageYears = value) }
        viewModelScope.launch {
            CallsPrefs.setAgeYears(getApplication(), value)
        }
    }

    fun onGenderChange(value: String) {
        val normalized = value.trim().lowercase()
        val gender = when (normalized) {
            "male", "m", "musko" -> "male"
            "female", "f", "zensko" -> "female"
            else -> null
        }
        _uiState.update { it.copy(gender = gender) }
        viewModelScope.launch {
            CallsPrefs.setGender(getApplication(), gender)
        }
    }

    fun onNotMarriedChange(value: Boolean) {
        _uiState.update { it.copy(notMarried = value) }
    }

    fun onAvailabilityFromChange(value: String) {
        _uiState.update { it.copy(availabilityFrom = value) }
    }

    fun saveAvailability() {
        viewModelScope.launch {
            val current = _uiState.value
            val name = current.displayName.trim()
            if (name.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Unesi ime.") }
                return@launch
            }
            val age = current.ageYears.trim().toIntOrNull()
            if (age == null || age !in 18..99) {
                _uiState.update { it.copy(errorMessage = "Unesi godine (18–99).") }
                return@launch
            }
            val gender = current.gender
            val favorite = current.favoritePsalm
            if (gender.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Odaberi spol (muško/žensko).") }
                return@launch
            }
            if (favorite.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Odaberi omiljeni psalm.") }
                return@launch
            }
            if (current.availabilityFrom.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Odaberi kada si dostupan.") }
                return@launch
            }
            val availabilityInstant = runCatching {
                OffsetDateTime.parse(current.availabilityFrom.trim()).toInstant()
            }.getOrNull()
            if (availabilityInstant == null) {
                _uiState.update { it.copy(errorMessage = "Neispravan datum/vrijeme dostupnosti.") }
                return@launch
            }
            // Avoid server-side 400 when user picks "now" and it becomes past due to clock/UTC conversion.
            if (availabilityInstant.isBefore(Instant.now().plusSeconds(60))) {
                _uiState.update { it.copy(errorMessage = "Odaberi vrijeme barem 1 min u budućnosti.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                @Suppress("DEPRECATION")
                val userToken = userTokenManager.getUserTokenAsyncAutoRegister()
                val res = repository.setPsalmCallAvailability(
                    userToken = userToken,
                    gender = gender,
                    notMarried = true,
                    favoritePsalm = favorite,
                    availableFrom = current.availabilityFrom.trim(),
                    displayName = name,
                    ageYears = age
                )
                if (!res.success) {
                    _uiState.update { it.copy(errorMessage = res.message ?: "Neuspješno spremanje dostupnosti") }
                    return@launch
                }
                refreshAvailability()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Greška: ${e.message ?: "nepoznato"}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refreshAvailability() {
        viewModelScope.launch {
            val current = _uiState.value
            val gender = current.gender ?: return@launch
            val favorite = current.favoritePsalm ?: return@launch
            try {
                @Suppress("DEPRECATION")
                val userToken = userTokenManager.getUserTokenAsyncAutoRegister()
                val res = repository.listPsalmCallAvailability(
                    userToken = userToken,
                    gender = gender,
                    favoritePsalm = favorite
                )
                if (res.success) {
                    _uiState.update { it.copy(availabilityItems = res.items) }
                }
            } catch (_: Exception) {
                // ignore (best-effort)
            }
        }
    }

    fun onFavoritePsalmChange(value: String) {
        val current = _uiState.value
        if (shouldLockFavoritePsalm && current.favoritePsalmLocked) return

        val trimmed = value.trim().takeIf { it.isNotBlank() }
        val number = trimmed?.toIntOrNull()
        if (number == null || number !in 1..150) {
            _uiState.update { it.copy(errorMessage = "Omiljeni psalm mora biti broj 1–150.") }
            return
        }

        val normalized = number.toString()
        _uiState.update {
            it.copy(
                favoritePsalm = normalized,
                favoritePsalmLocked = shouldLockFavoritePsalm,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            CallsPrefs.setFavoritePsalm(getApplication(), normalized)
        }
    }

    fun onTokenChecked(token: String, checked: Boolean) {
        _uiState.update { current ->
            val next = if (checked) current.selectedTokens + token else current.selectedTokens - token
            current.copy(selectedTokens = next)
        }
    }

    fun onMicEnabledChange(value: Boolean) {
        _uiState.update { it.copy(micEnabled = value) }
    }

    fun onCamEnabledChange(value: Boolean) {
        _uiState.update { it.copy(camEnabled = value) }
    }

    fun disconnect() {
        _uiState.update { it.copy(connectLiveKit = false) }
        clearLiveKitSession()
    }

    fun join() {
        viewModelScope.launch {
            val current = _uiState.value
            val filledName = current.displayName.trim()
            if (filledName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Unesi ime.") }
                return@launch
            }
            val age = current.ageYears.trim().toIntOrNull()
            if (age == null || age !in 18..99) {
                _uiState.update { it.copy(errorMessage = "Unesi godine (18–99).") }
                return@launch
            }
            val filledGender = current.gender
            if (filledGender == null) {
                _uiState.update { it.copy(errorMessage = "Odaberi spol (muško/žensko).") }
                return@launch
            }
            val favoritePsalm = current.favoritePsalm
            if (favoritePsalm.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Odaberi omiljeni psalm.") }
                return@launch
            }
            val filledNotMarried = true
            val filledSelected = if (current.selectedTokens.size == 5) {
                current.selectedTokens
            } else {
                current.availableTokens.take(5).toSet()
            }

            _uiState.update {
                it.copy(
                    displayName = filledName,
                    gender = filledGender,
                    notMarried = filledNotMarried,
                    selectedTokens = filledSelected
                )
            }

            val ok = requestPsalmRoom(
                displayName = filledName.trim(),
                gender = filledGender,
                notMarried = filledNotMarried,
                favoritePsalm = favoritePsalm,
                selectedTokens = filledSelected,
                highlights = current.availableTokens.toSet()
            )
            if (ok) {
                _uiState.update { it.copy(connectLiveKit = true) }
            }
        }
    }

    suspend fun requestPsalmRoom(
        displayName: String,
        gender: String,
        notMarried: Boolean,
        favoritePsalm: String,
        selectedTokens: Set<String>,
        highlights: Set<String>
    ): Boolean = withContext(ioDispatcher) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            @Suppress("DEPRECATION")
            val userToken = userTokenManager.getUserTokenAsyncAutoRegister()
            // Identity MUST be unique per device, otherwise two devices can overwrite each other in the same room.
            val identity = run {
                val ctx = getApplication<android.app.Application>().applicationContext
                val installId = InstallPrefs.getInstallId(ctx)
                if (!installId.isNullOrBlank()) {
                    installId
                } else {
                    // Fallback: generate and persist a random per-install id.
                    val generated = UUID.randomUUID().toString()
                    InstallPrefs.setInstallId(ctx, generated)
                    generated
                }
            }

            val syncRes = repository.syncPsalmHighlights(
                userToken = userToken,
                highlights = highlights.toList(),
                displayName = displayName,
                gender = gender,
                notMarried = notMarried
            )
            if (!syncRes.success) {
                _uiState.update { it.copy(errorMessage = syncRes.message ?: "Neuspjesna sinkronizacija") }
                return@withContext false
            }

            val roomRes = repository.getPsalmCallRoom(
                userToken = userToken,
                displayName = displayName,
                gender = gender,
                notMarried = notMarried,
                selectedTokens = selectedTokens.toList(),
                favoritePsalm = favoritePsalm
            )
            if (!roomRes.success || roomRes.room.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = roomRes.message ?: "Neuspjesno spajanje") }
                return@withContext false
            }

            val roomName = roomRes.room
            if (roomName.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = roomRes.message ?: "Neuspjesno spajanje") }
                return@withContext false
            }

            val lkRes = repository.getLiveKitToken(
                userToken = userToken,
                room = roomName,
                identity = identity,
                name = displayName
            )
            if (!lkRes.success || lkRes.token.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = lkRes.message ?: "Neuspjesan token") }
                return@withContext false
            }

            Log.d(
                "LiveKitCalls",
                "token ok room=$roomName identity=${identity.take(8)} url=${lkRes.url} tokenLen=${lkRes.token?.length ?: 0}"
            )

            _uiState.update { current ->
                // Always prefer our known-good reverse proxy URL (TLS + proper headers via nginx).
                // This avoids devices connecting directly to ws://IP:7880 and failing due to network policy/TLS.
                val url = defaultLiveKitUrl
                current.copy(
                    livekitToken = lkRes.token,
                    livekitUrl = url ?: current.livekitUrl,
                    roomName = roomName,
                    connectLiveKit = true
                )
            }
            return@withContext true
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Greska: ${e.message ?: "nepoznato"}") }
            return@withContext false
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearLiveKitSession() {
        _uiState.update { it.copy(livekitToken = null, roomName = null, errorMessage = null, isLoading = false) }
    }
}
