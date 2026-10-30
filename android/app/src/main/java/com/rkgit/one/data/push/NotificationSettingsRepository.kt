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

    private val _promptShown = MutableStateFlow(store.readPromptShown())
    val promptShown: StateFlow<Boolean> = _promptShown.asStateFlow()

    private val _permissionRequested = MutableStateFlow(store.readPermissionRequested())
    val permissionRequested: StateFlow<Boolean> = _permissionRequested.asStateFlow()

    fun markPromptShown() {
        store.writePromptShown(true)
        _promptShown.value = true
    }

    fun markPermissionRequested() {
        store.writePermissionRequested(true)
        _permissionRequested.value = true
    }

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

    fun setBoth(notices: Boolean, events: Boolean) {
        val updated = NotificationSettings(noticesEnabled = notices, eventsEnabled = events)
        store.write(updated)
        _settings.value = updated
    }
}

