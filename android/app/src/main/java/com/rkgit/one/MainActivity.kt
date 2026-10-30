package com.rkgit.one

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rkgit.one.push.DeepLinkHolder
import com.rkgit.one.push.EXTRA_PUSH_ID
import com.rkgit.one.push.EXTRA_PUSH_TYPE
import com.rkgit.one.push.parseDeepLink
import com.rkgit.one.ui.AppRoot
import com.rkgit.one.ui.theme.RkgitTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var deepLinkHolder: DeepLinkHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            handleDeepLink(intent)
        }

        setContent { RkgitTheme { AppRoot() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) return
        val type = intent.getStringExtra(EXTRA_PUSH_TYPE)
        val id = intent.getStringExtra(EXTRA_PUSH_ID)
        val link = parseDeepLink(type, id)
        if (link != null) {
            deepLinkHolder.set(link)
        }
        intent.removeExtra(EXTRA_PUSH_TYPE)
        intent.removeExtra(EXTRA_PUSH_ID)
    }
}
