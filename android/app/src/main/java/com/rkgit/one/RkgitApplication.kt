package com.rkgit.one

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class RkgitApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
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
