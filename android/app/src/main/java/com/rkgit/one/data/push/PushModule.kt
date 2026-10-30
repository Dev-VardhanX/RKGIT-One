package com.rkgit.one.data.push

import com.rkgit.one.push.NotificationSettingsStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {

    @Binds
    @Singleton
    abstract fun bindNotificationSettingsStore(impl: PrefsNotificationSettingsStore): NotificationSettingsStore
}

