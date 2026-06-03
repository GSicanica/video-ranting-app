package com.youtube.rating.core.presentation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Minimal MVI base:
 * - UI sends [I] intents via [dispatch]
 * - ViewModel reduces them into a single [state]
 * - one-off events go through [effects]
 */
abstract class MviViewModel<I : Any, S : Any, E : Any>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _intents = MutableSharedFlow<I>(extraBufferCapacity = 64)
    private val _effects = Channel<E>(capacity = Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            _intents.collect { intent ->
                val prev = _state.value
                val next = reduce(state = prev, intent = intent)
                if (next != prev) {
                    _state.value = next
                }
                handle(intent = intent, state = next)
            }
        }
    }

    fun dispatch(intent: I) {
        _intents.tryEmit(intent)
    }

    protected fun setState(transform: (S) -> S) {
        _state.value = transform(_state.value)
    }

    protected fun emitEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }

    protected abstract fun reduce(state: S, intent: I): S

    /**
     * Side effects (network, persistence, paging refresh, etc.).
     * Runs after [reduce] and receives the reduced state snapshot.
     */
    protected open fun handle(intent: I, state: S) = Unit
}
