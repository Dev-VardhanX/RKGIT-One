package com.rkgit.one.ui.screens.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rkgit.one.data.repository.EventsRepository
import com.rkgit.one.domain.model.Event
import com.rkgit.one.ui.UiState
import com.rkgit.one.ui.navigation.EventDetailRoute
import com.rkgit.one.ui.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventsRepository
) : ViewModel() {

    private val eventId = savedStateHandle.toRoute<EventDetailRoute>().id

    private val _state = MutableStateFlow<UiState<Event>>(UiState.Loading)
    val state: StateFlow<UiState<Event>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.byId(eventId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }
}
