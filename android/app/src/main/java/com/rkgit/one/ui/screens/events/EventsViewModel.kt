package com.rkgit.one.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkgit.one.data.repository.EventsRepository
import com.rkgit.one.domain.model.Event
import com.rkgit.one.ui.UiState
import com.rkgit.one.ui.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsData(val upcoming: List<Event>, val recent: List<Event>)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<EventsData>>(UiState.Loading)
    val state: StateFlow<UiState<EventsData>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            fetch(keepOldDataOnError = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            fetch(keepOldDataOnError = true)
            _refreshing.value = false
        }
    }

    private suspend fun fetch(keepOldDataOnError: Boolean) {
        try {
            val data = coroutineScope {
                val upcoming = async { repository.upcoming() }
                val recent = async { repository.recent() }
                EventsData(upcoming.await(), recent.await())
            }
            _state.value = UiState.Success(data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!(keepOldDataOnError && _state.value is UiState.Success)) {
                _state.value = UiState.Error(e.toUserMessage())
            }
        }
    }
}
