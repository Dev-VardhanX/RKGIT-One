package com.rkgit.one.ui.screens.notices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rkgit.one.data.downloads.DownloadRepository
import com.rkgit.one.data.downloads.downloadKey
import com.rkgit.one.data.repository.NoticesRepository
import com.rkgit.one.domain.model.Notice
import com.rkgit.one.ui.LoadableViewModel
import com.rkgit.one.ui.navigation.NoticeDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NoticeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoticesRepository,
    downloadRepository: DownloadRepository
) : LoadableViewModel<Notice>() {

    private val route = savedStateHandle.toRoute<NoticeDetailRoute>()
    val noticeId: String = route.id

    val downloaded: StateFlow<Boolean> = downloadRepository.observeAllKeys()
        .map { it.contains(downloadKey("notice", noticeId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    override suspend fun fetch(): Notice = repository.byId(noticeId)

    init {
        load()
    }
}
