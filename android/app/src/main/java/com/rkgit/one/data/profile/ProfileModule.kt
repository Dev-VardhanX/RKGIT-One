package com.rkgit.one.data.profile

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileStore(impl: PrefsProfileStore): ProfileStore

    @Binds
    @Singleton
    abstract fun bindDepartmentsCacheStore(impl: PrefsDepartmentsCacheStore): DepartmentsCacheStore
}
