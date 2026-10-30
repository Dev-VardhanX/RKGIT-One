package com.rkgit.one.ui

import androidx.lifecycle.ViewModel
import com.rkgit.one.data.profile.ProfileRepository
import com.rkgit.one.push.DeepLink
import com.rkgit.one.push.DeepLinkHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val deepLinkHolder: DeepLinkHolder
) : ViewModel() {
    val startsWithSetup: Boolean = !profileRepository.isSetupDone()
    val deepLink: StateFlow<DeepLink?> = deepLinkHolder.link

    fun isSetupDone(): Boolean = profileRepository.isSetupDone()

    fun consumeDeepLink() {
        deepLinkHolder.consume()
    }
}
