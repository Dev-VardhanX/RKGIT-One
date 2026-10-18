package com.rkgit.one.ui.screens.notices

import com.rkgit.one.data.repository.NoticesRepository
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.ui.LoadableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoticesViewModel @Inject constructor(
    private val repository: NoticesRepository
) : LoadableViewModel<List<Notice>>() {

    override suspend fun fetch(): List<Notice> = repository.active()

    init {
        load()
    }
}
