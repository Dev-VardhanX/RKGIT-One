package com.rkgit.one.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RkgitMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushHandler: PushHandler

    @Inject
    lateinit var topicSync: TopicSync

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        pushHandler.handle(remoteMessage.data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        topicSync.sync()
    }
}

