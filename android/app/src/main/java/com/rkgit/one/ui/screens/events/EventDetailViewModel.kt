package com.rkgit.one.ui.screens.events

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.rkgit.one.data.repository.EventsRepository
import com.rkgit.one.domain.model.Event
import com.rkgit.one.ui.LoadableViewModel
import com.rkgit.one.ui.navigation.EventDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventsRepository
) : LoadableViewModel<Event>() {

    private val eventId = savedStateHandle.toRoute<EventDetailRoute>().id

    init {
        load()
    }

    override suspend fun fetch(): Event = repository.byId(eventId)
}

