package com.rkgit.one.data.push

import com.rkgit.one.push.NotificationSettings
import com.rkgit.one.push.NotificationSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsRepository @Inject constructor(
    private val store: NotificationSettingsStore
) {
    private val _settings = MutableStateFlow(store.read())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    fun setNoticesEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(noticesEnabled = enabled)
        store.write(updated)
        _settings.value = updated
    }

    fun setEventsEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(eventsEnabled = enabled)
        store.write(updated)
        _settings.value = updated
    }
}

