package com.rkgit.one.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rkgit.one.MainActivity
import com.rkgit.one.data.push.NotificationSettingsRepository
import com.rkgit.one.ui.util.toDisplay
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: NotificationSettingsRepository
) {

    fun handle(data: Map<String, String>) {
        val msg = parsePush(data) ?: return

        val settings = settingsRepository.settings.value
        val isEnabled = when (msg.type) {
            PushType.NOTICE -> settings.noticesEnabled
            PushType.EVENT -> settings.eventsEnabled
        }
        if (!isEnabled) {
            Log.d("PushHandler", "Notification type disabled in settings")
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            Log.d("PushHandler", "Notifications not enabled for app")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("PushHandler", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val channelId = when (msg.type) {
            PushType.NOTICE -> NotificationChannels.CHANNEL_NOTICES
            PushType.EVENT -> NotificationChannels.CHANNEL_EVENTS
        }

        val notifId = notificationId(msg)
        val text = notificationText(msg, formatDate = { it.toDisplay() })

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_TYPE, if (msg.type == PushType.NOTICE) "notice" else "event")
            putExtra(EXTRA_PUSH_ID, msg.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(notifId, notification)
            Log.d("PushHandler", "Notification posted successfully")
        } catch (e: SecurityException) {
            Log.w("PushHandler", "SecurityException when posting notification: ${e.message}")
        }
    }
}
