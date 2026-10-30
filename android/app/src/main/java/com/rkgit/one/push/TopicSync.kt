package com.rkgit.one.push

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.rkgit.one.BuildConfig
import com.rkgit.one.data.profile.DepartmentsCacheStore
import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.data.push.NotificationSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class TopicSync @Inject constructor(
    private val settingsRepository: NotificationSettingsRepository,
    private val profileRepository: ProfileRepository,
    private val departmentsCacheStore: DepartmentsCacheStore,
    private val store: NotificationSettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    fun sync() {
        scope.launch {
            mutex.withLock {
                try {
                    val settings = settingsRepository.settings.value
                    val profile = profileRepository.profile.value
                    val activeCodes = departmentsCacheStore.read()
                        .filter { it.active }
                        .map { it.code }

                    val desired = desiredTopics(
                        settings = settings,
                        year = profile.year,
                        departmentCode = profile.departmentCode,
                        activeDepartmentCodes = activeCodes,
                        prefix = BuildConfig.FCM_TOPIC_PREFIX
                    )

                    val subscribed = store.readSubscribed()
                    val diff = diffTopics(desired = desired, subscribed = subscribed)

                    Log.d("TopicSync", "Syncing topics: toSubscribe=${diff.toSubscribe.size}, toUnsubscribe=${diff.toUnsubscribe.size}")

                    for (topic in diff.toUnsubscribe) {
                        try {
                            withTimeout(10_000) {
                                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
                            }
                            val current = store.readSubscribed().toMutableSet()
                            current.remove(topic)
                            store.writeSubscribed(current)
                        } catch (e: Exception) {
                            Log.w("TopicSync", "Unsubscribe attempt failed: ${e.message}")
                        }
                    }

                    for (topic in diff.toSubscribe) {
                        try {
                            withTimeout(10_000) {
                                FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
                            }
                            val current = store.readSubscribed().toMutableSet()
                            current.add(topic)
                            store.writeSubscribed(current)
                        } catch (e: Exception) {
                            Log.w("TopicSync", "Subscribe attempt failed: ${e.message}")
                        }
                    }

                    val remainingDesired = desiredTopics(
                        settings = settingsRepository.settings.value,
                        year = profileRepository.profile.value.year,
                        departmentCode = profileRepository.profile.value.departmentCode,
                        activeDepartmentCodes = departmentsCacheStore.read().filter { it.active }.map { it.code },
                        prefix = BuildConfig.FCM_TOPIC_PREFIX
                    )
                    val remainingDiff = diffTopics(desired = remainingDesired, subscribed = store.readSubscribed())
                    if (remainingDiff.toSubscribe.isNotEmpty() || remainingDiff.toUnsubscribe.isNotEmpty()) {
                        scope.launch {
                            delay(15_000)
                            sync()
                        }
                    }
                } catch (e: Exception) {
                    Log.w("TopicSync", "Sync execution error: ${e.message}")
                }
            }
        }
    }

    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cont.resume(task.result)
                } else {
                    cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
                }
            }
        }
}
