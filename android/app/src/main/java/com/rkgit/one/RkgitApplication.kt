package com.rkgit.one

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.data.push.NotificationSettingsRepository
import com.rkgit.one.push.NotificationChannels
import com.rkgit.one.push.TopicSync
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class RkgitApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var topicSync: TopicSync

    @Inject
    lateinit var settingsRepository: NotificationSettingsRepository

    @Inject
    lateinit var profileRepository: ProfileRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)

        topicSync.sync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            combine(
                settingsRepository.settings,
                profileRepository.profile
            ) { settings, profile ->
                Triple(settings, profile.year, profile.departmentCode)
            }
            .distinctUntilChanged()
            .drop(1)
            .debounce(500)
            .collect {
                topicSync.sync()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val root = File(cacheDir, "files")
            if (root.exists()) {
                root.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".part") }
                    .forEach { it.delete() }
            }
            val downloadsRoot = File(filesDir, "downloads")
            if (downloadsRoot.exists()) {
                downloadsRoot.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".part") }
                    .forEach { it.delete() }
            }
        }
    }
}
