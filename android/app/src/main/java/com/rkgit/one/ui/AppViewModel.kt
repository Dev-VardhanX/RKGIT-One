package com.rkgit.one.ui

import androidx.lifecycle.ViewModel
import com.rkgit.one.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    profileRepository: ProfileRepository
) : ViewModel() {
    val startsWithSetup: Boolean = !profileRepository.isSetupDone()
}
