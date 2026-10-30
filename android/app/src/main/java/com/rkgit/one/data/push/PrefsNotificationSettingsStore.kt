package com.rkgit.one.data.push

import android.content.Context
import com.rkgit.one.push.NotificationSettings
import com.rkgit.one.push.NotificationSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsNotificationSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) : NotificationSettingsStore {

    private val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains("notices_enabled") || !prefs.contains("events_enabled")) {
            prefs.edit()
                .putBoolean("notices_enabled", prefs.getBoolean("notices_enabled", true))
                .putBoolean("events_enabled", prefs.getBoolean("events_enabled", true))
                .apply()
        }
    }

    override fun read(): NotificationSettings {
        val noticesEnabled = prefs.getBoolean("notices_enabled", true)
        val eventsEnabled = prefs.getBoolean("events_enabled", true)
        return NotificationSettings(
            noticesEnabled = noticesEnabled,
            eventsEnabled = eventsEnabled
        )
    }

    override fun write(s: NotificationSettings) {
        prefs.edit()
            .putBoolean("notices_enabled", s.noticesEnabled)
            .putBoolean("events_enabled", s.eventsEnabled)
            .apply()
    }

    override fun readSubscribed(): Set<String> {
        val set = prefs.getStringSet("subscribed_topics", emptySet())
        return set?.toSet() ?: emptySet()
    }

    override fun writeSubscribed(topics: Set<String>) {
        prefs.edit()
            .putStringSet("subscribed_topics", HashSet(topics))
            .apply()
    }

    override fun readPromptShown(): Boolean =
        prefs.getBoolean("prompt_shown", false)

    override fun writePromptShown(shown: Boolean) {
        prefs.edit().putBoolean("prompt_shown", shown).apply()
    }

    override fun readPermissionRequested(): Boolean =
        prefs.getBoolean("permission_requested", false)

    override fun writePermissionRequested(requested: Boolean) {
        prefs.edit().putBoolean("permission_requested", requested).apply()
    }
}
