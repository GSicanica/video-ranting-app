package com.youtube.rating.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youtube.rating.android.localization.UiText
import com.youtube.rating.android.data.repository.PsalmsRepository
import com.youtube.rating.android.utils.BibleApiService
import com.youtube.rating.shared.models.PsalmDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.youtube.rating.core.coroutines.makeIOCall

data class PsalmsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val psalms: List<PsalmDto> = emptyList(),          // original
    val filtered: List<PsalmDto> = emptyList(),        // NEW: već filtrirano
    val expanded: Set<Int> = emptySet(),
    val error: UiText? = null
)

private data class PsalmIndex(
    val dto: PsalmDto,
    val titleLower: String,
    val textLower: String,
    val textTrimmed: String
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PsalmsViewModel(
    private val repo: PsalmsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PsalmsUiState())
    val state: StateFlow<PsalmsUiState> = _state.asStateFlow()

    // Index (precompute) - radi jednom, posle je pretraga brza
    private var index: List<PsalmIndex> = emptyList()
    private var currentLanguage: BibleApiService.BibleLanguage = BibleApiService.BibleLanguage.CROATIAN

    // Query flow (debounce)
    private val queryFlow = MutableStateFlow("")

    init {
        // load default
        loadPsalms(language = currentLanguage)

        // filter pipeline (brzo + ne blokira UI)
        viewModelScope.launch {
            queryFlow
                .debounce(200) // 150–300ms je sweet spot
                .map { it.trim() }
                .distinctUntilChanged()
                .mapLatest { q ->
                    filterIndex(query = q)
                }
                .flowOn(Dispatchers.Default) // CPU posao van main thread-a
                .collect { filtered ->
                    _state.update { it.copy(filtered = filtered) }
                }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        queryFlow.value = q
    }

    fun setLanguage(language: BibleApiService.BibleLanguage) {
        if (language == currentLanguage && _state.value.psalms.isNotEmpty()) return
        currentLanguage = language
        loadPsalms(language = language)
    }

    fun toggleExpanded(psalmNumber: Int) {
        _state.update { s ->
            val next = if (psalmNumber in s.expanded) s.expanded - psalmNumber else s.expanded + psalmNumber
            s.copy(expanded = next)
        }
    }

    fun expandPsalm(psalmNumber: Int) {
        _state.update { s ->
            if (psalmNumber in s.expanded) s else s.copy(expanded = s.expanded + psalmNumber)
        }
    }

    private fun loadPsalms(language: BibleApiService.BibleLanguage) {
        _state.update { it.copy(isLoading = true, error = null) }
        makeIOCall {
            runCatching { repo.loadPsalms(language) }
                .onSuccess { list ->
                    index = list.map { p ->
                        PsalmIndex(
                            dto = p,
                            titleLower = p.title.lowercase(),
                            textLower = p.text.lowercase(),
                            textTrimmed = p.text.trim()
                        )
                    }
                    val currentQuery = _state.value.query
                    val filtered = filterIndex(query = currentQuery)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            psalms = list,
                            filtered = filtered,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = UiText.Dynamic(e.message ?: "Greška")) }
                }
        }
    }

    private fun filterIndex(query: String): List<PsalmDto> {
        val q = query.trim()
        if (q.isEmpty()) return index.map { it.dto }
        val qLower = q.lowercase()
        val qInt = q.toIntOrNull()
        return index.asSequence()
            .filter { p ->
                (qInt != null && p.dto.psalm == qInt) ||
                    p.titleLower.contains(qLower) ||
                    p.textLower.contains(qLower)
            }
            .map { it.dto }
            .toList()
    }
}