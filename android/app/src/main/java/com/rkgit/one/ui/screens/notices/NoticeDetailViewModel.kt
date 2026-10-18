package com.rkgit.one.ui.screens.notices

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.rkgit.one.data.repository.NoticesRepository
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.ui.LoadableViewModel
import com.rkgit.one.ui.navigation.NoticeDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoticeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoticesRepository
) : LoadableViewModel<Notice>() {

    private val route = savedStateHandle.toRoute<NoticeDetailRoute>()
    val noticeId: String = route.id

    override suspend fun fetch(): Notice = repository.byId(noticeId)

    init {
        load()
    }
}
