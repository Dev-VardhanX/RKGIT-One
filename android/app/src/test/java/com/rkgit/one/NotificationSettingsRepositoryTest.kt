package com.rkgit.one

import com.rkgit.one.data.push.NotificationSettingsRepository
import com.rkgit.one.push.NotificationSettings
import com.rkgit.one.push.NotificationSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsRepositoryTest {

    private class FakeNotificationSettingsStore(
        private var settings: NotificationSettings = NotificationSettings(),
        private var subscribed: Set<String> = emptySet(),
        private var promptShown: Boolean = false,
        private var permissionRequested: Boolean = false
    ) : NotificationSettingsStore {
        var writeCount = 0
            private set

        override fun read(): NotificationSettings = settings

        override fun write(s: NotificationSettings) {
            writeCount++
            settings = s
        }

        override fun readSubscribed(): Set<String> = subscribed

        override fun writeSubscribed(topics: Set<String>) {
            subscribed = topics
        }

        override fun readPromptShown(): Boolean = promptShown

        override fun writePromptShown(shown: Boolean) {
            promptShown = shown
        }

        override fun readPermissionRequested(): Boolean = permissionRequested

        override fun writePermissionRequested(requested: Boolean) {
            permissionRequested = requested
        }
    }

    @Test
    fun `defaults are false for promptShown and permissionRequested`() {
        val store = FakeNotificationSettingsStore()
        val repo = NotificationSettingsRepository(store)

        assertFalse(repo.promptShown.value)
        assertFalse(repo.permissionRequested.value)
        assertTrue(repo.settings.value.noticesEnabled)
        assertTrue(repo.settings.value.eventsEnabled)
    }

    @Test
    fun `markPromptShown persists and updates flow`() {
        val store = FakeNotificationSettingsStore()
        val repo = NotificationSettingsRepository(store)

        repo.markPromptShown()

        assertTrue(store.readPromptShown())
        assertTrue(repo.promptShown.value)
    }

    @Test
    fun `markPermissionRequested persists and updates flow`() {
        val store = FakeNotificationSettingsStore()
        val repo = NotificationSettingsRepository(store)

        repo.markPermissionRequested()

        assertTrue(store.readPermissionRequested())
        assertTrue(repo.permissionRequested.value)
    }

    @Test
    fun `setBoth writes once and updates the flow once`() {
        val store = FakeNotificationSettingsStore()
        val repo = NotificationSettingsRepository(store)

        repo.setBoth(notices = false, events = false)

        assertEquals(1, store.writeCount)
        assertEquals(NotificationSettings(noticesEnabled = false, eventsEnabled = false), store.read())
        assertEquals(NotificationSettings(noticesEnabled = false, eventsEnabled = false), repo.settings.value)

        repo.setBoth(notices = true, events = false)
        assertEquals(2, store.writeCount)
        assertEquals(NotificationSettings(noticesEnabled = true, eventsEnabled = false), store.read())
        assertEquals(NotificationSettings(noticesEnabled = true, eventsEnabled = false), repo.settings.value)
    }

    @Test
    fun `setNoticesEnabled and setEventsEnabled keep working`() {
        val store = FakeNotificationSettingsStore()
        val repo = NotificationSettingsRepository(store)

        repo.setNoticesEnabled(false)
        assertFalse(repo.settings.value.noticesEnabled)
        assertTrue(repo.settings.value.eventsEnabled)
        assertFalse(store.read().noticesEnabled)

        repo.setEventsEnabled(false)
        assertFalse(repo.settings.value.noticesEnabled)
        assertFalse(repo.settings.value.eventsEnabled)
        assertFalse(store.read().eventsEnabled)
    }

    @Test
    fun `old store without keys reads defaults`() {
        // Simulating an old store that only knows default initialisation
        val store = FakeNotificationSettingsStore()
        assertEquals(false, store.readPromptShown())
        assertEquals(false, store.readPermissionRequested())
        assertEquals(NotificationSettings(noticesEnabled = true, eventsEnabled = true), store.read())
    }
}

