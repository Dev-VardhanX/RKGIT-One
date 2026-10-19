package com.rkgit.one.ui.screens.events

import com.rkgit.one.data.repository.EventsRepository
import com.rkgit.one.domain.model.Event
import com.rkgit.one.ui.LoadableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

data class EventsData(val upcoming: List<Event>, val recent: List<Event>)

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventsRepository
) : LoadableViewModel<EventsData>() {

    init {
        load()
    }

    override suspend fun fetch(): EventsData = coroutineScope {
        val upcoming = async { repository.upcoming() }
        val recent = async { repository.recent() }
        EventsData(upcoming.await(), recent.await())
    }
}

