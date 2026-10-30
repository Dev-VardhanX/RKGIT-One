package com.rkgit.one.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimulatePushReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pushHandler: PushHandler

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: return
        val map = mutableMapOf<String, String>()
        for (key in extras.keySet()) {
            val value = extras.getString(key)
            if (value != null) {
                map[key] = value
            }
        }
        pushHandler.handle(map)
    }
}

