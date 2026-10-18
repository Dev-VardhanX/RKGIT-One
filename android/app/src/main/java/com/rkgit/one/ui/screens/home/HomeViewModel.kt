package com.rkgit.one.ui.screens.home

import com.rkgit.one.data.repository.EventsRepository
import com.rkgit.one.data.repository.MaterialsRepository
import com.rkgit.one.data.repository.NoticesRepository
import com.rkgit.one.domain.model.Event
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.RecentMaterial
import com.rkgit.one.ui.LoadableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

data class HomeData(
    val notices: List<Notice>,
    val materials: List<RecentMaterial>,
    val events: List<Event>
) {
    val isEmpty: Boolean get() = notices.isEmpty() && materials.isEmpty() && events.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noticesRepository: NoticesRepository,
    private val materialsRepository: MaterialsRepository,
    private val eventsRepository: EventsRepository
) : LoadableViewModel<HomeData>() {

    override suspend fun fetch(): HomeData = coroutineScope {
        val noticesDeferred = async { noticesRepository.active().take(3) }
        val materialsDeferred = async { materialsRepository.recent(5) }
        val eventsDeferred = async { eventsRepository.upcoming().take(3) }

        HomeData(
            notices = noticesDeferred.await(),
            materials = materialsDeferred.await(),
            events = eventsDeferred.await()
        )
    }

    init {
        load()
    }
}
