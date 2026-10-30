package com.rkgit.one.ui.screens.home

import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.data.profile.StudentProfile
import com.rkgit.one.data.profile.matchingAudience
import com.rkgit.one.data.push.NotificationSettingsRepository
import com.rkgit.one.data.repository.EventsRepository
import com.rkgit.one.data.repository.MaterialsRepository
import com.rkgit.one.data.repository.NoticesRepository
import com.rkgit.one.domain.model.Event
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.domain.model.RecentMaterial
import com.rkgit.one.push.NotificationSettings
import com.rkgit.one.ui.LoadableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class HomeData(
    val notices: List<Notice>,
    val materials: List<RecentMaterial>,
    val events: List<Event>
) {
    val isEmpty: Boolean get() = notices.isEmpty() && materials.isEmpty() && events.isEmpty()

    fun visibleNotices(filterYear: Int?, filterDepartment: String?): List<Notice> =
        notices.matchingAudience(filterYear, filterDepartment).take(3)

    fun isEmptyFor(filterYear: Int?, filterDepartment: String?): Boolean =
        visibleNotices(filterYear, filterDepartment).isEmpty() && materials.isEmpty() && events.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noticesRepository: NoticesRepository,
    private val materialsRepository: MaterialsRepository,
    private val eventsRepository: EventsRepository,
    private val profileRepository: ProfileRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository
) : LoadableViewModel<HomeData>() {

    val profile: StateFlow<StudentProfile> = profileRepository.profile
    val promptShown: StateFlow<Boolean> = notificationSettingsRepository.promptShown
    val notificationSettings: StateFlow<NotificationSettings> = notificationSettingsRepository.settings

    fun markPromptShown() {
        notificationSettingsRepository.markPromptShown()
    }

    fun setBothNotifications(notices: Boolean, events: Boolean) {
        notificationSettingsRepository.setBoth(notices, events)
    }

    override suspend fun fetch(): HomeData = coroutineScope {
        val noticesDeferred = async { noticesRepository.active() }
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

