package com.rkgit.one.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class LoadableViewModel<T> : ViewModel() {

    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

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
            if (!(keepOldDataOnError && _state.value is UiState.Success)) {
                _state.value = UiState.Error(e.toUserMessage())
            }
        }
    }
}
