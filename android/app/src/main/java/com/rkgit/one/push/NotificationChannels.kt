package com.rkgit.one.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_NOTICES = "notices"
    const val CHANNEL_EVENTS = "events"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val noticesChannel = NotificationChannel(
                CHANNEL_NOTICES,
                "Notices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Official college notices"
            }
            val eventsChannel = NotificationChannel(
                CHANNEL_EVENTS,
                "Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "College events"
            }
            nm.createNotificationChannels(listOf(noticesChannel, eventsChannel))
        }
    }
}

