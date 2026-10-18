package com.rkgit.one

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@HiltAndroidApp
class RkgitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            val root = File(cacheDir, "files")
            if (root.exists()) {
                root.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".part") }
                    .forEach { it.delete() }
            }
        }
    }
}
