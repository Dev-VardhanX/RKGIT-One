package com.rkgit.one.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class LoadableViewModel<T> : ViewModel() {

    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshErrors = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val refreshErrors: SharedFlow<String> = _refreshErrors.asSharedFlow()

    protected abstract suspend fun fetch(): T

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            run(false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            run(true)
            _refreshing.value = false
        }
    }

    private suspend fun run(keepOldDataOnError: Boolean) {
        try {
            _state.value = UiState.Success(fetch())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (keepOldDataOnError && _state.value is UiState.Success) {
                _refreshErrors.tryEmit(e.toRefreshMessage())
            } else {
                _state.value = UiState.Error(e.toUserMessage(), offline = e.isOfflineError())
            }
        }
    }
}

