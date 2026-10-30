package com.rkgit.one.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkHolder @Inject constructor() {

    private val _link = MutableStateFlow<DeepLink?>(null)
    val link: StateFlow<DeepLink?> = _link.asStateFlow()

    fun set(deepLink: DeepLink) {
        _link.value = deepLink
    }

    fun consume() {
        _link.value = null
    }
}

